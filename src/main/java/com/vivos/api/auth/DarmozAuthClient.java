package com.vivos.api.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cliente de los endpoints {@code /apps/api/users} de darmoz-auth.
 * <p>
 * Reenvia el header {@code Authorization} del request entrante (el Bearer del Super Admin,
 * que tiene el rol {@code APP_ADMIN}) y envia {@code API_ID} con el id de la aplicacion Vivos.
 * vivos-be no guarda ninguna credencial propia.
 */
@Component
public class DarmozAuthClient {

    static final String API_ID_HEADER = "API_ID";
    static final String USERS_PATH = "/apps/api/users";
    static final String UNREACHABLE_MESSAGE = "No se pudo contactar al servicio de autenticacion";

    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    DarmozAuthClient(@Value("${vivos.darmoz-auth-url}") String baseUrl,
                     @Value("${vivos.application-id}") String applicationId) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(API_ID_HEADER, applicationId)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw toException(response);
                })
                .build();
    }

    public List<AdminUserResponse> listUsers() {
        return call(() -> restClient.get()
                .uri(USERS_PATH)
                .headers(this::forwardAuthorization)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AdminUserResponse>>() {
                }));
    }

    public AdminUserResponse createUser(String email, String password, List<String> roles) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("roles", roles);
        return call(() -> restClient.post()
                .uri(USERS_PATH)
                .headers(this::forwardAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AdminUserResponse.class));
    }

    /** PATCH parcial: solo se envian los campos no nulos. */
    public AdminUserResponse updateUser(UUID userId, Boolean enabled, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (enabled != null) {
            body.put("enabled", enabled);
        }
        if (password != null) {
            body.put("password", password);
        }
        return call(() -> restClient.patch()
                .uri(USERS_PATH + "/{id}", userId)
                .headers(this::forwardAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AdminUserResponse.class));
    }

    public void deleteUser(UUID userId) {
        call(() -> restClient.delete()
                .uri(USERS_PATH + "/{id}", userId)
                .headers(this::forwardAuthorization)
                .retrieve()
                .toBodilessEntity());
    }

    public void sendVerificationEmail(UUID userId) {
        call(() -> restClient.post()
                .uri(USERS_PATH + "/{id}/verification-email", userId)
                .headers(this::forwardAuthorization)
                .retrieve()
                .toBodilessEntity());
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (DarmozAuthException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new DarmozAuthException(HttpStatus.BAD_GATEWAY, UNREACHABLE_MESSAGE, ex);
        } catch (RestClientException ex) {
            throw new DarmozAuthException(HttpStatus.BAD_GATEWAY,
                    "Respuesta invalida del servicio de autenticacion", ex);
        }
    }

    private void forwardAuthorization(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
    }

    private DarmozAuthException toException(ClientHttpResponse response) throws IOException {
        HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        String message = null;
        try {
            byte[] body = response.getBody().readAllBytes();
            if (body.length > 0) {
                message = jsonMapper.readValue(body, ErrorBody.class).message();
            }
        } catch (RuntimeException ignored) {
            // Cuerpo no JSON: se usa el mensaje generico.
        }
        if (message == null || message.isBlank()) {
            message = "Error del servicio de autenticacion (" + status.value() + ")";
        }
        return new DarmozAuthException(status, message);
    }

    /** Cuerpo de error de darmoz-auth: {@code {timestamp, status, error, message}}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorBody(String error, String message) {
    }
}
