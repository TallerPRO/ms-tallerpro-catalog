package com.ms_tallerpro.catalog.controller;

import com.ms_tallerpro.catalog.dto.RepuestoDtos.MovimientoStockRequest;
import com.ms_tallerpro.catalog.dto.RepuestoDtos.MovimientoStockResponse;
import com.ms_tallerpro.catalog.dto.RepuestoDtos.RepuestoRequest;
import com.ms_tallerpro.catalog.dto.RepuestoDtos.RepuestoResponse;
import com.ms_tallerpro.catalog.service.RepuestoService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Repuestos e inventario POR TALLER. Las rutas llevan siempre el tallerId
 * porque el stock es local a cada sucursal (RF-08).
 */
@RestController
@RequestMapping("/api/v1/talleres/{tallerId}/repuestos")
@RequiredArgsConstructor
@Tag(name = "Repuestos", description = "Inventario de repuestos por taller y movimientos de stock")
public class RepuestoController {

    private final RepuestoService service;

    @GetMapping
    @Operation(summary = "Listar repuestos del taller (busqueda, filtro de stock bajo, paginado)")
    public Page<RepuestoResponse> listar(@PathVariable UUID tallerId,
                                         @RequestParam(required = false) String busqueda,
                                         @RequestParam(defaultValue = "false") boolean bajoStock,
                                         @RequestParam(defaultValue = "true") boolean soloActivos,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return service.listar(tallerId, busqueda, bajoStock, soloActivos, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{repuestoId}")
    @Operation(summary = "Obtener un repuesto del taller")
    public RepuestoResponse obtener(@PathVariable UUID tallerId, @PathVariable UUID repuestoId) {
        return service.obtener(tallerId, repuestoId);
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Crear repuesto en el taller (Admin)")
    public ResponseEntity<RepuestoResponse> crear(@PathVariable UUID tallerId, @Valid @RequestBody RepuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(tallerId, request));
    }

    @PutMapping("/{repuestoId}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Actualizar repuesto / precio / stock (Admin)")
    public RepuestoResponse actualizar(@PathVariable UUID tallerId, @PathVariable UUID repuestoId,
                                       @Valid @RequestBody RepuestoRequest request) {
        return service.actualizar(tallerId, repuestoId, request);
    }

    @DeleteMapping("/{repuestoId}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Desactivar repuesto (baja logica, Admin)")
    public ResponseEntity<Void> desactivar(@PathVariable UUID tallerId, @PathVariable UUID repuestoId) {
        service.desactivar(tallerId, repuestoId);
        return ResponseEntity.noContent().build();
    }

    /** Contrato consumido por ms-tallerpro-jobs (CatalogClient#solicitarDisminucionStock). */
    @PostMapping("/{repuestoId}/stock/decremento")
    @PreAuthorize("hasAnyRole('Admin', 'JefeTaller')")
    @Operation(summary = "Descontar stock al diagnosticar una orden (RF-08). Idempotente por eventId / X-Event-Id")
    public MovimientoStockResponse decrementar(@PathVariable UUID tallerId, @PathVariable UUID repuestoId,
                                               @Valid @RequestBody MovimientoStockRequest request,
                                               @RequestHeader(value = "X-Event-Id", required = false) String eventId) {
        return service.decrementarStock(tallerId, repuestoId, request, eventId);
    }

    @PostMapping("/{repuestoId}/stock/incremento")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Reponer stock (Admin). Idempotente por eventId / X-Event-Id")
    public MovimientoStockResponse incrementar(@PathVariable UUID tallerId, @PathVariable UUID repuestoId,
                                               @Valid @RequestBody MovimientoStockRequest request,
                                               @RequestHeader(value = "X-Event-Id", required = false) String eventId) {
        return service.incrementarStock(tallerId, repuestoId, request, eventId);
    }

    @GetMapping("/{repuestoId}/stock/movimientos")
    @PreAuthorize("hasAnyRole('Admin', 'JefeTaller', 'Auditor')")
    @Operation(summary = "Historial de movimientos de stock del repuesto (kardex)")
    public List<MovimientoStockResponse> movimientos(@PathVariable UUID tallerId, @PathVariable UUID repuestoId) {
        return service.historial(tallerId, repuestoId);
    }
}
