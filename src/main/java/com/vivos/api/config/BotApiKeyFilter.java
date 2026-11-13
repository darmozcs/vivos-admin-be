package com.vivos.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autentica las llamadas del bot de WhatsApp (darmoz-whatsapp) a {@code /bot/**} con
 * una API key fija en el header {@code X-Api-Key}. Sin key configurada, {@code /bot}
 * queda cerrado. No es un {@code @Component} para que no se registre en todas las rutas:
 * solo lo usa la cadena de seguridad de {@code /bot/**}.
 */
final class BotApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Api-Key";
    static final String ROLE = "BOT";
    private static final String PRINCIPAL = "darmoz-whatsapp";

    private final byte[] apiKey;

    BotApiKeyFilter(String apiKey) {
        this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.trim().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (apiKey == null || provided == null
                || !MessageDigest.isEqual(apiKey, provided.trim().getBytes(StandardCharsets.UTF_8))) {
            ProblemDetailResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "API key ausente o invalida");
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(PRINCIPAL, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + ROLE))));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
