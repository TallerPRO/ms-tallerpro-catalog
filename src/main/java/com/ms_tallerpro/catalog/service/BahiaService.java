package com.ms_tallerpro.catalog.service;

import com.ms_tallerpro.catalog.dto.BahiaDtos.BahiaDisponibilidadResponse;
import com.ms_tallerpro.catalog.dto.BahiaDtos.BahiaRequest;
import com.ms_tallerpro.catalog.dto.BahiaDtos.BahiaResponse;
import com.ms_tallerpro.catalog.dto.BahiaDtos.ReservaBahiaRequest;
import com.ms_tallerpro.catalog.dto.BahiaDtos.ResumenOcupacion;
import com.ms_tallerpro.catalog.exception.ConflictoException;
import com.ms_tallerpro.catalog.exception.RecursoNoEncontradoException;
import com.ms_tallerpro.catalog.model.Bahia;
import com.ms_tallerpro.catalog.model.EstadoBahia;
import com.ms_tallerpro.catalog.repository.BahiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bahias de un taller: CRUD (Admin) y ciclo de ocupacion (Jefe de taller):
 * DISPONIBLE --reservar--> RESERVADA --ocupar--> OCUPADA --liberar--> DISPONIBLE.
 * Tambien se puede liberar directamente desde RESERVADA (cancelacion de la reserva).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BahiaService {

    private final BahiaRepository repository;

    @Transactional(readOnly = true)
    public List<BahiaResponse> listar(UUID tallerId, EstadoBahia estado) {
        List<Bahia> bahias = estado == null
                ? repository.findByTallerIdOrderByCodigo(tallerId)
                : repository.findByTallerIdAndEstadoOrderByCodigo(tallerId, estado);
        return bahias.stream().map(BahiaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BahiaResponse obtener(UUID tallerId, UUID id) {
        return BahiaResponse.from(buscarOFallar(tallerId, id));
    }

    /** RF-06: contrato que consume ms-tallerpro-jobs antes de asignar la bahia a una orden. */
    @Transactional(readOnly = true)
    public BahiaDisponibilidadResponse disponibilidad(UUID tallerId, UUID id) {
        Bahia bahia = buscarOFallar(tallerId, id);
        return new BahiaDisponibilidadResponse(bahia.getId(), tallerId, bahia.disponible());
    }

    @Transactional(readOnly = true)
    public ResumenOcupacion resumen(UUID tallerId) {
        List<Bahia> bahias = repository.findByTallerIdOrderByCodigo(tallerId).stream().filter(Bahia::isActiva).toList();
        long total = bahias.size();
        long disponibles = bahias.stream().filter(b -> b.getEstado() == EstadoBahia.DISPONIBLE).count();
        long reservadas = bahias.stream().filter(b -> b.getEstado() == EstadoBahia.RESERVADA).count();
        long ocupadas = bahias.stream().filter(b -> b.getEstado() == EstadoBahia.OCUPADA).count();
        double tasa = total == 0 ? 0 : Math.round((ocupadas + reservadas) * 1000.0 / total) / 10.0;

        Optional<Bahia> proxima = bahias.stream()
                .filter(b -> b.getEstado() != EstadoBahia.DISPONIBLE && b.getTerminoProgramado() != null)
                .min(Comparator.comparing(Bahia::getTerminoProgramado));

        return new ResumenOcupacion(tallerId, total, disponibles, reservadas, ocupadas, tasa,
                proxima.map(Bahia::getTerminoProgramado).orElse(null),
                proxima.map(Bahia::getCodigo).orElse(null));
    }

    public BahiaResponse crear(UUID tallerId, BahiaRequest req) {
        if (repository.existsByTallerIdAndCodigo(tallerId, req.codigo())) {
            throw new ConflictoException("Ya existe la bahia %s en el taller %s".formatted(req.codigo(), tallerId));
        }
        Bahia bahia = Bahia.builder()
                .tallerId(tallerId)
                .codigo(req.codigo())
                .sector(req.sector())
                .activa(req.activa() == null || req.activa())
                .build();
        return BahiaResponse.from(repository.save(bahia));
    }

    public BahiaResponse actualizar(UUID tallerId, UUID id, BahiaRequest req) {
        Bahia bahia = buscarOFallar(tallerId, id);
        if (!bahia.getCodigo().equals(req.codigo()) && repository.existsByTallerIdAndCodigo(tallerId, req.codigo())) {
            throw new ConflictoException("Ya existe la bahia %s en el taller %s".formatted(req.codigo(), tallerId));
        }
        bahia.setCodigo(req.codigo());
        bahia.setSector(req.sector());
        if (req.activa() != null) {
            if (!req.activa() && bahia.getEstado() != EstadoBahia.DISPONIBLE) {
                throw new ConflictoException("No se puede desactivar una bahia reservada u ocupada");
            }
            bahia.setActiva(req.activa());
        }
        return BahiaResponse.from(repository.save(bahia));
    }

    public void desactivar(UUID tallerId, UUID id) {
        Bahia bahia = buscarOFallar(tallerId, id);
        if (bahia.getEstado() != EstadoBahia.DISPONIBLE) {
            throw new ConflictoException("No se puede desactivar una bahia reservada u ocupada");
        }
        bahia.setActiva(false);
        repository.save(bahia);
    }

    /** DISPONIBLE -> RESERVADA. Idempotente si la misma orden ya la tiene reservada. */
    public BahiaResponse reservar(UUID tallerId, UUID id, ReservaBahiaRequest req) {
        Bahia bahia = buscarOFallar(tallerId, id);
        if (req.ordenId().equals(bahia.getOrdenId()) && bahia.getEstado() != EstadoBahia.DISPONIBLE) {
            return BahiaResponse.from(bahia);
        }
        if (!bahia.disponible()) {
            throw new ConflictoException("La bahia %s no esta disponible (estado %s)".formatted(bahia.getCodigo(), bahia.getEstado()));
        }
        repository.findByOrdenId(req.ordenId()).ifPresent(otra -> {
            throw new ConflictoException("La orden %s ya tiene asignada la bahia %s".formatted(req.ordenId(), otra.getCodigo()));
        });
        bahia.setEstado(EstadoBahia.RESERVADA);
        bahia.setOrdenId(req.ordenId());
        bahia.setInicioProgramado(req.inicioProgramado() != null ? req.inicioProgramado() : Instant.now());
        bahia.setTerminoProgramado(req.terminoProgramado());
        bahia.setLibreDesde(null);
        return BahiaResponse.from(repository.save(bahia));
    }

    /** RESERVADA -> OCUPADA: el vehiculo ingreso fisicamente a la bahia. */
    public BahiaResponse ocupar(UUID tallerId, UUID id) {
        Bahia bahia = buscarOFallar(tallerId, id);
        if (bahia.getEstado() == EstadoBahia.OCUPADA) {
            return BahiaResponse.from(bahia);
        }
        if (bahia.getEstado() != EstadoBahia.RESERVADA) {
            throw new ConflictoException("Solo una bahia RESERVADA puede pasar a OCUPADA (estado actual %s)".formatted(bahia.getEstado()));
        }
        bahia.setEstado(EstadoBahia.OCUPADA);
        if (bahia.getInicioProgramado() == null) {
            bahia.setInicioProgramado(Instant.now());
        }
        return BahiaResponse.from(repository.save(bahia));
    }

    /** RESERVADA u OCUPADA -> DISPONIBLE (cancelacion de reserva o termino de trabajos). */
    public BahiaResponse liberar(UUID tallerId, UUID id) {
        Bahia bahia = buscarOFallar(tallerId, id);
        if (bahia.getEstado() == EstadoBahia.DISPONIBLE) {
            return BahiaResponse.from(bahia);
        }
        bahia.setEstado(EstadoBahia.DISPONIBLE);
        bahia.setOrdenId(null);
        bahia.setInicioProgramado(null);
        bahia.setTerminoProgramado(null);
        bahia.setLibreDesde(Instant.now());
        return BahiaResponse.from(repository.save(bahia));
    }

    private Bahia buscarOFallar(UUID tallerId, UUID id) {
        return repository.findByIdAndTallerId(id, tallerId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Bahia %s no encontrada en el taller %s".formatted(id, tallerId)));
    }
}
