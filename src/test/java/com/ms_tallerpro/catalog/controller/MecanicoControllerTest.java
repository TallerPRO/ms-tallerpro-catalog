package com.ms_tallerpro.catalog.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alta de mecanicos del taller: RUT valido y unico, y control de acceso por rol. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MecanicoControllerTest {

    private static final UUID TALLER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String cuerpo(String rut, String nombre, String correo) {
        return objectMapper.writeValueAsString(Map.of(
                "rut", rut, "nombre", nombre, "correo", correo, "telefono", "+56911111111"));
    }

    @Test
    void creaMecanicoNormalizandoElRutYLoDevuelveEnElListado() throws Exception {
        mockMvc.perform(post("/api/v1/talleres/{t}/mecanicos", TALLER)
                        .with(jwt().authorities(() -> "ROLE_JefeTaller"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("12.345.678-5", "Pedro Soto", "Pedro@Taller.cl")))
                .andExpect(status().isCreated())
                // Se guarda sin puntos y el correo en minusculas.
                .andExpect(jsonPath("$.rut").value("12345678-5"))
                .andExpect(jsonPath("$.correo").value("pedro@taller.cl"))
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get("/api/v1/talleres/{t}/mecanicos", TALLER)
                        .with(jwt().authorities(() -> "ROLE_Mecanico")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Pedro Soto"));
    }

    @Test
    void rechazaRutConDigitoVerificadorIncorrecto() throws Exception {
        mockMvc.perform(post("/api/v1/talleres/{t}/mecanicos", TALLER)
                        .with(jwt().authorities(() -> "ROLE_Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("12345678-9", "RUT Malo", "malo@taller.cl")))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazaRutDuplicadoEnElMismoTaller() throws Exception {
        UUID otroTaller = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/talleres/{t}/mecanicos", otroTaller)
                        .with(jwt().authorities(() -> "ROLE_Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("9.876.543-3", "Ana Rojas", "ana@taller.cl")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/talleres/{t}/mecanicos", otroTaller)
                        .with(jwt().authorities(() -> "ROLE_Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("98765433", "Ana Rojas Duplicada", "ana2@taller.cl")))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazaCorreoInvalidoYMecanicoSinPermisoDeAlta() throws Exception {
        mockMvc.perform(post("/api/v1/talleres/{t}/mecanicos", TALLER)
                        .with(jwt().authorities(() -> "ROLE_Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("11.111.111-1", "Correo Malo", "no-es-un-correo")))
                .andExpect(status().isBadRequest());

        // El Mecanico puede leer la lista, pero no dar de alta companeros.
        mockMvc.perform(post("/api/v1/talleres/{t}/mecanicos", TALLER)
                        .with(jwt().authorities(() -> "ROLE_Mecanico"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("11.111.111-1", "Sin Permiso", "sin@taller.cl")))
                .andExpect(status().isForbidden());
    }
}
