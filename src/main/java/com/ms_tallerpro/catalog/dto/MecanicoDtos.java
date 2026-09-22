package com.ms_tallerpro.catalog.dto;

import com.ms_tallerpro.catalog.model.Mecanico;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class MecanicoDtos {

    /**
     * El RUT se valida en el servicio (formato + digito verificador) porque una
     * expresion regular no alcanza: hay que calcular el DV.
     */
    public record MecanicoRequest(
            @NotBlank @Size(max = 12) String rut,
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Email @Size(max = 150) String correo,
            @Size(max = 30) String telefono,
            Boolean activo
    ) {}

    public record MecanicoResponse(
            UUID id,
            UUID tallerId,
            String rut,
            String nombre,
            String correo,
            String telefono,
            boolean activo,
            Instant fechaActualizacion
    ) {
        public static MecanicoResponse from(Mecanico m) {
            return new MecanicoResponse(m.getId(), m.getTallerId(), m.getRut(), m.getNombre(),
                    m.getCorreo(), m.getTelefono(), m.isActivo(), m.getFechaActualizacion());
        }
    }
}
