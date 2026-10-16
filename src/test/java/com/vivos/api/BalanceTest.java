package com.vivos.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Balance: vivos finalizados, ventas vendidas y compras compradas en una sola lista. */
class BalanceTest extends AbstractIntegrationTest {

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

    /**
     * Vivo con una venta pagada de {@code quantity} prendas a {@code price} y, si viene
     * {@code pendingClientId}, otra pendiente que no cuenta en el total recaudado.
     */
    private void liveWithSale(TestStore store, String clientId, int quantity, int price, boolean end,
                              String pendingClientId) throws Exception {
        String liveId = idOf(post("/lives"), store, "{\"title\":\"Vivo\"}");
        mockMvc.perform(post("/lives/{id}/start", liveId).with(admin(store))).andExpect(status().isOk());
        sale(store, liveId, clientId, "PAID", quantity, price);
        if (pendingClientId != null) {
            sale(store, liveId, pendingClientId, "PENDING", 7, 7777);
        }
        if (end) {
            mockMvc.perform(post("/lives/{id}/end", liveId).with(admin(store))).andExpect(status().isOk());
        }
    }

    private void trade(TestStore store, String type, String status, int quantity, int price, String counterpart)
            throws Exception {
        String counterpartField = "SALE".equals(type) ? "clientId" : "sellerId";
        idOf(post("/trades"), store, "{\"type\":\"" + type + "\",\"status\":\"" + status + "\",\"quantity\":" + quantity
                + ",\"price\":" + price + ",\"" + counterpartField + "\":\"" + counterpart + "\"}");
    }

    @Test
    void paidPaymentsSubtractLikePurchases() throws Exception {
        TestStore store = createStore("Tienda balance pagos", true);
        String clientId = idOf(post("/clients"), store, "{\"fullName\":\"Clienta\"}");
        String sellerId = idOf(post("/sellers"), store, "{\"fullName\":\"Vendedor\"}");
        trade(store, "SALE", "SOLD", 10, 5000, clientId);
        trade(store, "PURCHASE", "PURCHASED", 4, 1000, sellerId);
        idOf(post("/trades"), store,
                "{\"type\":\"PAYMENT\",\"status\":\"PAID\",\"price\":1500,\"sellerId\":\"" + sellerId + "\"}");
        idOf(post("/trades"), store,
                "{\"type\":\"PAYMENT\",\"status\":\"PENDING\",\"price\":9999,\"sellerId\":\"" + sellerId + "\"}");

        // El pago pendiente no cuenta; el pagado resta junto con la compra y no suma prendas.
        mockMvc.perform(get("/balance").with(admin(store)))
                .andExpect(jsonPath("$.entries", hasSize(3)))
                .andExpect(jsonPath("$.entries[0].type").value("PAYMENT"))
                .andExpect(jsonPath("$.entries[0].quantity").value(0))
                .andExpect(jsonPath("$.summary.purchasesTotal").value(2500))
                .andExpect(jsonPath("$.summary.balance").value(2500))
                .andExpect(jsonPath("$.summary.boughtQuantity").value(4))
                .andExpect(jsonPath("$.summary.netQuantity").value(-6));
        mockMvc.perform(get("/balance").param("type", "PAYMENT").with(admin(store)))
                .andExpect(jsonPath("$.entries", hasSize(1)))
                .andExpect(jsonPath("$.summary.balance").value(-1500));
    }

    @Test
    void balanceJoinsEndedLivesSoldSalesAndPurchasedPurchases() throws Exception {
        TestStore store = createStore("Tienda balance", true);
        String clientId = idOf(post("/clients"), store, "{\"fullName\":\"Clienta\"}");
        String sellerId = idOf(post("/sellers"), store, "{\"fullName\":\"Vendedor\"}");

        String pendingClient = idOf(post("/clients"), store, "{\"fullName\":\"Debe\"}");
        // Del vivo solo cuenta lo pagado: la venta pendiente (7 prendas, $54439) queda afuera.
        liveWithSale(store, clientId, 3, 500, true, pendingClient);
        liveWithSale(store, clientId, 9, 9999, false, null);
        trade(store, "SALE", "SOLD", 10, 2000, clientId);
        trade(store, "SALE", "PUBLISHED", 99, 9999, clientId);
        trade(store, "PURCHASE", "PURCHASED", 40, 4000, sellerId);
        trade(store, "PURCHASE", "PENDING", 99, 9999, sellerId);

        mockMvc.perform(get("/balance").with(admin(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries", hasSize(3)))
                .andExpect(jsonPath("$.entries[0].type").value("PURCHASE"))
                .andExpect(jsonPath("$.entries[2].type").value("LIVE"))
                .andExpect(jsonPath("$.entries[2].quantity").value(3))
                .andExpect(jsonPath("$.entries[2].amount").value(1500))
                .andExpect(jsonPath("$.summary.salesTotal").value(3500))
                .andExpect(jsonPath("$.summary.purchasesTotal").value(4000))
                .andExpect(jsonPath("$.summary.balance").value(-500))
                .andExpect(jsonPath("$.summary.soldQuantity").value(13))
                .andExpect(jsonPath("$.summary.boughtQuantity").value(40))
                .andExpect(jsonPath("$.summary.netQuantity").value(27));

        mockMvc.perform(get("/balance").param("type", "LIVE").with(admin(store)))
                .andExpect(jsonPath("$.entries", hasSize(1)))
                .andExpect(jsonPath("$.summary.salesTotal").value(1500))
                .andExpect(jsonPath("$.summary.netQuantity").value(-3));

        String today = LocalDate.now(ZoneId.of("America/Montevideo")).toString();
        String tomorrow = LocalDate.now(ZoneId.of("America/Montevideo")).plusDays(1).toString();
        mockMvc.perform(get("/balance").param("from", today).param("to", today).with(admin(store)))
                .andExpect(jsonPath("$.entries", hasSize(3)));
        mockMvc.perform(get("/balance").param("from", tomorrow).with(admin(store)))
                .andExpect(jsonPath("$.entries", hasSize(0)))
                .andExpect(jsonPath("$.summary.balance").value(0));
        mockMvc.perform(get("/balance").param("from", tomorrow).param("to", today).with(admin(store)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void balanceIsIsolatedByStore() throws Exception {
        TestStore store = createStore("Tienda A balance", true);
        TestStore other = createStore("Tienda B balance", true);
        String otherSeller = idOf(post("/sellers"), other, "{\"fullName\":\"Ajeno\"}");
        trade(other, "PURCHASE", "PURCHASED", 5, 100, otherSeller);

        mockMvc.perform(get("/balance").with(admin(store)))
                .andExpect(jsonPath("$.entries", hasSize(0)))
                .andExpect(jsonPath("$.summary.purchasesTotal").value(0));
    }
}
