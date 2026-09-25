package com.vollailelink.backend;

import com.vollailelink.backend.model.Administrator;
import com.vollailelink.backend.model.enums.AdminStatus;
import com.vollailelink.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-for-hmac-sha256";

    @Test
    void generatesTokenWithAdminPhoneAndValidatesIt() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        Administrator administrator = Administrator.builder()
                .id(7L)
                .phone("+221771234567")
                .passwordHash("hash")
                .status(AdminStatus.ACTIVE)
                .mustChangePassword(true)
                .build();

        String token = jwtService.generateToken(administrator);
        Claims claims = jwtService.extractClaims(token);

        assertEquals("+221771234567", claims.getSubject());
        assertTrue(jwtService.isTokenValid(token, administrator));
    }

    @Test
    void rejectsTokenForAnotherAdministrator() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        Administrator administrator = Administrator.builder()
                .id(7L)
                .phone("+221771234567")
                .passwordHash("hash")
                .status(AdminStatus.ACTIVE)
                .mustChangePassword(false)
                .build();
        Administrator otherAdministrator = Administrator.builder()
                .id(8L)
                .phone("+221771234568")
                .passwordHash("hash")
                .status(AdminStatus.ACTIVE)
                .mustChangePassword(false)
                .build();

        String token = jwtService.generateToken(administrator);

        assertFalse(jwtService.isTokenValid(token, otherAdministrator));
    }
}
