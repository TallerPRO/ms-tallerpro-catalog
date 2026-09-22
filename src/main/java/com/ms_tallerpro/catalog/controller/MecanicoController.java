package com.ms_tallerpro.catalog.controller;

import com.ms_tallerpro.catalog.dto.MecanicoDtos.MecanicoRequest;
import com.ms_tallerpro.catalog.dto.MecanicoDtos.MecanicoResponse;
import com.ms_tallerpro.catalog.service.MecanicoService;
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
 * Mecanicos de un taller. Alta y edicion: Admin y Jefe de taller (son ellos
 * quienes arman el equipo). Lectura: ademas el Mecanico, porque la pantalla de
 * asignacion de trabajo necesita la lista.
 */
@RestController
@RequestMapping("/api/v1/talleres/{tallerId}/mecanicos")
@RequiredArgsConstructor
@Tag(name = "Mecanicos", description = "Equipo de trabajo del taller")
public class MecanicoController {

    private static final String GESTION = "hasAnyRole('Admin', 'JefeTaller')";
    private static final String LECTURA = "hasAnyRole('Admin', 'JefeTaller', 'Mecanico')";

    private final MecanicoService service;

    @GetMapping
    @PreAuthorize(LECTURA)
    @Operation(summary = "Listar mecanicos del taller")
    public List<MecanicoResponse> listar(@PathVariable UUID tallerId,
                                          @RequestParam(defaultValue = "true") boolean soloActivos) {
        return service.listar(tallerId, soloActivos);
    }

    @GetMapping("/{mecanicoId}")
    @PreAuthorize(LECTURA)
    @Operation(summary = "Obtener un mecanico del taller")
    public MecanicoResponse obtener(@PathVariable UUID tallerId, @PathVariable UUID mecanicoId) {
        return service.obtener(tallerId, mecanicoId);
    }

    @PostMapping
    @PreAuthorize(GESTION)
    @Operation(summary = "Crear mecanico (RUT unico por taller, con digito verificador valido)")
    public ResponseEntity<MecanicoResponse> crear(@PathVariable UUID tallerId,
                                                   @Valid @RequestBody MecanicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(tallerId, request));
    }

    @PutMapping("/{mecanicoId}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Actualizar mecanico")
    public MecanicoResponse actualizar(@PathVariable UUID tallerId, @PathVariable UUID mecanicoId,
                                        @Valid @RequestBody MecanicoRequest request) {
        return service.actualizar(tallerId, mecanicoId, request);
    }

    @DeleteMapping("/{mecanicoId}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Dar de baja un mecanico (baja logica)")
    public ResponseEntity<Void> desactivar(@PathVariable UUID tallerId, @PathVariable UUID mecanicoId) {
        service.desactivar(tallerId, mecanicoId);
        return ResponseEntity.noContent().build();
    }
}
