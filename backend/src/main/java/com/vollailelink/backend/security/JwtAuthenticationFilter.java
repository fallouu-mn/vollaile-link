package com.vollailelink.backend.security;

import com.vollailelink.backend.model.Administrator;
import com.vollailelink.backend.model.enums.AdminStatus;
import com.vollailelink.backend.repository.AdministratorRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdministratorRepository administratorRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AdministratorRepository administratorRepository) {
        this.jwtService = jwtService;
        this.administratorRepository = administratorRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();
        try {
            Claims claims = jwtService.extractClaims(token);
            String phone = claims.getSubject();
            Administrator administrator = administratorRepository.findByPhone(phone).orElse(null);

            if (administrator != null
                    && administrator.getStatus() == AdminStatus.ACTIVE
                    && (administrator.getLockedUntil() == null
                    || !administrator.getLockedUntil().isAfter(java.time.OffsetDateTime.now()))
                    && jwtService.isTokenValid(token, administrator)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                AdminPrincipal principal = new AdminPrincipal(administrator);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // Ne jamais logger le token lui-même.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
