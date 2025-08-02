package com.snnsoluciones.backnathbitpos.config.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que intercepta cada request para establecer el tenant actual
 * basándose en el header X-Tenant-ID
 */
@Component
@Order(1)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

  private static final String TENANT_HEADER = "X-Tenant-ID";
  private static final String[] EXCLUDED_PATHS = {
      "/api/auth/register-tenant",
      "/api/public",
      "/swagger-ui",
      "/v3/api-docs",
      "/actuator"
  };

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String requestURI = request.getRequestURI();

    // Verificar si la ruta está excluida
    if (isExcludedPath(requestURI)) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String tenantId = request.getHeader(TENANT_HEADER);

      if (tenantId == null || tenantId.trim().isEmpty()) {
        log.warn("No se encontró el header {} en la petición a: {}", TENANT_HEADER, requestURI);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"error\": \"Tenant ID es requerido\"}");
        return;
      }

      log.debug("Estableciendo tenant: {} para la petición: {}", tenantId, requestURI);
      TenantContext.setCurrentTenant(tenantId);

      filterChain.doFilter(request, response);

    } finally {
      // Limpiar el contexto después de procesar la petición
      TenantContext.clear();
    }
  }

  private boolean isExcludedPath(String path) {
    for (String excludedPath : EXCLUDED_PATHS) {
      if (path.startsWith(excludedPath)) {
        return true;
      }
    }
    return false;
  }
}