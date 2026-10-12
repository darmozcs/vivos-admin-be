package com.vivos.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityTest extends AbstractIntegrationTest {

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/clients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void validSignedTokenOfVivosIsAccepted() throws Exception {
        TestStore store = createStore("Tienda token", true);
        String token = signedToken(store.userId(), APP_ID, "access", "darmoz-auth", "ADMIN");

        mockMvc.perform(get("/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Tienda token"));
    }

    @Test
    void tokenOfAnotherApplicationIsRejected() throws Exception {
        TestStore store = createStore("Tienda otra app", true);
        String token = signedToken(store.userId(), OTHER_APP_ID, "access", "darmoz-auth", "ADMIN");

        mockMvc.perform(get("/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenIsRejected() throws Exception {
        TestStore store = createStore("Tienda refresh", true);
        String token = signedToken(store.userId(), APP_ID, "refresh", "darmoz-auth", "ADMIN");

        mockMvc.perform(get("/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithOtherIssuerIsRejected() throws Exception {
        TestStore store = createStore("Tienda issuer", true);
        String token = signedToken(store.userId(), APP_ID, "access", "otro-issuer", "ADMIN");

        mockMvc.perform(get("/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void storeAdminCannotAccessAdminStores() throws Exception {
        TestStore store = createStore("Tienda admin", true);

        mockMvc.perform(get("/admin/stores").with(admin(store)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void storeAdminWithSignedTokenCannotAccessAdminStores() throws Exception {
        TestStore store = createStore("Tienda admin firmado", true);
        String token = signedToken(store.userId(), APP_ID, "access", "darmoz-auth", "ADMIN");

        mockMvc.perform(get("/admin/stores").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminCannotAccessStoreEndpoints() throws Exception {
        mockMvc.perform(get("/clients").with(superAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/settings").with(superAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/lives").with(superAdmin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/orders").with(superAdmin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveStoreIsForbidden() throws Exception {
        TestStore store = createStore("Tienda inactiva", false);

        mockMvc.perform(get("/clients").with(admin(store)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("La tienda esta desactivada"));
    }

    @Test
    void adminWithoutStoreIsForbidden() throws Exception {
        mockMvc.perform(get("/clients").with(asUser(UUID.randomUUID(), "ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("El usuario no tiene una tienda asignada"));
    }

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/clients")
                        .header("Origin", TEST_ORIGIN)
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", TEST_ORIGIN));
    }
}
