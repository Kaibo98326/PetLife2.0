package com.petlife.config;

import java.io.IOException;
import java.security.Key;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminBeautyReadOnlyFilter extends OncePerRequestFilter {

    private static final String BEAUTY_ADMIN_PREFIX = "/api/admin/beauty/";
    private static final String ROLE_GROOMER = "groomer";
    private static final String ROLE_SUPERUSER = "superuser";

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!isBeautyAdminRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractBearerToken(request);
        if (token == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing employee token");
            return;
        }

        List<String> roles;
        try {
            roles = extractRoles(token);
        } catch (JwtException | IllegalArgumentException ex) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid employee token");
            return;
        }

        if (roles.contains(ROLE_SUPERUSER) || isReadRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (roles.contains(ROLE_GROOMER)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Groomer role is read-only for beauty admin APIs");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBeautyAdminRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.startsWith(BEAUTY_ADMIN_PREFIX);
    }

    private boolean isReadRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7).trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return Collections.emptyList();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
