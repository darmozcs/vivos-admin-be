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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Vendedores y movimientos del negocio (ventas, compras y pagos). */
class TradesAndSellersTest extends AbstractIntegrationTest {

    private String idOf(MockHttpServletRequestBuilder request, TestStore store, String body) throws Exception {
        String response = mockMvc.perform(request.with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    /** Venta a una clienta. */
    private String sale(String clientId, String status, int quantity, String price) {
        return "{\"type\":\"SALE\",\"status\":\"" + status + "\",\"quantity\":" + quantity
                + ",\"price\":" + price + ",\"clientId\":\"" + clientId + "\"}";
    }

    /** Compra a un vendedor. */
    private String purchase(String sellerId, String status, int quantity, String price) {
        return "{\"type\":\"PURCHASE\",\"status\":\"" + status + "\",\"quantity\":" + quantity
                + ",\"price\":" + price + ",\"sellerId\":\"" + sellerId + "\"}";
    }

    @Test
    void sellersCrudAndNormalization() throws Exception {
        TestStore store = createStore("Tienda vendedores", true);

        String id = idOf(post("/sellers"), store,
                "{\"fullName\":\"  Ana Ropa \",\"account\":\"anaropa\",\"department\":\"SALTO\",\"deliveryType\":\"AGENCIA\"}");
        mockMvc.perform(get("/sellers").param("search", "ANA").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Ana Ropa"))
                .andExpect(jsonPath("$[0].account").value("@anaropa"))
                .andExpect(jsonPath("$[0].department").value("SALTO"));
        mockMvc.perform(post("/sellers").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"X\",\"email\":\"mal\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El correo no es valido"));
        mockMvc.perform(patch("/sellers/{id}", id).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ana L.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana L."))
                .andExpect(jsonPath("$.account").value(""));
        mockMvc.perform(delete("/sellers/{id}", id).with(admin(store)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/sellers/{id}", id).with(admin(store)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Vendedor no encontrado"));
    }

    @Test
    void tradesCrudSummaryAndCounterparts() throws Exception {
        TestStore store = createStore("Tienda compras ventas", true);
        String sellerId = idOf(post("/sellers"), store, "{\"fullName\":\"Vendedor\"}");
        String clientId = idOf(post("/clients"), store, "{\"fullName\":\"Clienta\"}");

        String saleId = idOf(post("/trades"), store, sale(clientId, "PUBLISHED", 120, "10000.50"));
        String purchaseId = idOf(post("/trades"), store, purchase(sellerId, "PENDING", 80, "4000"));

        mockMvc.perform(get("/trades").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].type").value("SALE"))
                .andExpect(jsonPath("$[1].clientName").value("Clienta"))
                .andExpect(jsonPath("$[1].sellerId").doesNotExist())
                .andExpect(jsonPath("$[1].price").value(10000.5))
                .andExpect(jsonPath("$[0].sellerName").value("Vendedor"));
        mockMvc.perform(get("/trades/summary").with(admin(store)))
                .andExpect(jsonPath("$.salesTotal").value(10000.5))
                .andExpect(jsonPath("$.purchasesTotal").value(4000))
                .andExpect(jsonPath("$.balance").value(6000.5))
                .andExpect(jsonPath("$.salesCount").value(1))
                .andExpect(jsonPath("$.purchasesCount").value(1))
                .andExpect(jsonPath("$.salesQuantity").value(120))
                .andExpect(jsonPath("$.purchasesQuantity").value(80));
        mockMvc.perform(get("/trades/summary").param("type", "PURCHASE").with(admin(store)))
                .andExpect(jsonPath("$.salesTotal").value(0))
                .andExpect(jsonPath("$.balance").value(-4000));

        // Fechas: dias completos (inclusive) de createdAt en la zona de la tienda.
        String today = java.time.LocalDate.now(java.time.ZoneId.of("America/Montevideo")).toString();
        String tomorrow = java.time.LocalDate.parse(today).plusDays(1).toString();
        mockMvc.perform(get("/trades").param("from", today).param("to", today).with(admin(store)))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/trades").param("from", tomorrow).with(admin(store)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/trades/summary").param("from", tomorrow).with(admin(store)))
                .andExpect(jsonPath("$.balance").value(0));
        mockMvc.perform(get("/trades").param("from", tomorrow).param("to", today).with(admin(store)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La fecha desde no puede ser posterior a la fecha hasta"));

        mockMvc.perform(patch("/trades/{id}", saleId).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sale(clientId, "SOLD", 100, "9000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.quantity").value(100));
        mockMvc.perform(get("/trades").param("status", "SOLD").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/trades/{id}", saleId).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchase(sellerId, "PENDING", 1, "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No se puede cambiar el tipo de un movimiento"));
        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sale(clientId, "PENDING", 1, "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El estado no corresponde al tipo de movimiento"));
        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SALE\",\"status\":\"SOLD\",\"quantity\":1,\"price\":1,\"sellerId\":\"" + sellerId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La venta necesita una clienta"));
        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PURCHASE\",\"status\":\"PENDING\",\"quantity\":1,\"price\":1,\"clientId\":\"" + clientId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La compra necesita un vendedor"));
        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sale(clientId, "SOLD", 0, "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La cantidad debe ser al menos 1"));

        mockMvc.perform(delete("/sellers/{id}", sellerId).with(admin(store)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede eliminar un vendedor con movimientos"));
        mockMvc.perform(delete("/clients/{id}", clientId).with(admin(store)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede eliminar una clienta con movimientos"));

        mockMvc.perform(delete("/trades/{id}", purchaseId).with(admin(store)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/sellers/{id}", sellerId).with(admin(store)))
                .andExpect(status().isNoContent());
    }

    @Test
    void tradesAreIsolatedByStore() throws Exception {
        TestStore store = createStore("Tienda A", true);
        TestStore other = createStore("Tienda B", true);
        String otherSellerId = idOf(post("/sellers"), other, "{\"fullName\":\"Ajeno\"}");
        String otherTradeId = idOf(post("/trades"), other, purchase(otherSellerId, "PURCHASED", 1, "100"));

        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchase(otherSellerId, "PURCHASED", 1, "100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Vendedor no encontrado"));
        mockMvc.perform(get("/trades").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/trades/summary").with(admin(store)))
                .andExpect(jsonPath("$.balance").value(0));
        mockMvc.perform(delete("/trades/{id}", otherTradeId).with(admin(store)))
                .andExpect(status().isNotFound());
    }

    private String payment(String sellerId, String status, String price) {
        return "{\"type\":\"PAYMENT\",\"status\":\"" + status + "\",\"price\":" + price
                + ",\"sellerId\":\"" + sellerId + "\"}";
    }

    @Test
    void paymentsHaveNoQuantityAndSubtractWithPurchases() throws Exception {
        TestStore store = createStore("Tienda pagos", true);
        String sellerId = idOf(post("/sellers"), store, "{\"fullName\":\"Vendedor\"}");
        String clientId = idOf(post("/clients"), store, "{\"fullName\":\"Clienta\"}");

        idOf(post("/trades"), store, sale(clientId, "SOLD", 10, "5000"));
        idOf(post("/trades"), store, purchase(sellerId, "PURCHASED", 4, "1000"));
        // La cantidad de un pago se ignora.
        String paymentId = idOf(post("/trades"), store,
                "{\"type\":\"PAYMENT\",\"status\":\"PENDING\",\"quantity\":9,\"price\":1500,\"sellerId\":\"" + sellerId + "\"}");

        mockMvc.perform(get("/trades").param("type", "PAYMENT").with(admin(store)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quantity").doesNotExist())
                .andExpect(jsonPath("$[0].sellerName").value("Vendedor"));
        mockMvc.perform(get("/trades/summary").with(admin(store)))
                .andExpect(jsonPath("$.salesTotal").value(5000))
                .andExpect(jsonPath("$.purchasesTotal").value(2500))
                .andExpect(jsonPath("$.balance").value(2500))
                .andExpect(jsonPath("$.purchasesCount").value(2))
                .andExpect(jsonPath("$.purchasesQuantity").value(4));
        mockMvc.perform(get("/trades/summary").param("type", "PAYMENT").with(admin(store)))
                .andExpect(jsonPath("$.purchasesTotal").value(1500))
                .andExpect(jsonPath("$.purchasesQuantity").value(0));

        mockMvc.perform(patch("/trades/{id}", paymentId).with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment(sellerId, "PAID", "1500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment(sellerId, "PURCHASED", "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El estado no corresponde al tipo de movimiento"));
        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PAYMENT\",\"status\":\"PAID\",\"price\":1,\"clientId\":\"" + clientId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El pago necesita un vendedor"));
        // Compras y ventas siguen exigiendo cantidad.
        mockMvc.perform(post("/trades").with(admin(store))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PURCHASE\",\"status\":\"PENDING\",\"price\":1,\"sellerId\":\"" + sellerId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La cantidad es obligatoria"));
        mockMvc.perform(delete("/sellers/{id}", sellerId).with(admin(store)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede eliminar un vendedor con movimientos"));
    }
}
