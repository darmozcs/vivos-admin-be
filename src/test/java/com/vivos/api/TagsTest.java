package com.vivos.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etiquetas: CRUD y su uso para relacionar y filtrar vivos y movimientos. */
class TagsTest extends AbstractIntegrationTest {

    private String idOf(MockHttpServletRequestBuilder request, TestStore store, String body) throws Exception {
        String response = mockMvc.perform(request.with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String tag(TestStore store, String name) throws Exception {
        return idOf(post("/tags"), store, "{\"name\":\"" + name + "\",\"color\":\"#ff8800\"}");
    }

    private String sale(TestStore store, String clientId, String tagsJson) throws Exception {
        return idOf(post("/trades"), store, "{\"type\":\"SALE\",\"status\":\"SOLD\",\"quantity\":2,\"price\":1000,"
                + "\"clientId\":\"" + clientId + "\",\"tagIds\":" + tagsJson + "}");
    }

    @Test
    void tagsCrud() throws Exception {
        TestStore store = createStore("Tienda etiquetas", true);
        String id = tag(store, "  Invierno ");

        mockMvc.perform(get("/tags").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Invierno"))
                .andExpect(jsonPath("$[0].color").value("#FF8800"));
        mockMvc.perform(post("/tags").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"invierno\",\"color\":\"#000000\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una etiqueta con ese nombre"));
        mockMvc.perform(post("/tags").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Verano\",\"color\":\"rojo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El color no es valido"));
        mockMvc.perform(patch("/tags/{id}", id).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Invierno 2026\",\"color\":\"#123abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Invierno 2026"))
                .andExpect(jsonPath("$.color").value("#123ABC"));
        mockMvc.perform(delete("/tags/{id}", id).with(admin(store))).andExpect(status().isNoContent());
        mockMvc.perform(get("/tags").with(admin(store))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void tradesAndLivesAreTaggedAndFiltered() throws Exception {
        TestStore store = createStore("Tienda etiquetas uso", true);
        TestStore other = createStore("Otra tienda etiquetas", true);
        String invierno = tag(store, "Invierno");
        String feria = tag(store, "Feria");
        String ajena = tag(other, "Ajena");
        String clientId = idOf(post("/clients"), store, "{\"fullName\":\"Clienta\"}");

        String both = sale(store, clientId, "[\"" + invierno + "\",\"" + feria + "\"]");
        sale(store, clientId, "[\"" + feria + "\"]");
        sale(store, clientId, "[]");
        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SALE\",\"status\":\"SOLD\",\"quantity\":1,\"price\":1,"
                                + "\"clientId\":\"" + clientId + "\",\"tagIds\":[\"" + ajena + "\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Alguna etiqueta no existe"));

        // Filtro: tiene alguna de las etiquetas pedidas; los totales no se duplican.
        mockMvc.perform(get("/trades").param("tagIds", invierno).with(admin(store)))
                .andExpect(jsonPath("$[*].id", contains(both)))
                .andExpect(jsonPath("$[0].tagIds", containsInAnyOrder(invierno, feria)));
        mockMvc.perform(get("/trades").param("tagIds", invierno + "," + feria).with(admin(store)))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/trades/summary").param("tagIds", invierno + "," + feria).with(admin(store)))
                .andExpect(jsonPath("$.salesTotal").value(2000))
                .andExpect(jsonPath("$.salesCount").value(2));

        // Vivo: etiquetas al crearlo y despues.
        String liveId = idOf(post("/lives"), store, "{\"title\":\"Vivo\",\"tagIds\":[\"" + feria + "\"]}");
        mockMvc.perform(get("/lives/{id}", liveId).with(admin(store)))
                .andExpect(jsonPath("$.tagIds", contains(feria)));
        mockMvc.perform(put("/lives/{id}/tags", liveId).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[\"" + invierno + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagIds", contains(invierno)));
        mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store))).andExpect(status().isOk());
        mockMvc.perform(post("/lives/{id}/end", liveId).with(admin(store))).andExpect(status().isOk());

        // Balance: el vivo (Invierno) y la venta con Invierno; la venta solo Feria queda afuera.
        mockMvc.perform(get("/balance").param("tagIds", invierno).with(admin(store)))
                .andExpect(jsonPath("$.entries", hasSize(2)))
                .andExpect(jsonPath("$.entries[*].type", containsInAnyOrder("LIVE", "SALE")));

        // Borrar la etiqueta la quita de todo.
        mockMvc.perform(delete("/tags/{id}", invierno).with(admin(store))).andExpect(status().isNoContent());
        mockMvc.perform(get("/lives/{id}", liveId).with(admin(store)))
                .andExpect(jsonPath("$.tagIds", hasSize(0)));
    }
}
