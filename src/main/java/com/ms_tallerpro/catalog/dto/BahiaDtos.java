package com.ms_tallerpro.catalog.dto;

import com.ms_tallerpro.catalog.model.Bahia;
import com.ms_tallerpro.catalog.model.EstadoBahia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class BahiaDtos {

    public record BahiaRequest(
            @NotBlank @Size(max = 10) String codigo,
            @NotBlank @Size(max = 60) String sector,
            Boolean activa
    ) {}

    public record BahiaResponse(
            UUID id,
            UUID tallerId,
            String codigo,
            String sector,
            EstadoBahia estado,
            UUID ordenId,
            Instant inicioProgramado,
            Instant terminoProgramado,
            Instant libreDesde,
            boolean activa,
            Instant fechaActualizacion
    ) {
        public static BahiaResponse from(Bahia b) {
            return new BahiaResponse(b.getId(), b.getTallerId(), b.getCodigo(), b.getSector(), b.getEstado(), b.getOrdenId(),
                    b.getInicioProgramado(), b.getTerminoProgramado(), b.getLibreDesde(), b.isActiva(), b.getFechaActualizacion());
        }
    }

    /** Contrato esperado por ms-tallerpro-jobs (BahiaDisponibilidadResponse). */
    public record BahiaDisponibilidadResponse(UUID bahiaId, UUID tallerId, boolean disponible) {}

    /** Reserva de la bahia para una orden (RF-06). Fechas opcionales. */
    public record ReservaBahiaRequest(
            @NotNull UUID ordenId,
            Instant inicioProgramado,
            Instant terminoProgramado
    ) {}

    /** Resumen de ocupacion del taller para el dashboard. */
    public record ResumenOcupacion(
            UUID tallerId,
            long total,
            long disponibles,
            long reservadas,
            long ocupadas,
            /** 0-100 */
            double tasaOcupacion,
            Instant proximaLiberacion,
            String proximaLiberacionCodigo
    ) {}
}
