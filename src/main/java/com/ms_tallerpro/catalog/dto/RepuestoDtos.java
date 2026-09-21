package com.ms_tallerpro.catalog.dto;

import com.ms_tallerpro.catalog.model.MovimientoStock;
import com.ms_tallerpro.catalog.model.Repuesto;
import com.ms_tallerpro.catalog.model.TipoMovimientoStock;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RepuestoDtos {

    public record RepuestoRequest(
            @NotBlank @Size(max = 40) String codigo,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 80) String marca,
            @NotNull @DecimalMin("0.0") BigDecimal precioUnitario,
            @NotNull @Min(0) Integer stock,
            @NotNull @Min(0) Integer stockMinimo,
            Boolean activo
    ) {}

    public record RepuestoResponse(
            UUID id,
            UUID tallerId,
            String codigo,
            String nombre,
            String marca,
            BigDecimal precioUnitario,
            Integer stock,
            Integer stockMinimo,
            boolean bajoStock,
            boolean activo,
            Instant fechaActualizacion
    ) {
        public static RepuestoResponse from(Repuesto r) {
            return new RepuestoResponse(r.getId(), r.getTallerId(), r.getCodigo(), r.getNombre(), r.getMarca(),
                    r.getPrecioUnitario(), r.getStock(), r.getStockMinimo(), r.bajoStock(), r.isActivo(),
                    r.getFechaActualizacion());
        }
    }

    /**
     * Cuerpo que envia ms-tallerpro-jobs (CatalogClient#solicitarDisminucionStock):
     * { "cantidad": n, "eventId": "..." }. El eventId tambien puede venir en X-Event-Id.
     */
    public record MovimientoStockRequest(
            @NotNull @Min(1) Integer cantidad,
            String eventId,
            @Size(max = 200) String motivo
    ) {}

    public record MovimientoStockResponse(
            UUID id,
            String eventId,
            UUID repuestoId,
            UUID tallerId,
            TipoMovimientoStock tipo,
            Integer cantidad,
            Integer stockResultante,
            String motivo,
            Instant fecha,
            /** true si el eventId ya habia sido procesado y no se volvio a descontar. */
            boolean duplicado
    ) {
        public static MovimientoStockResponse from(MovimientoStock m, boolean duplicado) {
            return new MovimientoStockResponse(m.getId(), m.getEventId(), m.getRepuestoId(), m.getTallerId(), m.getTipo(),
                    m.getCantidad(), m.getStockResultante(), m.getMotivo(), m.getFecha(), duplicado);
        }
    }
}
