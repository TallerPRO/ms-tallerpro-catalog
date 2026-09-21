package com.ms_tallerpro.catalog.service;

import com.ms_tallerpro.catalog.dto.RepuestoDtos.MovimientoStockRequest;
import com.ms_tallerpro.catalog.dto.RepuestoDtos.MovimientoStockResponse;
import com.ms_tallerpro.catalog.dto.RepuestoDtos.RepuestoRequest;
import com.ms_tallerpro.catalog.dto.RepuestoDtos.RepuestoResponse;
import com.ms_tallerpro.catalog.exception.ConflictoException;
import com.ms_tallerpro.catalog.exception.RecursoNoEncontradoException;
import com.ms_tallerpro.catalog.model.MovimientoStock;
import com.ms_tallerpro.catalog.model.Repuesto;
import com.ms_tallerpro.catalog.model.TipoMovimientoStock;
import com.ms_tallerpro.catalog.repository.MovimientoStockRepository;
import com.ms_tallerpro.catalog.repository.RepuestoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repuestos e inventario por taller. El decremento de stock (RF-08) lo dispara
 * ms-tallerpro-jobs al diagnosticar una orden y es idempotente por eventId.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RepuestoService {

    private final RepuestoRepository repository;
    private final MovimientoStockRepository movimientos;

    @Transactional(readOnly = true)
    public Page<RepuestoResponse> listar(UUID tallerId, String busqueda, boolean soloBajoStock, boolean soloActivos, Pageable pageable) {
        String texto = (busqueda == null || busqueda.isBlank()) ? null : busqueda.trim();
        return repository.buscar(tallerId, texto, soloBajoStock, soloActivos, pageable).map(RepuestoResponse::from);
    }

    @Transactional(readOnly = true)
    public RepuestoResponse obtener(UUID tallerId, UUID id) {
        return RepuestoResponse.from(buscarOFallar(tallerId, id));
    }

    public RepuestoResponse crear(UUID tallerId, RepuestoRequest req) {
        if (repository.existsByTallerIdAndCodigo(tallerId, req.codigo())) {
            throw new ConflictoException("Ya existe el repuesto %s en el taller %s".formatted(req.codigo(), tallerId));
        }
        Repuesto repuesto = Repuesto.builder()
                .tallerId(tallerId)
                .codigo(req.codigo())
                .nombre(req.nombre())
                .marca(req.marca())
                .precioUnitario(req.precioUnitario())
                .stock(req.stock())
                .stockMinimo(req.stockMinimo())
                .activo(req.activo() == null || req.activo())
                .build();
        return RepuestoResponse.from(repository.save(repuesto));
    }

    public RepuestoResponse actualizar(UUID tallerId, UUID id, RepuestoRequest req) {
        Repuesto repuesto = buscarOFallar(tallerId, id);
        if (!repuesto.getCodigo().equals(req.codigo()) && repository.existsByTallerIdAndCodigo(tallerId, req.codigo())) {
            throw new ConflictoException("Ya existe el repuesto %s en el taller %s".formatted(req.codigo(), tallerId));
        }
        repuesto.setCodigo(req.codigo());
        repuesto.setNombre(req.nombre());
        repuesto.setMarca(req.marca());
        repuesto.setPrecioUnitario(req.precioUnitario());
        repuesto.setStock(req.stock());
        repuesto.setStockMinimo(req.stockMinimo());
        if (req.activo() != null) {
            repuesto.setActivo(req.activo());
        }
        return RepuestoResponse.from(repository.save(repuesto));
    }

    public void desactivar(UUID tallerId, UUID id) {
        Repuesto repuesto = buscarOFallar(tallerId, id);
        repuesto.setActivo(false);
        repository.save(repuesto);
    }

    /**
     * RF-08. Si el eventId ya fue procesado devuelve el movimiento original sin
     * volver a descontar (reintentos de jobs). Si no hay stock suficiente -> 409.
     */
    public MovimientoStockResponse decrementarStock(UUID tallerId, UUID id, MovimientoStockRequest req, String eventIdHeader) {
        return mover(tallerId, id, req, eventIdHeader, TipoMovimientoStock.DECREMENTO);
    }

    /** Reposicion de inventario (rol Admin). Tambien idempotente por eventId. */
    public MovimientoStockResponse incrementarStock(UUID tallerId, UUID id, MovimientoStockRequest req, String eventIdHeader) {
        return mover(tallerId, id, req, eventIdHeader, TipoMovimientoStock.INCREMENTO);
    }

    @Transactional(readOnly = true)
    public List<MovimientoStockResponse> historial(UUID tallerId, UUID id) {
        buscarOFallar(tallerId, id);
        return movimientos.findByRepuestoIdOrderByFechaDesc(id).stream()
                .map(m -> MovimientoStockResponse.from(m, false))
                .toList();
    }

    private MovimientoStockResponse mover(UUID tallerId, UUID id, MovimientoStockRequest req, String eventIdHeader,
                                          TipoMovimientoStock tipo) {
        String eventId = Optional.ofNullable(req.eventId()).filter(s -> !s.isBlank())
                .or(() -> Optional.ofNullable(eventIdHeader).filter(s -> !s.isBlank()))
                .orElseGet(() -> UUID.randomUUID().toString());

        Optional<MovimientoStock> previo = movimientos.findByEventId(eventId);
        if (previo.isPresent()) {
            log.info("Movimiento de stock duplicado ignorado (eventId={})", eventId);
            return MovimientoStockResponse.from(previo.get(), true);
        }

        Repuesto repuesto = buscarOFallar(tallerId, id);
        int nuevoStock = tipo == TipoMovimientoStock.DECREMENTO
                ? repuesto.getStock() - req.cantidad()
                : repuesto.getStock() + req.cantidad();
        if (nuevoStock < 0) {
            throw new ConflictoException("Stock insuficiente para %s: disponible %d, solicitado %d"
                    .formatted(repuesto.getCodigo(), repuesto.getStock(), req.cantidad()));
        }
        repuesto.setStock(nuevoStock);
        repository.save(repuesto);

        MovimientoStock movimiento = movimientos.save(MovimientoStock.builder()
                .eventId(eventId)
                .repuestoId(repuesto.getId())
                .tallerId(tallerId)
                .tipo(tipo)
                .cantidad(req.cantidad())
                .stockResultante(nuevoStock)
                .motivo(req.motivo())
                .build());

        if (repuesto.bajoStock()) {
            log.warn("Stock bajo en taller {}: {} ({} unidades, minimo {})",
                    tallerId, repuesto.getCodigo(), repuesto.getStock(), repuesto.getStockMinimo());
        }
        return MovimientoStockResponse.from(movimiento, false);
    }

    private Repuesto buscarOFallar(UUID tallerId, UUID id) {
        return repository.findByIdAndTallerId(id, tallerId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Repuesto %s no encontrado en el taller %s".formatted(id, tallerId)));
    }
}
