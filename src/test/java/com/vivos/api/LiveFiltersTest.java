package com.vivos.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Filtros de GET /lives y GET /lives/summary. */
class LiveFiltersTest extends AbstractIntegrationTest {

    private static final String INVALID_RANGE = "El minimo no puede ser mayor que el maximo";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String idOf(MockHttpServletRequestBuilder request, TestStore store, String body) throws Exception {
        String response = mockMvc.perform(request.with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String client(TestStore store, String name) throws Exception {
        return idOf(post("/clients"), store, "{\"fullName\":\"" + name + "\"}");
    }

    private String live(TestStore store, String title, boolean started) throws Exception {
        String liveId = idOf(post("/lives"), store, "{\"title\":\"" + title + "\"}");
        if (started) {
            mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store))).andExpect(status().isOk());
        }
        return liveId;
    }

    private void order(TestStore store, String liveId, String clientId, String status, String items)
            throws Exception {
        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, clientId).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\",\"items\":[" + items + "]}"))
                .andExpect(status().isOk());
    }

    private static String item(String price, int quantity) {
        return "{\"code\":\"001\",\"name\":\"Remera\",\"price\":" + price + ",\"quantity\":" + quantity + "}";
    }

    /** Fija createdAt (UTC), estado y duracion del vivo. */
    private void setLive(String liveId, String createdAt, String status, long accumulatedMs,
                         OffsetDateTime runningSince) {
        jdbcTemplate.update("""
                        update viv_live set created_at = ?, status = ?, accumulated_ms = ?, running_since = ?
                        where id = ?""",
                OffsetDateTime.parse(createdAt), status, accumulatedMs, runningSince, UUID.fromString(liveId));
    }

    private ResultActions getAs(TestStore store, String path, String... params) throws Exception {
        MockHttpServletRequestBuilder request = get(path).with(admin(store));
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    private void expectLives(TestStore store, String[] params, String... ids) throws Exception {
        ResultActions result = getAs(store, "/lives", params).andExpect(status().isOk());
        if (ids.length == 0) {
            result.andExpect(jsonPath("$", empty()));
        } else {
            result.andExpect(jsonPath("$[*].id", contains(ids)));
        }
    }

    private static String[] p(String... params) {
        return params;
    }

    @Test
    void liveFiltersAndSummary() throws Exception {
        TestStore store = createStore("Tienda filtros vivos", true);
        TestStore other = createStore("Otra tienda filtros vivos", true);

        String ana = client(store, "Ana");
        String bea = client(store, "Bea");
        String cami = client(store, "Cami");
        String dani = client(store, "Dani");

        // Horas en UTC; Montevideo es UTC-3.
        // primavera: 2026-10-10 00:00 local, 30 min. Compradoras 2, unidades 4, total 1589.5
        // (la venta cancelada y la vacia no cuentan).
        String primavera = live(store, "Vivo Primavera", true);
        order(store, primavera, ana, "PAID", item("100", 2) + "," + item("99.50", 1));
        order(store, primavera, bea, "PENDING", item("1290", 1));
        order(store, primavera, cami, "CANCELLED", item("50", 4));
        order(store, primavera, dani, "PENDING", "");
        setLive(primavera, "2026-10-10T03:00:00Z", "ENDED", 1_800_000, null);

        // invierno: 2026-10-10 23:59:59 local, 59 min 59.999 s (59 min). 1 compradora, 1 unidad, total 10.
        String invierno = live(store, "Liquidación invierno", true);
        order(store, invierno, ana, "PENDING", item("10", 1));
        setLive(invierno, "2026-10-11T02:59:59Z", "ENDED", 3_599_999, null);

        // enCurso: 2026-10-11 00:00 local, RUNNING: 10 min acumulados + 50 min del tramo actual = 60 min.
        String enCurso = live(store, "Primavera II", true);
        setLive(enCurso, "2026-10-11T03:00:00Z", "RUNNING", 600_000,
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(50).minusSeconds(5));

        // sinEmpezar: 2026-10-09 09:00 local, 0 min, sin ventas.
        String sinEmpezar = live(store, "Sin empezar", false);
        setLive(sinEmpezar, "2026-10-09T12:00:00Z", "NOT_STARTED", 0, null);

        String otherClient = client(other, "Ana");
        String otherLive = live(other, "Vivo Primavera", true);
        order(other, otherLive, otherClient, "PAID", item("999", 3));
        setLive(otherLive, "2026-10-10T12:00:00Z", "ENDED", 1_800_000, null);

        // Sin filtros (y con filtros vacios): todos, del mas nuevo al mas viejo, con sus totales.
        expectLives(store, p(), enCurso, invierno, primavera, sinEmpezar);
        expectLives(store, p("title", "  "), enCurso, invierno, primavera, sinEmpezar);
        getAs(store, "/lives")
                .andExpect(jsonPath("$[2].title").value("Vivo Primavera"))
                .andExpect(jsonPath("$[2].status").value("ENDED"))
                .andExpect(jsonPath("$[2].accumulatedMs").value(1_800_000))
                .andExpect(jsonPath("$[2].buyers").value(2))
                .andExpect(jsonPath("$[2].units").value(4))
                .andExpect(jsonPath("$[2].totalAmount").value(1589.5))
                .andExpect(jsonPath("$[0].buyers").value(0))
                .andExpect(jsonPath("$[0].units").value(0))
                .andExpect(jsonPath("$[0].totalAmount").value(0));

        // Fechas: dias completos en America/Montevideo, inclusive, sobre createdAt.
        expectLives(store, p("from", "2026-10-10", "to", "2026-10-10"), invierno, primavera);
        expectLives(store, p("from", "2026-10-11"), enCurso);
        expectLives(store, p("to", "2026-10-09"), sinEmpezar);
        expectLives(store, p("from", "2026-10-09", "to", "2026-10-10"), invierno, primavera, sinEmpezar);
        expectLives(store, p("from", "2026-10-12"));

        // Titulo: contiene, sin distinguir mayusculas, recortado; comodines literales.
        expectLives(store, p("title", "primavera"), enCurso, primavera);
        expectLives(store, p("title", "  LIQUIDACIÓN "), invierno);
        expectLives(store, p("title", "%"));

        // Duracion efectiva en minutos enteros, inclusive; la del vivo en curso llega hasta ahora.
        expectLives(store, p("minDurationMinutes", "60"), enCurso);
        expectLives(store, p("minDurationMinutes", "61"));
        expectLives(store, p("maxDurationMinutes", "59"), invierno, primavera, sinEmpezar);
        expectLives(store, p("minDurationMinutes", "30", "maxDurationMinutes", "59"), invierno, primavera);
        expectLives(store, p("minDurationMinutes", "31", "maxDurationMinutes", "59"), invierno);
        expectLives(store, p("minDurationMinutes", "60", "maxDurationMinutes", "60"), enCurso);
        expectLives(store, p("maxDurationMinutes", "0"), sinEmpezar);

        // Compradoras: sin canceladas ni ventas vacias.
        expectLives(store, p("minBuyers", "1"), invierno, primavera);
        expectLives(store, p("maxBuyers", "0"), enCurso, sinEmpezar);
        expectLives(store, p("minBuyers", "2", "maxBuyers", "2"), primavera);
        expectLives(store, p("minBuyers", "3"));

        // Unidades.
        expectLives(store, p("minUnits", "4"), primavera);
        expectLives(store, p("minUnits", "5"));
        expectLives(store, p("maxUnits", "1"), enCurso, invierno, sinEmpezar);
        expectLives(store, p("minUnits", "1", "maxUnits", "3"), invierno);

        // Monto total.
        expectLives(store, p("minTotal", "1589.5"), primavera);
        expectLives(store, p("minTotal", "1589.51"));
        expectLives(store, p("maxTotal", "1589.49"), enCurso, invierno, sinEmpezar);
        expectLives(store, p("minTotal", "10", "maxTotal", "10"), invierno);
        expectLives(store, p("minTotal", "0.01"), invierno, primavera);
        expectLives(store, p("minTotal", "0", "maxTotal", "0"), enCurso, sinEmpezar);

        // Combinados con AND.
        expectLives(store, p("title", "primavera", "minBuyers", "1"), primavera);
        expectLives(store, p("from", "2026-10-10", "maxDurationMinutes", "59", "minUnits", "1"),
                invierno, primavera);
        expectLives(store, p("title", "primavera", "minDurationMinutes", "60", "maxTotal", "0"), enCurso);
        expectLives(store, p("to", "2026-10-10", "minTotal", "11", "maxDurationMinutes", "30"), primavera);
        expectLives(store, p("title", "invierno", "minBuyers", "2"));

        // Resumen: buyers suma por vivo (Ana compro en dos vivos y cuenta dos veces).
        getAs(store, "/lives/summary").andExpect(status().isOk())
                .andExpect(jsonPath("$.lives").value(4))
                .andExpect(jsonPath("$.buyers").value(3))
                .andExpect(jsonPath("$.units").value(5))
                .andExpect(jsonPath("$.totalAmount").value(1599.5));
        getAs(store, "/lives/summary", "from", "2026-10-10", "to", "2026-10-10")
                .andExpect(jsonPath("$.lives").value(2))
                .andExpect(jsonPath("$.buyers").value(3))
                .andExpect(jsonPath("$.units").value(5))
                .andExpect(jsonPath("$.totalAmount").value(1599.5));
        getAs(store, "/lives/summary", "title", "primavera")
                .andExpect(jsonPath("$.lives").value(2))
                .andExpect(jsonPath("$.buyers").value(2))
                .andExpect(jsonPath("$.units").value(4))
                .andExpect(jsonPath("$.totalAmount").value(1589.5));
        getAs(store, "/lives/summary", "maxBuyers", "0")
                .andExpect(jsonPath("$.lives").value(2))
                .andExpect(jsonPath("$.buyers").value(0))
                .andExpect(jsonPath("$.units").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
        getAs(store, "/lives/summary", "title", "otoño")
                .andExpect(jsonPath("$.lives").value(0))
                .andExpect(jsonPath("$.buyers").value(0))
                .andExpect(jsonPath("$.units").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
        // totalAmount sin ceros de sobra.
        assertThat(getAs(store, "/lives/summary", "title", "invierno").andReturn().getResponse()
                .getContentAsString()).isEqualTo("{\"lives\":1,\"buyers\":1,\"units\":1,\"totalAmount\":10}");
        assertThat(getAs(store, "/lives/summary", "title", "otoño").andReturn().getResponse()
                .getContentAsString()).isEqualTo("{\"lives\":0,\"buyers\":0,\"units\":0,\"totalAmount\":0}");

        // Los vivos de otra tienda nunca aparecen.
        expectLives(store, p("minUnits", "3", "title", "vivo primavera"), primavera);
        expectLives(store, p("minTotal", "2997"));
        getAs(store, "/lives/summary", "minUnits", "3")
                .andExpect(jsonPath("$.lives").value(1))
                .andExpect(jsonPath("$.totalAmount").value(1589.5));
        expectLives(other, p(), otherLive);
        getAs(other, "/lives/summary")
                .andExpect(jsonPath("$.lives").value(1))
                .andExpect(jsonPath("$.buyers").value(1))
                .andExpect(jsonPath("$.units").value(3))
                .andExpect(jsonPath("$.totalAmount").value(2997));

        // /lives/summary no pisa /lives/{id} ni /lives/current.
        getAs(store, "/lives/" + primavera).andExpect(status().isOk())
                .andExpect(jsonPath("$.buyers").value(2));
        getAs(store, "/lives/current").andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(enCurso));
    }

    @Test
    void invalidLiveFiltersAreRejected() throws Exception {
        TestStore store = createStore("Tienda filtros vivos invalidos", true);
        String[][] pairs = {
                {"minDurationMinutes", "maxDurationMinutes"},
                {"minBuyers", "maxBuyers"},
                {"minUnits", "maxUnits"},
                {"minTotal", "maxTotal"},
        };

        for (String path : new String[]{"/lives", "/lives/summary"}) {
            getAs(store, path, "from", "2026-10-11", "to", "2026-10-10")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("La fecha desde no puede ser posterior a la fecha hasta"));
            getAs(store, path, "from", "2026-13-01")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro from no es valido"));
            getAs(store, path, "to", "10/10/2026")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro to no es valido"));

            for (String[] pair : pairs) {
                for (String name : pair) {
                    for (String value : new String[]{"-1", "abc"}) {
                        getAs(store, path, name, value)
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").value("El parametro " + name + " no es valido"));
                    }
                }
                getAs(store, path, pair[0], "5", pair[1], "4")
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail").value(INVALID_RANGE));
                // Iguales es valido.
                getAs(store, path, pair[0], "4", pair[1], "4").andExpect(status().isOk());
            }
            getAs(store, path, "minBuyers", "1.5")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro minBuyers no es valido"));
            getAs(store, path, "maxTotal", "-0.01")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro maxTotal no es valido"));
            getAs(store, path, "minTotal", "10.5", "maxTotal", "10.49")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(INVALID_RANGE));
            getAs(store, path, "from", "2026-10-10", "to", "2026-10-10", "minTotal", "0.5", "maxTotal", "0.5")
                    .andExpect(status().isOk());
        }
    }

    @Test
    void summaryRequiresAdmin() throws Exception {
        mockMvc.perform(get("/lives/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/lives/summary").with(superAdmin())).andExpect(status().isForbidden());
    }
}
