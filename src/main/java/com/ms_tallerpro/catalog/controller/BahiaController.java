package com.ms_tallerpro.catalog.controller;

import com.ms_tallerpro.catalog.dto.BahiaDtos.BahiaDisponibilidadResponse;
import com.ms_tallerpro.catalog.dto.BahiaDtos.BahiaRequest;
import com.ms_tallerpro.catalog.dto.BahiaDtos.BahiaResponse;
import com.ms_tallerpro.catalog.dto.BahiaDtos.ReservaBahiaRequest;
import com.ms_tallerpro.catalog.dto.BahiaDtos.ResumenOcupacion;
import com.ms_tallerpro.catalog.model.EstadoBahia;
import com.ms_tallerpro.catalog.service.BahiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

/**
 * Bahias de un taller. CRUD: Admin. Reserva / ocupacion / liberacion: Admin y Jefe de taller.
 * El taller viene en la ruta; en la Parte 3/6 se contrasta con el alcance del usuario (oid).
 */
@RestController
@RequestMapping("/api/v1/talleres/{tallerId}/bahias")
@RequiredArgsConstructor
@Tag(name = "Bahias", description = "Puestos de trabajo del taller y su ocupacion")
public class BahiaController {

    private final BahiaService service;

    @GetMapping
    @Operation(summary = "Listar bahias del taller, opcionalmente por estado")
    public List<BahiaResponse> listar(@PathVariable UUID tallerId, @RequestParam(required = false) EstadoBahia estado) {
        return service.listar(tallerId, estado);
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de ocupacion del taller (total, disponibles, reservadas, ocupadas, tasa)")
    public ResumenOcupacion resumen(@PathVariable UUID tallerId) {
        return service.resumen(tallerId);
    }

    @GetMapping("/{bahiaId}")
    @Operation(summary = "Obtener una bahia")
    public BahiaResponse obtener(@PathVariable UUID tallerId, @PathVariable UUID bahiaId) {
        return service.obtener(tallerId, bahiaId);
    }

    /** Contrato consumido por ms-tallerpro-jobs (CatalogClient#consultarDisponibilidadBahia). */
    @GetMapping("/{bahiaId}/disponibilidad")
    @Operation(summary = "Consultar si la bahia esta disponible para asignarla a una orden (RF-06)")
    public BahiaDisponibilidadResponse disponibilidad(@PathVariable UUID tallerId, @PathVariable UUID bahiaId) {
        return service.disponibilidad(tallerId, bahiaId);
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Crear bahia (Admin)")
    public ResponseEntity<BahiaResponse> crear(@PathVariable UUID tallerId, @Valid @RequestBody BahiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(tallerId, request));
    }

    @PutMapping("/{bahiaId}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Actualizar bahia (Admin)")
    public BahiaResponse actualizar(@PathVariable UUID tallerId, @PathVariable UUID bahiaId,
                                    @Valid @RequestBody BahiaRequest request) {
        return service.actualizar(tallerId, bahiaId, request);
    }

    @DeleteMapping("/{bahiaId}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Desactivar bahia (baja logica, Admin)")
    public ResponseEntity<Void> desactivar(@PathVariable UUID tallerId, @PathVariable UUID bahiaId) {
        service.desactivar(tallerId, bahiaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bahiaId}/reserva")
    @PreAuthorize("hasAnyRole('Admin', 'JefeTaller')")
    @Operation(summary = "Reservar la bahia para una orden: DISPONIBLE -> RESERVADA")
    public BahiaResponse reservar(@PathVariable UUID tallerId, @PathVariable UUID bahiaId,
                                  @Valid @RequestBody ReservaBahiaRequest request) {
        return service.reservar(tallerId, bahiaId, request);
    }

    @PostMapping("/{bahiaId}/ocupacion")
    @PreAuthorize("hasAnyRole('Admin', 'JefeTaller')")
    @Operation(summary = "Registrar ingreso del vehiculo: RESERVADA -> OCUPADA")
    public BahiaResponse ocupar(@PathVariable UUID tallerId, @PathVariable UUID bahiaId) {
        return service.ocupar(tallerId, bahiaId);
    }

    @PostMapping("/{bahiaId}/liberacion")
    @PreAuthorize("hasAnyRole('Admin', 'JefeTaller')")
    @Operation(summary = "Liberar la bahia (cancelar reserva o terminar trabajos): -> DISPONIBLE")
    public BahiaResponse liberar(@PathVariable UUID tallerId, @PathVariable UUID bahiaId) {
        return service.liberar(tallerId, bahiaId);
    }
}
