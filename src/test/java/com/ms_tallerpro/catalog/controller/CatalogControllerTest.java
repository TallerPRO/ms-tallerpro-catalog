package com.ms_tallerpro.catalog.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion sobre H2. Los JWT se simulan con spring-security-test
 * (jwt() con el claim "roles" tal como lo emite Azure AD), por lo que se prueban
 * las reglas 401 / 403 / 2xx de ARQUITECTURA_ACCESO.md sin tenant real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogControllerTest {

    private static final UUID TALLER = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Sin tenant en los tests: evita que Spring intente descargar el JWKS. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    /** Simula el token de Azure AD con app roles y el mapeo roles -> ROLE_* que hace SecurityConfig. */
    private static RequestPostProcessor conRol(String... roles) {
        return jwt()
                .jwt(j -> j.claim("oid", "user-" + roles[0]).claim("roles", List.of(roles)))
                .authorities(Arrays.stream(roles)
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                        .toList());
    }

    // ---------- Seguridad ----------

    @Test
    void sinTokenRespondeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/servicios")).andExpect(status().isUnauthorized());
    }

    @Test
    void jefeTallerNoPuedeCrearServicio() throws Exception {
        mockMvc.perform(post("/api/v1/servicios").with(conRol("JefeTaller"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson("Alineacion")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void usuarioSinRolesPuedeLeerPeroNoEscribir() throws Exception {
        mockMvc.perform(get("/api/v1/servicios").with(jwt())).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/servicios").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(servicioJson("X")))
                .andExpect(status().isForbidden());
    }

    // ---------- Servicios ----------

    @Test
    void adminCreaListaActualizaYDesactivaServicio() throws Exception {
        String body = mockMvc.perform(post("/api/v1/servicios").with(conRol("Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson("Cambio de aceite")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(org.hamcrest.Matchers.startsWith("SRV-")))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(get("/api/v1/servicios").with(conRol("Cliente")).param("busqueda", "aceite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/v1/servicios/{id}", id).with(conRol("Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson("Cambio de aceite sintetico")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cambio de aceite sintetico"));

        mockMvc.perform(delete("/api/v1/servicios/{id}", id).with(conRol("Admin")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/servicios/{id}", id).with(conRol("Admin")))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void servicioInexistenteRespondeNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/servicios/{id}", UUID.randomUUID()).with(conRol("Admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void servicioInvalidoRespondeBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/servicios").with(conRol("Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"categoria\":\"FRENOS\",\"precio\":-1,\"duracionMinutos\":0}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Bahias ----------

    @Test
    void flujoCompletoDeBahia() throws Exception {
        UUID bahiaId = crearBahia("A-01");
        UUID ordenId = UUID.randomUUID();

        // Contrato que usa ms-tallerpro-jobs
        mockMvc.perform(get("/api/v1/talleres/{t}/bahias/{b}/disponibilidad", TALLER, bahiaId).with(conRol("JefeTaller")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bahiaId").value(bahiaId.toString()))
                .andExpect(jsonPath("$.tallerId").value(TALLER.toString()))
                .andExpect(jsonPath("$.disponible").value(true));

        mockMvc.perform(post("/api/v1/talleres/{t}/bahias/{b}/reserva", TALLER, bahiaId).with(conRol("JefeTaller"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":\"" + ordenId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.ordenId").value(ordenId.toString()));

        mockMvc.perform(get("/api/v1/talleres/{t}/bahias/{b}/disponibilidad", TALLER, bahiaId).with(conRol("JefeTaller")))
                .andExpect(jsonPath("$.disponible").value(false));

        // Otra orden no puede reservar la misma bahia
        mockMvc.perform(post("/api/v1/talleres/{t}/bahias/{b}/reserva", TALLER, bahiaId).with(conRol("JefeTaller"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/talleres/{t}/bahias/{b}/ocupacion", TALLER, bahiaId).with(conRol("JefeTaller")))
                .andExpect(jsonPath("$.estado").value("OCUPADA"));

        mockMvc.perform(get("/api/v1/talleres/{t}/bahias/resumen", TALLER).with(conRol("Admin")))
                .andExpect(jsonPath("$.ocupadas").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.tasaOcupacion").value(100.0));

        mockMvc.perform(post("/api/v1/talleres/{t}/bahias/{b}/liberacion", TALLER, bahiaId).with(conRol("JefeTaller")))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.ordenId").doesNotExist());

        // Un Mecanico no puede reservar
        mockMvc.perform(post("/api/v1/talleres/{t}/bahias/{b}/reserva", TALLER, bahiaId).with(conRol("Mecanico"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":\"" + ordenId + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bahiaDeOtroTallerNoEsVisible() throws Exception {
        UUID bahiaId = crearBahia("B-01");
        mockMvc.perform(get("/api/v1/talleres/{t}/bahias/{b}", UUID.randomUUID(), bahiaId).with(conRol("Admin")))
                .andExpect(status().isNotFound());
    }

    // ---------- Repuestos / stock ----------

    @Test
    void decrementoDeStockEsIdempotenteYRechazaStockInsuficiente() throws Exception {
        String body = mockMvc.perform(post("/api/v1/talleres/{t}/repuestos", TALLER).with(conRol("Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"BR-4521-T\",\"nombre\":\"Pastillas de freno\",\"marca\":\"Brembo\","
                                + "\"precioUnitario\":45000,\"stock\":5,\"stockMinimo\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bajoStock").value(false))
                .andReturn().getResponse().getContentAsString();
        UUID repuestoId = UUID.fromString(objectMapper.readTree(body).get("id").asText());
        String eventId = UUID.randomUUID().toString();

        // Contrato que usa ms-tallerpro-jobs: body {cantidad, eventId} + cabecera X-Event-Id
        mockMvc.perform(post("/api/v1/talleres/{t}/repuestos/{r}/stock/decremento", TALLER, repuestoId)
                        .with(conRol("JefeTaller"))
                        .header("X-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":3,\"eventId\":\"" + eventId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockResultante").value(2))
                .andExpect(jsonPath("$.duplicado").value(false));

        // Reintento con el mismo eventId: no vuelve a descontar
        mockMvc.perform(post("/api/v1/talleres/{t}/repuestos/{r}/stock/decremento", TALLER, repuestoId)
                        .with(conRol("JefeTaller"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":3,\"eventId\":\"" + eventId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockResultante").value(2))
                .andExpect(jsonPath("$.duplicado").value(true));

        mockMvc.perform(get("/api/v1/talleres/{t}/repuestos/{r}", TALLER, repuestoId).with(conRol("Admin")))
                .andExpect(jsonPath("$.stock").value(2))
                .andExpect(jsonPath("$.bajoStock").value(true));

        // Stock insuficiente -> 409
        mockMvc.perform(post("/api/v1/talleres/{t}/repuestos/{r}/stock/decremento", TALLER, repuestoId)
                        .with(conRol("JefeTaller"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":5}"))
                .andExpect(status().isConflict());

        // Filtro de stock bajo
        mockMvc.perform(get("/api/v1/talleres/{t}/repuestos", TALLER).with(conRol("Admin")).param("bajoStock", "true"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // Kardex
        mockMvc.perform(get("/api/v1/talleres/{t}/repuestos/{r}/stock/movimientos", TALLER, repuestoId).with(conRol("Auditor")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("DECREMENTO"));
    }

    @Test
    void codigoDeRepuestoDuplicadoEnMismoTallerRespondeConflict() throws Exception {
        String json = "{\"codigo\":\"DUP-1\",\"nombre\":\"Filtro\",\"precioUnitario\":1000,\"stock\":1,\"stockMinimo\":0}";
        mockMvc.perform(post("/api/v1/talleres/{t}/repuestos", TALLER).with(conRol("Admin"))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/talleres/{t}/repuestos", TALLER).with(conRol("Admin"))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isConflict());
        // El mismo codigo en OTRO taller si es valido (stock por taller)
        mockMvc.perform(post("/api/v1/talleres/{t}/repuestos", UUID.randomUUID()).with(conRol("Admin"))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated());
    }

    // ---------- helpers ----------

    private UUID crearBahia(String codigo) throws Exception {
        String body = mockMvc.perform(post("/api/v1/talleres/{t}/bahias", TALLER).with(conRol("Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"sector\":\"Mecanica General\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return UUID.fromString(node.get("id").asText());
    }

    private static String servicioJson(String nombre) {
        return "{\"nombre\":\"" + nombre + "\",\"categoria\":\"MANTENCION\",\"precio\":35000,\"duracionMinutos\":45}";
    }
}
