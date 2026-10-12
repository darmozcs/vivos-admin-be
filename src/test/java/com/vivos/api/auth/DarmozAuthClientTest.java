package com.vivos.api.auth;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba el cliente HTTP real contra un servidor local que simula darmoz-auth. */
class DarmozAuthClientTest {

    private static final String APP_ID = "22222222-2222-2222-2222-222222222222";
    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String USER_JSON = """
            {"id":"33333333-3333-3333-3333-333333333333","email":"admin@tienda.com","enabled":true,
             "emailVerified":false,"unverifiedLoginCount":0,"applicationId":"%s","applicationName":"Vivos",
             "roles":["ADMIN"],"createdAt":"2026-10-01T12:00:00Z","updatedAt":"2026-10-01T12:00:00Z","extra":1}
            """.formatted(APP_ID);

    private HttpServer server;
    private DarmozAuthClient client;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> apiId = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = USER_JSON;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/auth", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiId.set(exchange.getRequestHeaders().getFirst("API_ID"));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            if (bytes.length == 0) {
                exchange.sendResponseHeaders(responseStatus, -1);
            } else {
                exchange.sendResponseHeaders(responseStatus, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
        client = new DarmozAuthClient("http://127.0.0.1:" + server.getAddress().getPort() + "/auth", APP_ID);

        MockHttpServletRequest incoming = new MockHttpServletRequest();
        incoming.addHeader("Authorization", "Bearer super-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incoming));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        server.stop(0);
    }

    @Test
    void createUserForwardsHeadersAndParsesResponse() {
        responseStatus = 201;

        AdminUserResponse user = client.createUser("admin@tienda.com", "secreta123", List.of("ADMIN"));

        assertThat(method.get()).isEqualTo("POST");
        assertThat(path.get()).isEqualTo("/auth/apps/api/users");
        assertThat(apiId.get()).isEqualTo(APP_ID);
        assertThat(authorization.get()).isEqualTo("Bearer super-token");
        assertThat(body.get()).contains("\"email\":\"admin@tienda.com\"", "\"roles\":[\"ADMIN\"]");
        assertThat(user.id()).isEqualTo(USER_ID);
        assertThat(user.emailVerified()).isFalse();
        assertThat(user.roles()).containsExactly("ADMIN");
    }

    @Test
    void updateUserSendsOnlyNonNullFieldsWithPatch() {
        client.updateUser(USER_ID, false, null);

        assertThat(method.get()).isEqualTo("PATCH");
        assertThat(path.get()).isEqualTo("/auth/apps/api/users/" + USER_ID);
        assertThat(body.get()).isEqualTo("{\"enabled\":false}");
    }

    @Test
    void listUsersParsesArray() {
        responseBody = "[" + USER_JSON + "]";

        assertThat(client.listUsers()).extracting(AdminUserResponse::id).containsExactly(USER_ID);
        assertThat(method.get()).isEqualTo("GET");
    }

    @Test
    void deleteAndVerificationEmail() {
        responseStatus = 204;
        responseBody = "";

        client.deleteUser(USER_ID);
        assertThat(method.get()).isEqualTo("DELETE");

        client.sendVerificationEmail(USER_ID);
        assertThat(method.get()).isEqualTo("POST");
        assertThat(path.get()).isEqualTo("/auth/apps/api/users/" + USER_ID + "/verification-email");
    }

    @Test
    void errorsKeepStatusAndMessage() {
        responseStatus = 409;
        responseBody = """
                {"timestamp":"2026-10-01T12:00:00Z","status":409,"error":"Conflict","message":"El email ya esta registrado"}
                """;

        assertThatThrownBy(() -> client.createUser("a@b.com", "secreta123", List.of("ADMIN")))
                .isInstanceOfSatisfying(DarmozAuthException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getMessage()).isEqualTo("El email ya esta registrado");
                });
    }

    @Test
    void networkErrorIsBadGateway() {
        server.stop(0);

        assertThatThrownBy(() -> client.listUsers())
                .isInstanceOfSatisfying(DarmozAuthException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
