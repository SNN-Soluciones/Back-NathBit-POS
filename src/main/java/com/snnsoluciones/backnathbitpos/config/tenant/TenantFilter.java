package com.snnsoluciones.backnathbitpos.config.tenant;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que intercepta cada request para establecer el tenant actual
 * basándose en el header X-Tenant-ID o en el JWT token
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {
  private final JwtTokenProvider tokenProvider;

  // Rutas que NO requieren tenant
  private static final List<String> PUBLIC_PATHS = Arrays.asList(
      "/api/auth/login",
      "/api/auth/forgot-password",
      "/api/auth/reset-password",
      "/api/public",
      "/actuator",
      "/swagger-ui",
      "/v3/api-docs"
  );

  // Rutas que requieren autenticación pero no tenant específico
  private static final List<String> AUTH_NO_TENANT_PATHS = Arrays.asList(
      "/api/v2/auth/select-context",
      "/api/v2/auth/refresh",
      "/api/v2/auth/validate"
  );

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    String path = request.getRequestURI();

    try {
      // 1. Rutas públicas - no requieren tenant
      if (isPublicPath(path)) {
        log.debug("Ruta pública detectada: {}, sin tenant requerido", path);
        filterChain.doFilter(request, response);
        return;
      }

      // 2. Obtener token del header
      String authHeader = request.getHeader("Authorization");
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        log.debug("No se encontró token de autorización para ruta: {}", path);
        filterChain.doFilter(request, response);
        return;
      }

      String token = authHeader.substring(7);

      // 3. Rutas autenticadas pero sin tenant
      if (isAuthNoTenantPath(path)) {
        log.debug("Ruta autenticada sin tenant: {}", path);
        filterChain.doFilter(request, response);
        return;
      }

      // 4. Extraer tenant del JWT
      String tenantId = tokenProvider.getTenantFromToken(token);

      if (tenantId == null || tenantId.isEmpty()) {
        log.warn("Token sin tenant para ruta que lo requiere: {}", path);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\": \"Tenant no especificado\", " +
                "\"message\": \"Debe seleccionar una empresa y sucursal antes de continuar\"}"
        );
        return;
      }

      // 5. Establecer el tenant en el contexto
      log.debug("Estableciendo tenant: {} para ruta: {}", tenantId, path);
      TenantContext.setCurrentTenant(tenantId);

      // 6. Agregar información adicional al request
      String empresaId = tokenProvider.getEmpresaIdFromToken(token);
      String sucursalId = tokenProvider.getSucursalIdFromToken(token);

      if (empresaId != null) {
        request.setAttribute("empresaId", empresaId);
      }
      if (sucursalId != null) {
        request.setAttribute("sucursalId", sucursalId);
      }
      request.setAttribute("tenantId", tenantId);

      // 7. Continuar con la cadena de filtros
      filterChain.doFilter(request, response);

    } finally {
      // Limpiar el contexto del tenant
      TenantContext.clear();
    }
  }

  /**
   * Verifica si la ruta es pública
   */
  private boolean isPublicPath(String path) {
    return PUBLIC_PATHS.stream()
        .anyMatch(path::startsWith);
  }

  /**
   * Verifica si la ruta requiere autenticación pero no tenant
   */
  private boolean isAuthNoTenantPath(String path) {
    return AUTH_NO_TENANT_PATHS.stream()
        .anyMatch(path::startsWith);
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    // Opción para deshabilitar el filtro para ciertas rutas si es necesario
    String path = request.getRequestURI();
    return path.contains("/static") || path.contains("/public");
  }
}