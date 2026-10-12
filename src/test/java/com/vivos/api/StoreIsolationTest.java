package com.vivos.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La tienda A no puede ver ni modificar datos de la tienda B. */
class StoreIsolationTest extends AbstractIntegrationTest {

    private static final String ORDER_BODY = """
            {"status":"PENDING","items":[{"code":"001","name":"Remera","color":"Negro","price":100,"quantity":1}]}
            """;

    private String create(MockHttpServletRequestBuilder request, TestStore store, String body) throws Exception {
        String response = mockMvc.perform(request.with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void storeCannotReadOrModifyAnotherStoresData() throws Exception {
        TestStore storeA = createStore("Tienda A", true);
        TestStore storeB = createStore("Tienda B", true);

        String clientB = create(post("/clients"), storeB, "{\"fullName\":\"Clienta de B\"}");
        String clientA = create(post("/clients"), storeA, "{\"fullName\":\"Clienta de A\"}");
        String liveB = create(post("/lives"), storeB, "{\"title\":\"Vivo de B\"}");
        mockMvc.perform(post("/lives/{id}/start", liveB).with(admin(storeB))).andExpect(status().isOk());
        String orderB = create(put("/lives/{liveId}/orders/{clientId}", liveB, clientB), storeB, ORDER_BODY);

        // Lecturas
        mockMvc.perform(get("/clients").with(admin(storeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(clientB))))
                .andExpect(jsonPath("$[*].id", hasItem(clientA)));
        mockMvc.perform(get("/orders").with(admin(storeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(orderB))));
        mockMvc.perform(get("/lives").with(admin(storeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(liveB))));
        mockMvc.perform(get("/lives/{id}", liveB).with(admin(storeA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/lives/{id}/orders", liveB).with(admin(storeA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/settings").with(admin(storeA)))
                .andExpect(jsonPath("$.storeName").value("Tienda A"));

        // Escrituras
        mockMvc.perform(patch("/clients/{id}", clientB).with(admin(storeA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Hackeada\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/clients/{id}", clientB).with(admin(storeA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/orders/{id}", orderB).with(admin(storeA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/lives/{id}/end", liveB).with(admin(storeA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/lives/{liveId}/orders/{clientId}", liveB, clientA).with(admin(storeA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isNotFound());

        // Tienda A con su propio vivo no puede vender a una clienta de B.
        String liveA = create(post("/lives"), storeA, "{\"title\":\"Vivo de A\"}");
        mockMvc.perform(post("/lives/{id}/start", liveA).with(admin(storeA))).andExpect(status().isOk());
        mockMvc.perform(put("/lives/{liveId}/orders/{clientId}", liveA, clientB).with(admin(storeA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Clienta no encontrada"));

        // Los datos de B siguen intactos.
        mockMvc.perform(get("/clients").with(admin(storeB)))
                .andExpect(jsonPath("$[0].fullName").value("Clienta de B"));
        mockMvc.perform(get("/orders").with(admin(storeB)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
        mockMvc.perform(get("/lives/{id}", liveB).with(admin(storeB)))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }
}
