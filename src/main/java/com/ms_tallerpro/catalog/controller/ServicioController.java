package com.ms_tallerpro.catalog.controller;

import com.ms_tallerpro.catalog.dto.ServicioDtos.ServicioRequest;
import com.ms_tallerpro.catalog.dto.ServicioDtos.ServicioResponse;
import com.ms_tallerpro.catalog.model.CategoriaServicio;
import com.ms_tallerpro.catalog.service.ServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * MS-02 - ms-tallerpro-catalog: catalogo maestro de servicios (comun a toda la red).
 * Lectura: cualquier usuario autenticado. Escritura: solo Admin.
 */
@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios", description = "Catalogo de servicios y tarifas de la red TallerPro")
public class ServicioController {

    private final ServicioService service;

    @GetMapping
    @Operation(summary = "Listar servicios (busqueda por nombre/codigo, filtro por categoria, paginado)")
    public Page<ServicioResponse> listar(@RequestParam(required = false) String busqueda,
                                         @RequestParam(required = false) CategoriaServicio categoria,
                                         @RequestParam(defaultValue = "true") boolean soloActivos,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return service.listar(busqueda, categoria, soloActivos, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un servicio por id")
    public ServicioResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Crear servicio (Admin)")
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Actualizar servicio / tarifa (Admin)")
    public ServicioResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ServicioRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Desactivar servicio (baja logica, Admin)")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
