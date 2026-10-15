package com.vivos.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Numero de paquete: se asigna con la primera compra de la clienta en el vivo, se pierde si la
 * venta se queda sin items, el contador del vivo nunca retrocede y se puede editar sin repetir.
 */
class PackageNumberTest extends AbstractIntegrationTest {

    private static final String ONE_ITEM = "{\"status\":\"PENDING\",\"items\":[{\"price\":100}]}";
    private static final String NO_ITEMS = "{\"status\":\"PENDING\",\"items\":[]}";

    private String idOf(MockHttpServletRequestBuilder request, TestStore store, String body) throws Exception {
        String response = mockMvc.perform(request.with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String startedLive(TestStore store) throws Exception {
        String liveId = idOf(post("/lives"), store, "{\"title\":\"Vivo paquetes\"}");
        mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store))).andExpect(status().isOk());
        return liveId;
    }

    private ResultActions saveOrder(TestStore store, String liveId, String clientId, String body) throws Exception {
        return mockMvc.perform(put("/lives/{l}/orders/{c}", liveId, clientId).with(admin(store))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions patchNumber(TestStore store, String orderId, int number) throws Exception {
        return mockMvc.perform(patch("/orders/{id}", orderId).with(admin(store))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"number\":" + number + "}"));
    }

    @Test
    void numberIsAssignedWithTheFirstPurchaseAndNeverGoesBack() throws Exception {
        TestStore store = createStore("Tienda paquetes", true);
        String ana = idOf(post("/clients"), store, "{\"fullName\":\"Ana\"}");
        String bea = idOf(post("/clients"), store, "{\"fullName\":\"Bea\"}");
        String caro = idOf(post("/clients"), store, "{\"fullName\":\"Caro\"}");
        String dani = idOf(post("/clients"), store, "{\"fullName\":\"Dani\"}");
        String liveId = startedLive(store);

        // Seleccionarla sin compras no le da numero.
        saveOrder(store, liveId, ana, NO_ITEMS).andExpect(jsonPath("$.number").value(nullValue()));
        saveOrder(store, liveId, ana, ONE_ITEM).andExpect(jsonPath("$.number").value(1));
        saveOrder(store, liveId, bea, ONE_ITEM).andExpect(jsonPath("$.number").value(2));
        saveOrder(store, liveId, caro, ONE_ITEM).andExpect(jsonPath("$.number").value(3));
        // Mas compras no cambian el numero.
        saveOrder(store, liveId, ana, ONE_ITEM).andExpect(jsonPath("$.number").value(1));

        // Caro se queda sin compras: pierde el 3 y el contador sigue en 4.
        saveOrder(store, liveId, caro, NO_ITEMS).andExpect(jsonPath("$.number").value(nullValue()));
        saveOrder(store, liveId, dani, ONE_ITEM).andExpect(jsonPath("$.number").value(4));
        // Si Caro vuelve a comprar recibe uno nuevo.
        saveOrder(store, liveId, caro, ONE_ITEM).andExpect(jsonPath("$.number").value(5));
    }

    @Test
    void numberCanBeEditedButNotRepeated() throws Exception {
        TestStore store = createStore("Tienda paquetes edit", true);
        String ana = idOf(post("/clients"), store, "{\"fullName\":\"Ana\"}");
        String bea = idOf(post("/clients"), store, "{\"fullName\":\"Bea\"}");
        String caro = idOf(post("/clients"), store, "{\"fullName\":\"Caro\"}");
        String dani = idOf(post("/clients"), store, "{\"fullName\":\"Dani\"}");
        String liveId = startedLive(store);

        String anaOrder = JsonPath.read(saveOrder(store, liveId, ana, ONE_ITEM)
                .andReturn().getResponse().getContentAsString(), "$.id");
        saveOrder(store, liveId, bea, ONE_ITEM).andExpect(jsonPath("$.number").value(2));

        // Ana pasa al 10: la siguiente clienta sigue despues de el.
        patchNumber(store, anaOrder, 10).andExpect(status().isOk()).andExpect(jsonPath("$.number").value(10));
        saveOrder(store, liveId, caro, ONE_ITEM).andExpect(jsonPath("$.number").value(11));

        // Repetido dentro del vivo: se rechaza.
        patchNumber(store, anaOrder, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese número de paquete ya está en uso en este vivo"));
        patchNumber(store, anaOrder, 0).andExpect(status().isBadRequest());

        // Un numero bajo y libre se puede usar, y el contador no lo vuelve a dar.
        String daniOrder = JsonPath.read(saveOrder(store, liveId, dani, NO_ITEMS)
                .andReturn().getResponse().getContentAsString(), "$.id");
        patchNumber(store, daniOrder, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La venta no tiene compras"));
        saveOrder(store, liveId, dani, ONE_ITEM).andExpect(jsonPath("$.number").value(12));
        patchNumber(store, daniOrder, 1).andExpect(status().isOk()).andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void autoNumberSkipsNumbersSetByHand() throws Exception {
        TestStore store = createStore("Tienda paquetes salto", true);
        String ana = idOf(post("/clients"), store, "{\"fullName\":\"Ana\"}");
        String bea = idOf(post("/clients"), store, "{\"fullName\":\"Bea\"}");
        String liveId = startedLive(store);

        // Ana toma el 1 y lo cambia al 2: el contador pasa a 3, asi que Bea recibe el 3.
        String anaOrder = JsonPath.read(saveOrder(store, liveId, ana, ONE_ITEM)
                .andReturn().getResponse().getContentAsString(), "$.id");
        patchNumber(store, anaOrder, 2).andExpect(status().isOk());
        saveOrder(store, liveId, bea, ONE_ITEM).andExpect(jsonPath("$.number").value(3));
    }
}
