package com.adserve.security;

import com.adserve.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
    }

    @Test
    @DisplayName("generateToken creates valid JWT with expected claims")
    void testGenerateTokenAndExtractClaims() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(10L)
                .name("John Advertiser")
                .email("john@example.com")
                .role(Role.ADVERTISER)
                .enabled(true)
                .advertiserId(5L)
                .build();

        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("john@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(10L);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADVERTISER");
        assertThat(jwtService.extractAdvertiserId(token)).isEqualTo(5L);
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for malformed or tampered token")
    void testValidateInvalidToken() {
        assertThat(jwtService.validateToken("invalid.token.structure")).isFalse();
        assertThat(jwtService.validateToken(null)).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for expired token")
    void testExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L); // Already expired

        UserPrincipal principal = UserPrincipal.builder()
                .id(10L)
                .name("Expired User")
                .email("expired@example.com")
                .role(Role.ADVERTISER)
                .enabled(true)
                .build();

        String expiredToken = jwtService.generateToken(principal);
        assertThat(jwtService.validateToken(expiredToken)).isFalse();
    }
}
