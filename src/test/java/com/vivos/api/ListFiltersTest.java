package com.vivos.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Filtros de GET /clients, GET /orders y GET /orders/summary. */
class ListFiltersTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String idOf(MockHttpServletRequestBuilder request, TestStore store, String body) throws Exception {
        String response = mockMvc.perform(request.with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String client(TestStore store, String body) throws Exception {
        return idOf(post("/clients"), store, body);
    }

    private String startedLive(TestStore store, String title) throws Exception {
        String liveId = idOf(post("/lives"), store, "{\"title\":\"" + title + "\"}");
        mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store))).andExpect(status().isOk());
        return liveId;
    }

    /** Crea la venta y fija su createdAt (UTC). */
    private String order(TestStore store, String liveId, String clientId, String status, String items,
                         String createdAt) throws Exception {
        String id = idOf(put("/lives/{l}/orders/{c}", liveId, clientId), store,
                "{\"status\":\"" + status + "\",\"items\":[" + items + "]}");
        jdbcTemplate.update("update viv_order set created_at = ? where id = ?",
                OffsetDateTime.parse(createdAt), UUID.fromString(id));
        return id;
    }

    private static String item(String price, int quantity) {
        return "{\"code\":\"001\",\"name\":\"Remera\",\"price\":" + price + ",\"quantity\":" + quantity + "}";
    }

    private ResultActions getAs(TestStore store, String path, String... params) throws Exception {
        MockHttpServletRequestBuilder request = get(path).with(admin(store));
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    // ----------------------------------------------------------------- clientas

    @Test
    void clientFilters() throws Exception {
        TestStore store = createStore("Tienda filtros clientas", true);
        TestStore other = createStore("Otra tienda filtros clientas", true);

        String lucia = client(store, """
                {"fullName":"Lucía Fernández","account":"luciaf","phone":"099 123 456",
                 "department":"MONTEVIDEO","deliveryType":"CADETERIA"}""");
        String luciaP = client(store, """
                {"fullName":"Lucía Pérez","account":"@lperez","phone":"098 555 000",
                 "department":"CANELONES","deliveryType":"AGENCIA"}""");
        String martina = client(store, """
                {"fullName":"Martina Pereira","account":"marpereira","phone":"091 123 000",
                 "department":"MONTEVIDEO","deliveryType":"DOMICILIO"}""");
        String sinDatos = client(store, "{\"fullName\":\"Sin datos\"}");
        client(other, """
                {"fullName":"Lucía de otra tienda","account":"luciaf","phone":"099 123 456",
                 "department":"MONTEVIDEO","deliveryType":"CADETERIA"}""");

        // Sin filtros (y con filtros vacios): todas las de la tienda, en orden de alta.
        getAs(store, "/clients").andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(lucia, luciaP, martina, sinDatos)));
        getAs(store, "/clients", "fullName", "  ", "account", "", "phone", " ", "search", "")
                .andExpect(jsonPath("$[*].id", contains(lucia, luciaP, martina, sinDatos)));

        // fullName: contiene, sin distinguir mayusculas, recortado.
        getAs(store, "/clients", "fullName", "  LUCÍA ")
                .andExpect(jsonPath("$[*].id", contains(lucia, luciaP)));
        getAs(store, "/clients", "fullName", "EZ")
                .andExpect(jsonPath("$[*].id", contains(lucia, luciaP)));
        // Los comodines de LIKE se buscan literalmente.
        getAs(store, "/clients", "fullName", "%").andExpect(jsonPath("$", empty()));

        // account: la "@" del filtro es opcional; solo busca en la cuenta.
        getAs(store, "/clients", "account", "luciaf").andExpect(jsonPath("$[*].id", contains(lucia)));
        getAs(store, "/clients", "account", "@luciaf").andExpect(jsonPath("$[*].id", contains(lucia)));
        getAs(store, "/clients", "account", " @LPER ").andExpect(jsonPath("$[*].id", contains(luciaP)));
        getAs(store, "/clients", "account", "pere").andExpect(jsonPath("$[*].id", contains(luciaP, martina)));
        getAs(store, "/clients", "account", "@pere").andExpect(jsonPath("$[*].id", contains(luciaP, martina)));
        getAs(store, "/clients", "account", "martina").andExpect(jsonPath("$", empty()));

        // phone: contiene.
        getAs(store, "/clients", "phone", "123").andExpect(jsonPath("$[*].id", contains(lucia, martina)));

        // department y deliveryType: exactos.
        getAs(store, "/clients", "department", "MONTEVIDEO")
                .andExpect(jsonPath("$[*].id", contains(lucia, martina)));
        getAs(store, "/clients", "deliveryType", "AGENCIA").andExpect(jsonPath("$[*].id", contains(luciaP)));
        getAs(store, "/clients", "department", "SALTO").andExpect(jsonPath("$", empty()));

        // Combinados con AND (incluido search).
        getAs(store, "/clients", "fullName", "lucía", "department", "MONTEVIDEO")
                .andExpect(jsonPath("$[*].id", contains(lucia)));
        getAs(store, "/clients", "department", "MONTEVIDEO", "deliveryType", "DOMICILIO", "phone", "123")
                .andExpect(jsonPath("$[*].id", contains(martina)));
        getAs(store, "/clients", "fullName", "martina", "deliveryType", "CADETERIA")
                .andExpect(jsonPath("$", empty()));
        getAs(store, "/clients", "search", "lucía", "department", "CANELONES")
                .andExpect(jsonPath("$[*].id", contains(luciaP)));
        getAs(store, "/clients", "search", "123", "account", "@mar")
                .andExpect(jsonPath("$[*].id", contains(martina)));

        // Valores invalidos.
        getAs(store, "/clients", "department", "BUENOS_AIRES")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El parametro department no es valido"));
        getAs(store, "/clients", "deliveryType", "DRON")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El parametro deliveryType no es valido"));
    }

    // ------------------------------------------------------------------- ventas

    @Test
    void orderFiltersAndSummary() throws Exception {
        TestStore store = createStore("Tienda filtros ventas", true);
        TestStore other = createStore("Otra tienda filtros ventas", true);

        String ana = client(store, "{\"fullName\":\"Ana\"}");
        String bea = client(store, "{\"fullName\":\"Bea\"}");
        String cami = client(store, "{\"fullName\":\"Cami\"}");
        String primavera = startedLive(store, "Vivo Primavera");
        String invierno = startedLive(store, "Liquidación invierno");

        // Horas en UTC; Montevideo es UTC-3.
        String o1 = order(store, primavera, ana, "PENDING", item("100", 2) + "," + item("99.50", 1),
                "2026-10-10T03:00:00Z");                           // 2026-10-10 00:00 local
        String o2 = order(store, primavera, bea, "PAID", item("1290", 1),
                "2026-10-11T02:30:00Z");                           // 2026-10-10 23:30 local
        String o3 = order(store, invierno, ana, "CANCELLED", item("50", 4),
                "2026-10-11T03:00:00Z");                           // 2026-10-11 00:00 local
        order(store, invierno, bea, "PENDING", "", "2026-10-10T12:00:00Z"); // sin items: nunca aparece
        String o0 = order(store, primavera, cami, "PAID", item("10", 1),
                "2026-10-10T02:59:59Z");                           // 2026-10-09 23:59:59 local
        mockMvc.perform(patch("/orders/{id}", o2).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageStatus\":\"SHIPPED\"}"))
                .andExpect(status().isOk());

        String otherClient = client(other, "{\"fullName\":\"Ana\"}");
        String otherLive = startedLive(other, "Vivo Primavera");
        String otherOrder = order(other, otherLive, otherClient, "PAID", item("999", 3), "2026-10-10T12:00:00Z");

        // Sin filtros: solo ventas con items, por vivo y numero desc (invierno es el vivo 2).
        getAs(store, "/orders").andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(o3, o0, o2, o1)));

        // Fechas: dias completos en America/Montevideo, inclusive.
        getAs(store, "/orders", "from", "2026-10-10", "to", "2026-10-10")
                .andExpect(jsonPath("$[*].id", contains(o2, o1)));
        getAs(store, "/orders", "from", "2026-10-11").andExpect(jsonPath("$[*].id", contains(o3)));
        getAs(store, "/orders", "to", "2026-10-09").andExpect(jsonPath("$[*].id", contains(o0)));
        getAs(store, "/orders", "from", "2026-10-11", "to", "2026-10-11")
                .andExpect(jsonPath("$[*].id", contains(o3)));
        getAs(store, "/orders", "from", "2026-10-09", "to", "2026-10-10")
                .andExpect(jsonPath("$[*].id", contains(o0, o2, o1)));
        getAs(store, "/orders", "from", "2026-10-12").andExpect(jsonPath("$", empty()));

        // Titulo del vivo: contiene, sin distinguir mayusculas.
        getAs(store, "/orders", "liveTitle", "primavera").andExpect(jsonPath("$[*].id", contains(o0, o2, o1)));
        getAs(store, "/orders", "liveTitle", "  LIQUIDACIÓN ").andExpect(jsonPath("$[*].id", contains(o3)));
        getAs(store, "/orders", "liveTitle", "otoño").andExpect(jsonPath("$", empty()));

        // Clienta, estado y estado del paquete: exactos.
        getAs(store, "/orders", "clientId", ana).andExpect(jsonPath("$[*].id", contains(o3, o1)));
        getAs(store, "/orders", "status", "PAID").andExpect(jsonPath("$[*].id", contains(o0, o2)));
        getAs(store, "/orders", "packageStatus", "SHIPPED").andExpect(jsonPath("$[*].id", contains(o2)));
        getAs(store, "/orders", "packageStatus", "PENDING").andExpect(jsonPath("$[*].id", contains(o3, o0, o1)));

        // Combinados.
        getAs(store, "/orders", "liveTitle", "primavera", "status", "PAID", "from", "2026-10-10")
                .andExpect(jsonPath("$[*].id", contains(o2)));
        getAs(store, "/orders", "clientId", ana, "to", "2026-10-10")
                .andExpect(jsonPath("$[*].id", contains(o1)));

        // La respuesta es la misma de siempre.
        getAs(store, "/orders", "clientId", ana, "status", "PENDING")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].number").value(1))
                .andExpect(jsonPath("$[0].liveId").value(primavera))
                .andExpect(jsonPath("$[0].items", hasSize(2)))
                .andExpect(jsonPath("$[0].items[1].price").value(99.5));

        // Resumen: todas las ventas filtradas, de cualquier estado.
        getAs(store, "/orders/summary").andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(4))
                .andExpect(jsonPath("$.units").value(9))
                .andExpect(jsonPath("$.totalAmount").value(1799.5));
        getAs(store, "/orders/summary", "from", "2026-10-10", "to", "2026-10-10")
                .andExpect(jsonPath("$.orders").value(2))
                .andExpect(jsonPath("$.units").value(4))
                .andExpect(jsonPath("$.totalAmount").value(1589.5));
        getAs(store, "/orders/summary", "clientId", ana)
                .andExpect(jsonPath("$.orders").value(2))
                .andExpect(jsonPath("$.units").value(7))
                .andExpect(jsonPath("$.totalAmount").value(499.5));
        getAs(store, "/orders/summary", "status", "CANCELLED")
                .andExpect(jsonPath("$.orders").value(1))
                .andExpect(jsonPath("$.units").value(4))
                .andExpect(jsonPath("$.totalAmount").value(200));
        getAs(store, "/orders/summary", "liveTitle", "primavera", "packageStatus", "PENDING")
                .andExpect(jsonPath("$.orders").value(2))
                .andExpect(jsonPath("$.units").value(4))
                .andExpect(jsonPath("$.totalAmount").value(309.5));
        getAs(store, "/orders/summary", "liveTitle", "otoño")
                .andExpect(jsonPath("$.orders").value(0))
                .andExpect(jsonPath("$.units").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
        // totalAmount sin ceros de sobra.
        String body = getAs(store, "/orders/summary", "status", "CANCELLED")
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).contains("\"totalAmount\":200}");

        // Los datos de otra tienda nunca aparecen.
        getAs(store, "/orders", "clientId", otherClient).andExpect(jsonPath("$", empty()));
        getAs(store, "/orders/summary", "clientId", otherClient)
                .andExpect(jsonPath("$.orders").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
        getAs(store, "/orders/summary", "liveTitle", "primavera", "from", "2026-10-10", "to", "2026-10-10")
                .andExpect(jsonPath("$.orders").value(2))
                .andExpect(jsonPath("$.totalAmount").value(1589.5));
        getAs(other, "/orders").andExpect(jsonPath("$[*].id", contains(otherOrder)));
        getAs(other, "/orders", "clientId", ana).andExpect(jsonPath("$", empty()));
        getAs(other, "/orders/summary")
                .andExpect(jsonPath("$.orders").value(1))
                .andExpect(jsonPath("$.units").value(3))
                .andExpect(jsonPath("$.totalAmount").value(2997));
    }

    @Test
    void invalidOrderFiltersAreRejected() throws Exception {
        TestStore store = createStore("Tienda filtros invalidos", true);

        for (String path : new String[]{"/orders", "/orders/summary"}) {
            getAs(store, path, "from", "2026-10-11", "to", "2026-10-10")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("La fecha desde no puede ser posterior a la fecha hasta"));
            getAs(store, path, "from", "2026-13-01")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro from no es valido"));
            getAs(store, path, "to", "10/10/2026")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro to no es valido"));
            getAs(store, path, "clientId", "no-es-uuid")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro clientId no es valido"));
            getAs(store, path, "status", "NOPE")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro status no es valido"));
            getAs(store, path, "packageStatus", "LOST")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El parametro packageStatus no es valido"));
            // Mismo dia en ambos extremos es valido.
            getAs(store, path, "from", "2026-10-10", "to", "2026-10-10").andExpect(status().isOk());
        }
    }

    @Test
    void summaryRequiresAdmin() throws Exception {
        mockMvc.perform(get("/orders/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/orders/summary").with(superAdmin())).andExpect(status().isForbidden());
    }
}
