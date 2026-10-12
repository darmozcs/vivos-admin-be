package com.vivos.api;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vivos.api.auth.DarmozAuthClient;
import com.vivos.api.store.Store;
import com.vivos.api.store.StoreRepository;
import com.vivos.api.store.StoreUser;
import com.vivos.api.store.StoreUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Base de los tests de integracion: contexto completo, Postgres en Testcontainers,
 * darmoz-auth mockeado y un par de llaves RSA propio para firmar JWT reales cuando
 * se quiere probar el decoder (issuer, typ, applicationId).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    protected static final String APP_ID = "22222222-2222-2222-2222-222222222222";
    protected static final String OTHER_APP_ID = "11111111-1111-1111-1111-111111111111";

    private static final KeyPair KEY_PAIR;
    private static final Path PUBLIC_KEY_PATH;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KEY_PAIR = generator.generateKeyPair();
            PUBLIC_KEY_PATH = Files.createTempFile("vivos-test-public-key", ".pem");
            String pem = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n";
            Files.writeString(PUBLIC_KEY_PATH, pem);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("No se pudieron generar las llaves RSA de test", e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.public-key-location", () -> PUBLIC_KEY_PATH.toUri().toString());
        registry.add("vivos.application-id", () -> APP_ID);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected StoreRepository storeRepository;

    @Autowired
    protected StoreUserRepository storeUserRepository;

    @MockitoBean
    protected DarmozAuthClient darmozAuthClient;

    /** Crea una tienda con su admin y devuelve el id del usuario admin (el {@code sub}). */
    protected TestStore createStore(String name, boolean active) {
        Store store = new Store();
        store.setName(name);
        store.setActive(active);
        store = storeRepository.save(store);
        UUID userId = UUID.randomUUID();
        storeUserRepository.save(new StoreUser(userId, store.getId(), userId + "@test.com"));
        return new TestStore(store.getId(), userId);
    }

    /** JWT de test (sin firma) con sub y roles como authorities, via spring-security-test. */
    protected static RequestPostProcessor asUser(UUID userId, String... roles) {
        return jwt()
                .jwt(builder -> builder
                        .subject(userId.toString())
                        .claim("roles", List.of(roles))
                        .claim("applicationId", APP_ID)
                        .claim("typ", "access")
                        .issuer("darmoz-auth"))
                .authorities(Arrays.stream(roles)
                        .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList());
    }

    protected static RequestPostProcessor admin(TestStore store) {
        return asUser(store.userId(), "ADMIN");
    }

    protected static RequestPostProcessor superAdmin() {
        return asUser(UUID.randomUUID(), "SUPER_ADMIN", "APP_ADMIN");
    }

    /** Access token firmado de verdad, para pasar por el NimbusJwtDecoder y sus validadores. */
    protected static String signedToken(UUID subject, String applicationId, String type, String issuer,
                                        String... roles) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject.toString())
                    .issuer(issuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(600)))
                    .claim("roles", List.of(roles))
                    .claim("applicationId", applicationId)
                    .claim("typ", type)
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    protected record TestStore(UUID storeId, UUID userId) {
    }
}
