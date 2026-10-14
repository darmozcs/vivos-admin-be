package com.vivos.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comportamiento portado de los handlers MSW de tienda-admin. */
class BusinessBehaviorTest extends AbstractIntegrationTest {

    private String idOf(MockHttpServletRequestBuilder request, TestStore store, String body) throws Exception {
        String response = mockMvc.perform(request.with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String startedLive(TestStore store, String title) throws Exception {
        String liveId = idOf(post("/lives"), store, "{\"title\":\"" + title + "\"}");
        mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store))).andExpect(status().isOk());
        return liveId;
    }

    @Test
    void settingsReadAndUpdateStoreName() throws Exception {
        TestStore store = createStore("Mi tienda", true);

        mockMvc.perform(get("/settings").with(admin(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Mi tienda"));
        mockMvc.perform(put("/settings").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"  Nuevo nombre  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Nuevo nombre"));
        mockMvc.perform(put("/settings").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El nombre de la tienda es obligatorio"));
    }

    @Test
    void clientsCrudSearchAndNormalization() throws Exception {
        TestStore store = createStore("Tienda clientas", true);

        mockMvc.perform(post("/clients").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"  Lucía Fernández \",\"account\":\"luciaf\",\"phone\":\" 099 123 456 \",\"email\":\" lucia@mail.com \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Lucía Fernández"))
                .andExpect(jsonPath("$.account").value("@luciaf"))
                .andExpect(jsonPath("$.phone").value("099 123 456"))
                .andExpect(jsonPath("$.email").value("lucia@mail.com"))
                .andExpect(jsonPath("$.address").value(""))
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
        String otherId = idOf(post("/clients"), store, "{\"fullName\":\"Martina Pereira\",\"account\":\"@marpereira\"}");

        mockMvc.perform(post("/clients").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El nombre es obligatorio"));
        mockMvc.perform(post("/clients").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Sin arroba\",\"email\":\"lucia.mail.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El correo no es valido"));
        mockMvc.perform(get("/clients").param("email", "LUCIA@").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/clients").param("search", "MAIL.COM").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/clients").param("search", "LUCÍA").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].account").value("@luciaf"));
        mockMvc.perform(get("/clients").param("search", "123 456").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/clients").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(patch("/clients/{id}", otherId).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Martina P.\",\"account\":\"\",\"phone\":\"098\",\"address\":\"Bvar. Artigas 860\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Martina P."))
                .andExpect(jsonPath("$.account").value(""))
                .andExpect(jsonPath("$.address").value("Bvar. Artigas 860"))
                .andExpect(jsonPath("$.email").value(""));

        mockMvc.perform(delete("/clients/{id}", otherId).with(admin(store)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/clients/{id}", otherId).with(admin(store)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Clienta no encontrada"));
    }

    @Test
    void clientDepartmentAndDeliveryTypeAreOptionalAndValidated() throws Exception {
        TestStore store = createStore("Tienda entregas", true);

        mockMvc.perform(post("/clients").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Sin entrega\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.department").doesNotExist())
                .andExpect(jsonPath("$.deliveryType").doesNotExist());

        String id = idOf(post("/clients"), store,
                "{\"fullName\":\"Con entrega\",\"department\":\"TREINTA_Y_TRES\",\"deliveryType\":\"AGENCIA\"}");
        mockMvc.perform(get("/clients").param("search", "Con entrega").with(admin(store)))
                .andExpect(jsonPath("$[0].department").value("TREINTA_Y_TRES"))
                .andExpect(jsonPath("$[0].deliveryType").value("AGENCIA"));

        // El PATCH reemplaza todo: sin los campos, quedan vacios.
        mockMvc.perform(patch("/clients/{id}", id).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Con entrega\",\"department\":\"MONTEVIDEO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("MONTEVIDEO"))
                .andExpect(jsonPath("$.deliveryType").doesNotExist());

        mockMvc.perform(post("/clients").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Invalida\",\"department\":\"BUENOS_AIRES\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/clients").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Invalida\",\"deliveryType\":\"DRON\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientWithPurchasesCannotBeDeletedButWithEmptyOrderCan() throws Exception {
        TestStore store = createStore("Tienda compras", true);
        String buyer = idOf(post("/clients"), store, "{\"fullName\":\"Compradora\"}");
        String emptyBuyer = idOf(post("/clients"), store, "{\"fullName\":\"Sin compras\"}");
        String liveId = startedLive(store, "Vivo");

        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, buyer).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[{\"code\":\"1\",\"name\":\"A\",\"color\":\"\",\"price\":10,\"quantity\":1}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, emptyBuyer).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/clients/{id}", buyer).with(admin(store)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede eliminar una clienta con compras"));
        mockMvc.perform(delete("/clients/{id}", emptyBuyer).with(admin(store)))
                .andExpect(status().isNoContent());
    }

    @Test
    void orderItemOnlyRequiresPrice() throws Exception {
        TestStore store = createStore("Tienda solo precio", true);
        String buyer = idOf(post("/clients"), store, "{\"fullName\":\"Compradora\"}");
        String liveId = startedLive(store, "Vivo");

        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, buyer).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[{\"price\":450}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value(""))
                .andExpect(jsonPath("$.items[0].name").value(""))
                .andExpect(jsonPath("$.items[0].color").value(""))
                .andExpect(jsonPath("$.items[0].price").value(450))
                .andExpect(jsonPath("$.items[0].quantity").value(1));

        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, buyer).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[{\"code\":\"7\",\"name\":\"Remera\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El precio es obligatorio"));
    }

    @Test
    void liveLifecycleTransitions() throws Exception {
        TestStore store = createStore("Tienda vivos", true);

        mockMvc.perform(get("/lives/current").with(admin(store)))
                .andExpect(status().isNotFound());

        String liveId = idOf(post("/lives"), store, "{\"title\":\"  Drop de primavera \"}");
        mockMvc.perform(get("/lives/current").with(admin(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(liveId))
                .andExpect(jsonPath("$.title").value("Drop de primavera"))
                .andExpect(jsonPath("$.platform").value("Instagram Live"))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.startedAt").isEmpty())
                .andExpect(jsonPath("$.accumulatedMs").value(0))
                .andExpect(jsonPath("$.runningSince").isEmpty());

        mockMvc.perform(post("/lives/{id}/pause", liveId).with(admin(store)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La acción no está permitida en este estado"));
        mockMvc.perform(post("/lives/{id}/volar", liveId).with(admin(store)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.runningSince").isNotEmpty());
        mockMvc.perform(post("/lives/{id}/pause", liveId).with(admin(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.runningSince").isEmpty());
        mockMvc.perform(post("/lives/{id}/resume", liveId).with(admin(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
        mockMvc.perform(post("/lives/{id}/end", liveId).with(admin(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.endedAt").isNotEmpty())
                .andExpect(jsonPath("$.runningSince").isEmpty());
        mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store)))
                .andExpect(status().isConflict());

        String newer = idOf(post("/lives"), store, "{\"title\":\"Otro\"}");
        mockMvc.perform(get("/lives").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(newer))
                .andExpect(jsonPath("$[1].id").value(liveId));
    }

    @Test
    void ordersNumberingTotalsAndStatus() throws Exception {
        TestStore store = createStore("Tienda ventas", true);
        TestStore otherStore = createStore("Otra tienda ventas", true);
        String c1 = idOf(post("/clients"), store, "{\"fullName\":\"Uno\"}");
        String c2 = idOf(post("/clients"), store, "{\"fullName\":\"Dos\"}");
        String c3 = idOf(post("/clients"), store, "{\"fullName\":\"Tres\"}");

        String notStarted = idOf(post("/lives"), store, "{\"title\":\"Sin empezar\"}");
        mockMvc.perform(put("/lives/{l}/orders/{c}", notStarted, c1).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El vivo todavía no comenzó"));

        String liveId = startedLive(store, "Vivo ventas");

        String order1 = JsonPath.read(mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, c1).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PAID","items":[
                                  {"code":"045","name":"Short de jean","color":"Celeste","price":1290,"quantity":1},
                                  {"code":"052","name":"Musculosa rib","color":"Blanco","price":690,"quantity":2}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.liveNumber").value(2))
                .andExpect(jsonPath("$.liveId").value(liveId))
                .andExpect(jsonPath("$.clientId").value(c1))
                .andExpect(jsonPath("$.packageStatus").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].price").value(1290))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, c2).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"packageStatus\":\"PREPARED\",\"items\":[{\"code\":\"1\",\"name\":\"A\",\"price\":99.5,\"quantity\":1}]}"))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.packageStatus").value("PREPARED"))
                .andExpect(jsonPath("$.items[0].color").value(""));

        String cancelled = JsonPath.read(mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, c3).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[{\"code\":\"9\",\"name\":\"Z\",\"price\":1000,\"quantity\":3}]}"))
                .andExpect(jsonPath("$.number").value(3))
                .andReturn().getResponse().getContentAsString(), "$.id");

        // Upsert: el mismo vivo + clienta conserva id y numero.
        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, c1).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PAID","items":[
                                  {"code":"045","name":"Short de jean","color":"Celeste","price":1290,"quantity":1},
                                  {"code":"052","name":"Musculosa rib","color":"Blanco","price":690,"quantity":2}]}
                                """))
                .andExpect(jsonPath("$.id").value(order1))
                .andExpect(jsonPath("$.number").value(1));

        mockMvc.perform(patch("/orders/{id}", cancelled).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.packageStatus").value("PENDING"));
        mockMvc.perform(patch("/orders/{id}", order1).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageStatus\":\"SHIPPED\"}"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.packageStatus").value("SHIPPED"));

        // Totales como toSummary: sin canceladas. buyers=2, units=1+2+1=4, total=1290+1380+99.5
        mockMvc.perform(get("/lives/{id}", liveId).with(admin(store)))
                .andExpect(jsonPath("$.buyers").value(2))
                .andExpect(jsonPath("$.units").value(4))
                .andExpect(jsonPath("$.totalAmount").value(2769.5));
        mockMvc.perform(get("/lives").with(admin(store)))
                .andExpect(jsonPath("$[0].id").value(liveId))
                .andExpect(jsonPath("$[0].buyers").value(2))
                .andExpect(jsonPath("$[1].buyers").value(0))
                .andExpect(jsonPath("$[1].totalAmount").value(0));

        // GET /orders: con items, numero descendente.
        mockMvc.perform(get("/orders").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].number").value(3))
                .andExpect(jsonPath("$[2].number").value(1));

        // Venta vaciada: sigue en el vivo pero no en GET /orders.
        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, c2).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[]}"))
                .andExpect(jsonPath("$.packageStatus").value("PREPARED"))
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(get("/orders").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/lives/{id}/orders", liveId).with(admin(store)))
                .andExpect(jsonPath("$", hasSize(3)));

        // Los vivos se numeran por tienda y las ventas dentro de su vivo: el vivo 3 arranca en 1.
        mockMvc.perform(get("/lives/{id}", notStarted).with(admin(store)))
                .andExpect(jsonPath("$.number").value(1));
        String secondLive = startedLive(store, "Otro vivo ventas");
        mockMvc.perform(get("/lives/{id}", secondLive).with(admin(store)))
                .andExpect(jsonPath("$.number").value(3));
        mockMvc.perform(put("/lives/{l}/orders/{c}", secondLive, c1).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[]}"))
                .andExpect(jsonPath("$.liveNumber").value(3))
                .andExpect(jsonPath("$.number").value(1));

        // La numeracion es por tienda: la otra tienda arranca en 1.
        String otherClient = idOf(post("/clients"), otherStore, "{\"fullName\":\"Otra\"}");
        String otherLive = startedLive(otherStore, "Vivo otra");
        mockMvc.perform(put("/lives/{l}/orders/{c}", otherLive, otherClient).with(admin(otherStore))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[]}"))
                .andExpect(jsonPath("$.number").value(1));

        mockMvc.perform(patch("/orders/{id}", java.util.UUID.randomUUID()).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Venta no encontrada"));
    }

    @Test
    void orderValidation() throws Exception {
        TestStore store = createStore("Tienda validacion", true);
        String client = idOf(post("/clients"), store, "{\"fullName\":\"Una\"}");
        String liveId = startedLive(store, "Vivo");

        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, client).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El estado de la venta es obligatorio"));
        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, client).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"items\":[{\"code\":\"1\",\"name\":\"A\",\"price\":10,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, client).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOPE\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
