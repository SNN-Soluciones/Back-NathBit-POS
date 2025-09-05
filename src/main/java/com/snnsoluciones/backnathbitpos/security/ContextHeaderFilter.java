package com.snnsoluciones.backnathbitpos.security;

import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro para procesar headers de contexto empresa/sucursal
 * Se ejecuta DESPUÉS del JwtAuthenticationFilter
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class ContextHeaderFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        // Solo procesar si ya hay autenticación
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof ContextoUsuario) {
            ContextoUsuario contexto = (ContextoUsuario) auth.getPrincipal();

            // Procesar headers de contexto si vienen
            String empresaIdHeader = request.getHeader("X-Empresa-Id");
            String sucursalIdHeader = request.getHeader("X-Sucursal-Id");

            // Actualizar contexto si vienen headers válidos
            if (StringUtils.hasText(empresaIdHeader)) {
                try {
                    Long empresaId = Long.parseLong(empresaIdHeader);

                    // Validar que el usuario tenga acceso a esta empresa
                    if (validarAccesoEmpresa(contexto, empresaId)) {
                        contexto.setEmpresaId(empresaId);
                        log.debug("Contexto de empresa establecido desde header: {}", empresaId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Header X-Empresa-Id inválido: {}", empresaIdHeader);
                }
            }

            if (StringUtils.hasText(sucursalIdHeader)) {
                try {
                    Long sucursalId = Long.parseLong(sucursalIdHeader);

                    // Validar que el usuario tenga acceso a esta sucursal
                    if (validarAccesoSucursal(contexto, sucursalId)) {
                        contexto.setSucursalId(sucursalId);
                        log.debug("Contexto de sucursal establecido desde header: {}", sucursalId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Header X-Sucursal-Id inválido: {}", sucursalIdHeader);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Valida si el usuario tiene acceso a la empresa
     */
    private boolean validarAccesoEmpresa(ContextoUsuario contexto, Long empresaId) {
        // ROOT y SOPORTE tienen acceso a todo
        if ("ROOT".equals(contexto.getRol()) || "SOPORTE".equals(contexto.getRol())) {
            return true;
        }

        // Para otros roles, aquí se debería validar contra la BD
        // Por ahora retornamos true, pero deberías implementar la validación real
        return true;
    }

    /**
     * Valida si el usuario tiene acceso a la sucursal
     */
    private boolean validarAccesoSucursal(ContextoUsuario contexto, Long sucursalId) {
        // ROOT y SOPORTE tienen acceso a todo
        if ("ROOT".equals(contexto.getRol()) || "SOPORTE".equals(contexto.getRol())) {
            return true;
        }

        // Para otros roles, aquí se debería validar contra la BD
        // Por ahora retornamos true, pero deberías implementar la validación real
        return true;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // No filtrar endpoints públicos
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/refresh") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs");
    }
}