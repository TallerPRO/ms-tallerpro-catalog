package com.ms_tallerpro.catalog.dto;

import com.ms_tallerpro.catalog.model.CategoriaServicio;
import com.ms_tallerpro.catalog.model.Servicio;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ServicioDtos {

    /** Cuerpo de POST/PUT. El codigo lo genera el servidor. */
    public record ServicioRequest(
            @NotBlank @Size(max = 120) String nombre,
            String descripcion,
            @NotNull CategoriaServicio categoria,
            @NotNull @DecimalMin("0.0") BigDecimal precio,
            @NotNull @Min(1) Integer duracionMinutos,
            Boolean activo
    ) {}

    public record ServicioResponse(
            UUID id,
            String codigo,
            String nombre,
            String descripcion,
            CategoriaServicio categoria,
            BigDecimal precio,
            Integer duracionMinutos,
            boolean activo,
            Instant fechaActualizacion
    ) {
        public static ServicioResponse from(Servicio s) {
            return new ServicioResponse(s.getId(), s.getCodigo(), s.getNombre(), s.getDescripcion(), s.getCategoria(),
                    s.getPrecio(), s.getDuracionMinutos(), s.isActivo(), s.getFechaActualizacion());
        }
    }
}
