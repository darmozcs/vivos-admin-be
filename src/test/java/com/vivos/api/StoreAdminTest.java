package com.vivos.api;

import com.vivos.api.auth.AdminUserResponse;
import com.vivos.api.auth.DarmozAuthException;
import com.vivos.api.store.Store;
import com.vivos.api.store.StoreUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoreAdminTest extends AbstractIntegrationTest {

    private static AdminUserResponse authUser(UUID id, String email, boolean enabled, boolean emailVerified) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdminUserResponse(id, email, enabled, emailVerified, 0,
                UUID.fromString(APP_ID), "Vivos", Set.of("ADMIN"), now, now);
    }

    @Test
    void createStoreHappyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        when(darmozAuthClient.createUser(eq("nueva@tienda.com"), eq("secreta123"), eq(List.of("ADMIN"))))
                .thenReturn(authUser(userId, "nueva@tienda.com", true, false));

        mockMvc.perform(post("/admin/stores").with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Tienda Nueva ","adminEmail":"Nueva@Tienda.com","adminPassword":"secreta123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Tienda Nueva"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.adminUserId").value(userId.toString()))
                .andExpect(jsonPath("$.adminEmail").value("nueva@tienda.com"))
                .andExpect(jsonPath("$.adminEmailVerified").value(false))
                .andExpect(jsonPath("$.createdAt").exists());

        StoreUser storeUser = storeUserRepository.findById(userId).orElseThrow();
        Store store = storeRepository.findById(storeUser.getStoreId()).orElseThrow();
        assertThat(store.getName()).isEqualTo("Tienda Nueva");
        assertThat(store.isActive()).isTrue();
        verify(darmozAuthClient, never()).deleteUser(any());
    }

    @Test
    void createStoreCompensatesWhenDatabaseFails() throws Exception {
        // El usuario "creado" en darmoz-auth ya tiene tienda: el INSERT de viv_store_user falla.
        TestStore existing = createStore("Tienda existente", true);
        long storesBefore = storeRepository.count();
        when(darmozAuthClient.createUser(anyString(), anyString(), anyList()))
                .thenReturn(authUser(existing.userId(), "dup@tienda.com", true, false));

        mockMvc.perform(post("/admin/stores").with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tienda rota","adminEmail":"dup@tienda.com","adminPassword":"secreta123"}
                                """))
                .andExpect(status().isConflict());

        verify(darmozAuthClient).deleteUser(existing.userId());
        assertThat(storeRepository.count()).isEqualTo(storesBefore);
    }

    @Test
    void createStoreMapsDarmozAuthConflict() throws Exception {
        when(darmozAuthClient.createUser(anyString(), anyString(), anyList()))
                .thenThrow(new DarmozAuthException(HttpStatus.CONFLICT, "El email ya esta registrado"));

        mockMvc.perform(post("/admin/stores").with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tienda","adminEmail":"ya@existe.com","adminPassword":"secreta123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El email ya esta registrado"));
    }

    @Test
    void createStoreValidatesBody() throws Exception {
        mockMvc.perform(post("/admin/stores").with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","adminEmail":"no-es-email","adminPassword":"corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.adminEmail").value("el email del administrador no es valido"))
                .andExpect(jsonPath("$.adminPassword").value("la contraseña debe tener entre 8 y 100 caracteres"));
    }

    @Test
    void listStoresUsesOneDarmozAuthCall() throws Exception {
        TestStore store = createStore("Tienda listada", true);
        when(darmozAuthClient.listUsers())
                .thenReturn(List.of(authUser(store.userId(), store.userId() + "@test.com", true, true)));

        mockMvc.perform(get("/admin/stores").with(superAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + store.storeId() + "')].adminEmailVerified").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.id == '" + store.storeId() + "')].name").value(hasItem("Tienda listada")));

        verify(darmozAuthClient).listUsers();
    }

    @Test
    void listStoresFailsLoudlyWhenDarmozAuthIsDown() throws Exception {
        when(darmozAuthClient.listUsers())
                .thenThrow(new DarmozAuthException(HttpStatus.BAD_GATEWAY, "No se pudo contactar al servicio de autenticacion"));

        mockMvc.perform(get("/admin/stores").with(superAdmin()))
                .andExpect(status().isBadGateway());
    }

    @Test
    void deactivatingStoreDisablesAdminInDarmozAuth() throws Exception {
        TestStore store = createStore("Tienda a desactivar", true);
        when(darmozAuthClient.updateUser(store.userId(), false, null))
                .thenReturn(authUser(store.userId(), "x@test.com", false, true));

        mockMvc.perform(patch("/admin/stores/{id}", store.storeId()).with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.adminEmailVerified").value(true));

        verify(darmozAuthClient).updateUser(store.userId(), false, null);
        assertThat(storeRepository.findById(store.storeId()).orElseThrow().isActive()).isFalse();

        // El admin pierde el acceso a la tienda al instante.
        mockMvc.perform(get("/clients").with(admin(store)))
                .andExpect(status().isForbidden());
    }

    @Test
    void renamingStoreDoesNotCallDarmozAuth() throws Exception {
        TestStore store = createStore("Nombre viejo", true);

        mockMvc.perform(patch("/admin/stores/{id}", store.storeId()).with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nombre nuevo\",\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nombre nuevo"));

        verify(darmozAuthClient, never()).updateUser(any(), any(), any());
    }

    @Test
    void patchUnknownStoreIsNotFound() throws Exception {
        mockMvc.perform(patch("/admin/stores/{id}", UUID.randomUUID()).with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resetAdminPasswordCallsDarmozAuth() throws Exception {
        TestStore store = createStore("Tienda password", true);

        mockMvc.perform(put("/admin/stores/{id}/admin-password", store.storeId()).with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"nuevaClave1\"}"))
                .andExpect(status().isNoContent());

        verify(darmozAuthClient).updateUser(eq(store.userId()), isNull(), eq("nuevaClave1"));
    }

    @Test
    void resendVerificationEmailCallsDarmozAuth() throws Exception {
        TestStore store = createStore("Tienda verificacion", true);

        mockMvc.perform(post("/admin/stores/{id}/admin-verification-email", store.storeId()).with(superAdmin()))
                .andExpect(status().isNoContent());

        verify(darmozAuthClient).sendVerificationEmail(store.userId());
    }
}
