package co.edu.uq.sga.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la API REST con los primeros usuarios del escenario inicial. */
class ApiIntegracionTest extends PruebaIntegracion {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String usuario, String clave) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"clave\":\"" + clave + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("token").asText();
    }

    private String admin() throws Exception {
        return login("admin", "Admin123*");
    }

    private String recepcion() throws Exception {
        return login("recepcion", "Recep123*");
    }

    private String laura() throws Exception {
        return login("laura.gomez@correo.com", "Huesped123*");
    }

    private ResultActions con(String token, MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON));
    }

    private JsonNode cuerpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    @Test
    void losPrimerosUsuariosInicianSesionConSuRol() throws Exception {
        for (String[] u : new String[][]{{"admin", "Admin123*", "ADMINISTRADOR"}, {"recepcion", "Recep123*", "RECEPCIONISTA"},
                {"phenao", "Recep123*", "RECEPCIONISTA"}, {"darias", "Recep123*", "RECEPCIONISTA"},
                {"laura.gomez@correo.com", "Huesped123*", "HUESPED"}, {"1094000002", "Huesped123*", "HUESPED"}}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"usuario\":\"" + u[0] + "\",\"clave\":\"" + u[1] + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value(u[2]));
        }
    }

    @Test
    void credencialesInvalidasYTokensFalsos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"clave\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
        con("token-falso", get("/api/reservas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reportes/tablero")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadaRolSoloAccedeASusOperaciones() throws Exception {
        con(laura(), get("/api/empleados")).andExpect(status().isForbidden());
        con(laura(), get("/api/reservas/R-00018")).andExpect(status().isForbidden());
        con(laura(), get("/api/reservas/R-00024")).andExpect(status().isOk());
        con(recepcion(), get("/api/reportes/estadisticas")).andExpect(status().isForbidden());
        con(admin(), get("/api/reportes/estadisticas")).andExpect(status().isOk());
        con(recepcion(), post("/api/apartamentos").content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void catalogoPublicoYOrdenamientos() throws Exception {
        mvc.perform(get("/api/apartamentos")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(15)))
                .andExpect(jsonPath("$[0].codigo").value("A101"));
        mvc.perform(get("/api/apartamentos?orden=capacidad")).andExpect(jsonPath("$[0].codigo").value("C201"));
        mvc.perform(get("/api/apartamentos?orden=precio")).andExpect(jsonPath("$[0].codigo").value("A202"));
        mvc.perform(get("/api/apartamentos/zzz")).andExpect(status().isNotFound());
        mvc.perform(get("/api/temporadas")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(21)));
    }

    @Test
    void registroDeHuespedYReservaCompletaPorElPortal() throws Exception {
        JsonNode sesion = cuerpo(mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"documento":"555","nombre":"Nuevo Huésped","fechaNacimiento":"1990-02-02","telefono":"3001234567",
                         "correo":"nuevo@correo.com","clave":"Clave1234"}""")).andExpect(status().isCreated()));
        String token = sesion.get("token").asText();
        assertThat(sesion.get("rol").asText()).isEqualTo("HUESPED");

        JsonNode reserva = cuerpo(con(token, post("/api/reservas/portal").content("""
                {"apartamentoCodigo":"B102","entrada":"2026-12-20","salida":"2026-12-23",
                 "ocupantes":[{"nombre":"Nuevo Huésped","fechaNacimiento":"1990-02-02"},
                              {"nombre":"Hija","fechaNacimiento":"2023-06-01"}]}""")).andExpect(status().isCreated()));
        String codigo = reserva.get("codigo").asText();
        assertThat(reserva.get("estado").asText()).isEqualTo("PENDIENTE");
        assertThat(reserva.get("desglose")).hasSize(3);
        assertThat(reserva.get("ocupantes").get(1).get("paga").asBoolean()).isFalse();

        con(token, post("/api/reservas/" + codigo + "/folio/anticipo")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPagado").value(reserva.get("anticipoRequerido").asLong()));
        con(token, get("/api/reservas/" + codigo)).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        con(token, get("/api/reservas/" + codigo + "/cancelacion")).andExpect(jsonPath("$.porcentaje").value(100));
        con(token, post("/api/reservas/" + codigo + "/cancelar").content("{\"motivo\":\"Prueba\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.saldo").value(0));
        con(token, get("/api/huespedes/555/reservas")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void erroresDeNegocioYDeValidacionTienenCodigosClaros() throws Exception {
        String r = recepcion();
        String directa = """
                {"titular":{"documento":"777","nombre":"Cliente Directo","fechaNacimiento":"1980-01-01",
                            "telefono":"3000000000","correo":"directo@correo.com"},
                 "apartamentoCodigo":"%s","entrada":"%s","salida":"%s",
                 "ocupantes":[{"nombre":"Cliente Directo","fechaNacimiento":"1980-01-01"}%s]}""";
        con(r, post("/api/reservas/directa").content(directa.formatted("A101", "2026-11-27", "2026-11-28", "")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("RESERVA_SOLAPADA"));
        con(r, post("/api/reservas/directa").content(directa.formatted("A202", "2026-12-20", "2026-12-21",
                ",{\"nombre\":\"B\",\"fechaNacimiento\":\"1980-01-01\"},{\"nombre\":\"C\",\"fechaNacimiento\":\"1980-01-01\"}")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("CAPACIDAD_EXCEDIDA"));
        con(r, post("/api/reservas/directa").content(directa.formatted("A202", "2026-12-21", "2026-12-20", "")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("FECHAS_INVALIDAS"));
        con(r, post("/api/reservas/R-00019/check-in")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRANSICION_INVALIDA"));
        con(r, post("/api/reservas/directa").content("{\"apartamentoCodigo\":\"A101\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDACION"));
        con(r, post("/api/reservas/directa").content("{no es json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("FORMATO_INVALIDO"));
        con(r, post("/api/preparacion/despachar")).andExpect(status().isOk());
        con(r, post("/api/preparacion/despachar")).andExpect(status().isOk());
        con(r, post("/api/preparacion/despachar")).andExpect(status().isOk());
        con(r, post("/api/preparacion/despachar")).andExpect(status().isOk());
        con(r, post("/api/preparacion/despachar")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ESTRUCTURA_VACIA"));
    }

    @Test
    void crudDeApartamentosYEstadoFueraDeServicio() throws Exception {
        String a = admin();
        String datos = """
                {"codigo":"D101","nombre":"Nuevo","bloque":"D","piso":1,"dormitorios":1,"capacidad":2,
                 "caracteristicas":["Wi-Fi"],"tarifaBaja":90000,"tarifaMedia":100000,"tarifaAlta":120000}""";
        con(a, post("/api/apartamentos").content(datos)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PREPARADO"));
        con(a, post("/api/apartamentos").content(datos)).andExpect(status().isConflict());
        con(a, put("/api/apartamentos/D101").content(datos.replace("Nuevo", "Renovado")))
                .andExpect(jsonPath("$.nombre").value("Renovado"));
        con(a, patch("/api/apartamentos/D101/estado").content("{\"estado\":\"FUERA_SERVICIO\"}"))
                .andExpect(status().isUnprocessableEntity());
        con(a, patch("/api/apartamentos/D101/estado").content("{\"estado\":\"FUERA_SERVICIO\",\"motivo\":\"Pintura\"}"))
                .andExpect(jsonPath("$.motivoFueraServicio").value("Pintura"));
        con(a, delete("/api/apartamentos/D101")).andExpect(status().isOk());
        con(a, delete("/api/apartamentos/A101")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value(containsString("reserva activa")));
    }

    @Test
    void mapaRutasYAnalisisDelGrafo() throws Exception {
        String r = recepcion();
        con(r, get("/api/mapa")).andExpect(jsonPath("$.nodos", hasSize(27))).andExpect(jsonPath("$.pasillos", hasSize(29)));
        JsonNode ruta = cuerpo(con(r, get("/api/mapa/ruta?origen=RECEPCION&destino=C201")));
        assertThat(ruta.get("camino").toString()).isEqualTo("[\"RECEPCION\",\"ESC-C1\",\"ESC-C2\",\"C201\"]");
        con(r, get("/api/mapa/simular-cierre?a=ESC-C1&b=ESC-C2")).andExpect(jsonPath("$", hasSize(2)));
        con(r, get("/api/mapa/bloques")).andExpect(jsonPath("$", hasSize(3)));
        con(r, get("/api/preparacion/ruta?algoritmo=DIJKSTRA"))
                .andExpect(jsonPath("$.paradas[0]").value("B101"))
                .andExpect(jsonPath("$.paradas[1]").value("A201"));
        String a = admin();
        con(a, patch("/api/mapa/pasillos/estado").content("{\"origen\":\"ESC-C1\",\"destino\":\"ESC-C2\",\"habilitado\":false}"))
                .andExpect(jsonPath("$.pasillos[?(@.origen=='ESC-C1' && @.destino=='ESC-C2')].habilitado").value(false));
        con(r, get("/api/mapa/inalcanzables")).andExpect(jsonPath("$", hasSize(2)));
        con(r, get("/api/mapa/ruta?origen=RECEPCION&destino=C201")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reportesCalendarioYTurnos() throws Exception {
        String r = recepcion();
        con(r, get("/api/reportes/tablero")).andExpect(jsonPath("$.apartamentos").value(15))
                .andExpect(jsonPath("$.llegadasHoy").value(3)).andExpect(jsonPath("$.fueraServicio").value(1));
        JsonNode cal = cuerpo(con(r, get("/api/reportes/calendario?mes=2026-11")));
        assertThat(cal.get("filas")).hasSize(15);
        JsonNode b301 = cal.get("filas").get(10);
        assertThat(b301.get("apartamento").asText()).isEqualTo("B301");
        assertThat(b301.get("celdas").get(20).get("estado").asText()).isEqualTo("BLOQUEADA");
        JsonNode stats = cuerpo(con(admin(), get("/api/reportes/estadisticas?desde=2026-01-01&hasta=2026-12-31")));
        int total = 0;
        for (JsonNode c : stats.get("reservasPorCanal")) {
            total += c.get("total").asInt();
        }
        assertThat(total).isEqualTo(30);
        assertThat(stats.get("conflictos").get("conflictos").asInt()).isEqualTo(2);
        assertThat(stats.get("preparacion").get("registros").asInt()).isEqualTo(12);
        assertThat(stats.get("ingresosPorMes")).hasSize(12);

        con(r, get("/api/empleados/turno")).andExpect(jsonPath("$.actual.usuario").value("recepcion"));
        con(r, post("/api/empleados/turno/rotar")).andExpect(jsonPath("$.actual.usuario").value("phenao"));
        con(r, post("/api/empleados/turno/rotar")).andExpect(jsonPath("$.actual.usuario").value("darias"));
        con(r, post("/api/empleados/turno/rotar")).andExpect(jsonPath("$.actual.usuario").value("recepcion"));
    }

    @Test
    void cargaDeEscenarioDesdeArchivoPlano() throws Exception {
        String a = admin();
        MockMultipartFile malo = new MockMultipartFile("archivo", "x.json", "application/json", "{malo".getBytes());
        mvc.perform(multipart("/api/admin/escenario/archivo").file(malo).header("Authorization", "Bearer " + a))
                .andExpect(status().isUnprocessableEntity());
        // los datos anteriores siguen intactos
        mvc.perform(get("/api/apartamentos")).andExpect(jsonPath("$", hasSize(15)));
        con(a, post("/api/admin/escenario")).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(containsString("15 apartamentos")));
    }
}
