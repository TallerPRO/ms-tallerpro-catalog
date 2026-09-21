package com.ms_tallerpro.catalog.service;

import com.ms_tallerpro.catalog.dto.ServicioDtos.ServicioRequest;
import com.ms_tallerpro.catalog.dto.ServicioDtos.ServicioResponse;
import com.ms_tallerpro.catalog.exception.RecursoNoEncontradoException;
import com.ms_tallerpro.catalog.model.CategoriaServicio;
import com.ms_tallerpro.catalog.model.Servicio;
import com.ms_tallerpro.catalog.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** CRUD del catalogo de servicios (rol Admin). */
@Service
@RequiredArgsConstructor
@Transactional
public class ServicioService {

    private final ServicioRepository repository;

    @Transactional(readOnly = true)
    public Page<ServicioResponse> listar(String busqueda, CategoriaServicio categoria, boolean soloActivos, Pageable pageable) {
        String texto = (busqueda == null || busqueda.isBlank()) ? null : busqueda.trim();
        return repository.buscar(texto, categoria, soloActivos, pageable).map(ServicioResponse::from);
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(UUID id) {
        return ServicioResponse.from(buscarOFallar(id));
    }

    public ServicioResponse crear(ServicioRequest req) {
        Servicio servicio = Servicio.builder()
                .codigo(generarCodigo())
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .categoria(req.categoria())
                .precio(req.precio())
                .duracionMinutos(req.duracionMinutos())
                .activo(req.activo() == null || req.activo())
                .build();
        return ServicioResponse.from(repository.save(servicio));
    }

    public ServicioResponse actualizar(UUID id, ServicioRequest req) {
        Servicio servicio = buscarOFallar(id);
        servicio.setNombre(req.nombre());
        servicio.setDescripcion(req.descripcion());
        servicio.setCategoria(req.categoria());
        servicio.setPrecio(req.precio());
        servicio.setDuracionMinutos(req.duracionMinutos());
        if (req.activo() != null) {
            servicio.setActivo(req.activo());
        }
        return ServicioResponse.from(repository.save(servicio));
    }

    /** Baja logica: las ordenes historicas siguen referenciando el servicio. */
    public void desactivar(UUID id) {
        Servicio servicio = buscarOFallar(id);
        servicio.setActivo(false);
        repository.save(servicio);
    }

    private Servicio buscarOFallar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio %s no encontrado".formatted(id)));
    }

    /** SRV-0001, SRV-0002, ... correlativo sobre el total de servicios. */
    private String generarCodigo() {
        long siguiente = repository.count() + 1;
        String codigo = "SRV-%04d".formatted(siguiente);
        while (repository.findByCodigo(codigo).isPresent()) {
            codigo = "SRV-%04d".formatted(++siguiente);
        }
        return codigo;
    }
}
