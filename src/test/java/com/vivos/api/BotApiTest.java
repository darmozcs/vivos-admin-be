package com.vivos.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API del bot de WhatsApp: API key, tienda por WABA ID y clienta por telefono. */
@TestPropertySource(properties = "vivos.bot.api-key=" + BotApiTest.API_KEY)
class BotApiTest extends AbstractIntegrationTest {

    static final String API_KEY = "clave-del-bot";
    private static final String PHONE = "59899123456";

    private String waba() {
        return String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()) % 1_000_000_000_000L);
    }

    /** Tienda con su cuenta de WhatsApp asociada desde Configuracion. */
    private TestStore storeWithWaba(String name, String wabaId) throws Exception {
        TestStore store = createStore(name, true);
        mockMvc.perform(put("/settings").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"" + name + "\",\"whatsappWabaId\":\"" + wabaId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappWabaId").value(wabaId));
        return store;
    }

    private MockHttpServletRequestBuilder bot(MockHttpServletRequestBuilder request, String wabaId, String from) {
        return request.header("X-Api-Key", API_KEY)
                .header("X-Whatsapp-Waba-Id", wabaId)
                .header("X-Whatsapp-From", from);
    }

    private String idOf(MockHttpServletRequestBuilder request, TestStore store, String body) throws Exception {
        String response = mockMvc.perform(request.with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void sale(TestStore store, String liveId, String clientId, String status, int quantity, int price)
            throws Exception {
        mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, clientId).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\",\"items\":[{\"price\":" + price
                                + ",\"quantity\":" + quantity + "}]}"))
                .andExpect(status().isOk());
    }

    private String startedLive(TestStore store, String title) throws Exception {
        String liveId = idOf(post("/lives"), store, "{\"title\":\"" + title + "\"}");
        mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store))).andExpect(status().isOk());
        return liveId;
    }

    @Test
    void requiresTheApiKeyAndDoesNotAcceptAdminTokens() throws Exception {
        String wabaId = waba();
        TestStore store = storeWithWaba("Tienda key", wabaId);

        mockMvc.perform(get("/bot/me").header("X-Whatsapp-Waba-Id", wabaId).header("X-Whatsapp-From", PHONE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/bot/me").header("X-Api-Key", "otra")
                        .header("X-Whatsapp-Waba-Id", wabaId).header("X-Whatsapp-From", PHONE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/bot/me").with(admin(store))
                        .header("X-Whatsapp-Waba-Id", wabaId).header("X-Whatsapp-From", PHONE))
                .andExpect(status().isUnauthorized());
        // La API key no abre el resto de la API.
        mockMvc.perform(get("/clients").header("X-Api-Key", API_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void identifiesTheStoreByWabaIdAndTheClientByPhone() throws Exception {
        String wabaId = waba();
        TestStore store = storeWithWaba("Tienda A", wabaId);
        TestStore other = storeWithWaba("Tienda B", waba());
        idOf(post("/clients"), store, "{\"fullName\":\"Lucia Gomez\",\"phone\":\"099 123 456\"}");
        idOf(post("/clients"), other, "{\"fullName\":\"Otra\",\"phone\":\"098 555 000\"}");

        mockMvc.perform(bot(get("/bot/me"), wabaId, PHONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Lucia Gomez"))
                .andExpect(jsonPath("$.storeName").value("Tienda A"));
        // Clienta de otra tienda: no la encuentra en esta.
        mockMvc.perform(bot(get("/bot/me"), wabaId, "59898555000"))
                .andExpect(status().isNotFound());
        // WABA sin tienda.
        mockMvc.perform(bot(get("/bot/me"), "1", PHONE))
                .andExpect(status().isNotFound());
        mockMvc.perform(bot(get("/bot/me"), wabaId, "hola"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveStoreIsForbidden() throws Exception {
        String wabaId = waba();
        TestStore store = storeWithWaba("Tienda inactiva", wabaId);
        storeRepository.findById(store.storeId()).ifPresent(s -> {
            s.setActive(false);
            storeRepository.save(s);
        });

        mockMvc.perform(bot(get("/bot/me"), wabaId, PHONE))
                .andExpect(status().isForbidden());
    }

    @Test
    void wabaIdBelongsToOneStoreAndCanBeCleared() throws Exception {
        String wabaId = waba();
        storeWithWaba("Duena", wabaId);
        TestStore other = createStore("Otra", true);

        mockMvc.perform(put("/settings").with(admin(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"Otra\",\"whatsappWabaId\":\"" + wabaId + "\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/settings").with(admin(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"Otra\",\"whatsappWabaId\":\"abc\"}"))
                .andExpect(status().isBadRequest());

        TestStore store = storeWithWaba("Con cuenta", waba());
        mockMvc.perform(put("/settings").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"Con cuenta\",\"whatsappWabaId\":\"  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappWabaId").isEmpty());
    }

    @Test
    void debtIsTheOrderOfTheLastLive() throws Exception {
        String wabaId = waba();
        TestStore store = storeWithWaba("Tienda deuda", wabaId);
        String lucia = idOf(post("/clients"), store, "{\"fullName\":\"Lucia\",\"phone\":\"+598 99 123 456\"}");
        String ana = idOf(post("/clients"), store, "{\"fullName\":\"Ana\",\"phone\":\"098 000 111\"}");

        mockMvc.perform(bot(get("/bot/debt"), wabaId, PHONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPurchase").value(false));

        String first = startedLive(store, "Primer vivo");
        sale(store, first, lucia, "PENDING", 1, 500);
        mockMvc.perform(post("/lives/{id}/end", first).with(admin(store))).andExpect(status().isOk());
        String second = startedLive(store, "Liquidacion");
        sale(store, second, lucia, "PENDING", 3, 400);
        sale(store, second, ana, "PENDING", 9, 999);

        mockMvc.perform(bot(get("/bot/debt"), wabaId, PHONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liveTitle").value("Liquidacion"))
                .andExpect(jsonPath("$.total").value(1200))
                .andExpect(jsonPath("$.owed").value(1200))
                .andExpect(jsonPath("$.items").value(3))
                .andExpect(jsonPath("$.paid").value(false))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("debés $ 1.200 por 3 prendas")));

        sale(store, second, lucia, "PAID", 3, 400);
        mockMvc.perform(bot(get("/bot/debt"), wabaId, PHONE))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.owed").value(0));

        // El cancelado no cuenta: vuelve al vivo anterior.
        sale(store, second, lucia, "CANCELLED", 3, 400);
        mockMvc.perform(bot(get("/bot/debt"), wabaId, PHONE))
                .andExpect(jsonPath("$.liveTitle").value("Primer vivo"))
                .andExpect(jsonPath("$.owed").value(500));

        mockMvc.perform(bot(get("/bot/debt"), wabaId, "59891000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientCanRegisterOnceInTheStoreOfTheBot() throws Exception {
        String wabaId = waba();
        String otherWaba = waba();
        TestStore store = storeWithWaba("Tienda registro", wabaId);
        storeWithWaba("Tienda otra", otherWaba);

        mockMvc.perform(bot(post("/bot/register"), wabaId, PHONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\" Lucia Gomez \",\"account\":\"luciag\",\"department\":\"paysandú\","
                                + "\"deliveryType\":\"Cadetería\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Lucia Gomez"));

        mockMvc.perform(get("/clients").with(admin(store)))
                .andExpect(jsonPath("$[0].phone").value("099 123 456"))
                .andExpect(jsonPath("$[0].account").value("@luciag"))
                .andExpect(jsonPath("$[0].department").value("PAYSANDU"))
                .andExpect(jsonPath("$[0].deliveryType").value("CADETERIA"));

        mockMvc.perform(bot(post("/bot/register"), wabaId, PHONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Lucia\"}"))
                .andExpect(status().isConflict());

        // En otra tienda todavia no es clienta.
        mockMvc.perform(bot(get("/bot/me"), otherWaba, PHONE))
                .andExpect(status().isNotFound());
        mockMvc.perform(bot(post("/bot/register"), otherWaba, PHONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Lucia\",\"department\":\"Narnia\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(bot(post("/bot/register"), wabaId, "59891222333")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
