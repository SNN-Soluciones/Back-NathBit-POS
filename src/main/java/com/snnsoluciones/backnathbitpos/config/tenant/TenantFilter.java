package com.snnsoluciones.backnathbitpos.config.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro que intercepta cada request para establecer el tenant actual
 * basándose en el header X-Tenant-ID o en el JWT token
 */
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

  private static final String TENANT_HEADER = "X-Tenant-ID";
  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider tokenProvider;
  private final ObjectMapper objectMapper;

  // Rutas que no requieren tenant
  private static final String[] EXCLUDED_PATHS = {
      "/api/auth/login",           // Login inicial
      "/api/auth/select-tenant",   // Selección de tenant
      "/api/auth/refresh",         // Refresh token
      "/api/auth/register",        // Registro de usuario
      "/api/auth/register-tenant", // Registro de nuevo tenant
      "/api/auth/forgot-password", // Recuperar contraseña
      "/api/auth/reset-password",  // Resetear contraseña
      "/api/public",              // Endpoints públicos
      "/api/health",              // Health check
      "/swagger-ui",              // Documentación Swagger
      "/v3/api-docs",             // OpenAPI docs
      "/actuator",                // Actuator endpoints
      "/error"                    // Error handling
  };

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String requestURI = request.getRequestURI();
    String method = request.getMethod();

    // Log para debugging
    log.debug("Processing {} request to {}", method, requestURI);

    // Verificar si la ruta está excluida
    if (isExcludedPath(requestURI)) {
      log.debug("Ruta excluida del tenant filter: {}", requestURI);
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // 1. Intentar obtener tenant del header X-Tenant-ID
      String tenantId = request.getHeader(TENANT_HEADER);

      // 2. Si no hay header, intentar obtener del JWT token
      if (!StringUtils.hasText(tenantId)) {
        tenantId = extractTenantFromToken(request);
      }

      // 3. Validar que tengamos un tenant
      if (!StringUtils.hasText(tenantId)) {
        log.warn("No se encontró tenant para la petición: {} {}", method, requestURI);
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
            "TENANT_REQUIRED",
            "Debe seleccionar una empresa primero. Use el endpoint /api/auth/select-tenant");
        return;
      }

      // 4. Establecer el tenant en el contexto
      log.debug("Estableciendo tenant: {} para la petición: {} {}", tenantId, method, requestURI);
      TenantContext.setCurrentTenant(tenantId);

      // 5. Continuar con la cadena de filtros
      filterChain.doFilter(request, response);

    } catch (Exception e) {
      log.error("Error procesando tenant filter", e);
      sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "TENANT_ERROR",
          "Error procesando la información del tenant");
    } finally {
      // Limpiar el contexto después de procesar la petición
      TenantContext.clear();
      log.debug("Tenant context cleared after processing request");
    }
  }

  /**
   * Verifica si la ruta está excluida del filtro de tenant
   */
  private boolean isExcludedPath(String path) {
    for (String excludedPath : EXCLUDED_PATHS) {
      if (path.startsWith(excludedPath)) {
        return true;
      }
    }
    // También excluir recursos estáticos
    return path.contains("/static/") ||
        path.contains("/public/") ||
        path.endsWith(".js") ||
        path.endsWith(".css") ||
        path.endsWith(".ico") ||
        path.endsWith(".png") ||
        path.endsWith(".jpg");
  }

  /**
   * Extrae el tenant ID del JWT token si existe
   */
  private String extractTenantFromToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTH_HEADER);

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      String token = bearerToken.substring(BEARER_PREFIX.length());

      try {
        if (tokenProvider.validateToken(token)) {
          String tenantId = tokenProvider.getTenantFromToken(token);
          if (StringUtils.hasText(tenantId)) {
            log.debug("Tenant {} extraído del JWT token", tenantId);
            return tenantId;
          }
        }
      } catch (Exception e) {
        log.debug("No se pudo extraer tenant del token: {}", e.getMessage());
      }
    }

    return null;
  }

  /**
   * Envía una respuesta de error en formato JSON
   */
  private void sendErrorResponse(HttpServletResponse response, int status,
      String errorCode, String message) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");

    Map<String, Object> errorBody = new HashMap<>();
    errorBody.put("timestamp", System.currentTimeMillis());
    errorBody.put("status", status);
    errorBody.put("error", errorCode);
    errorBody.put("message", message);
    errorBody.put("path", getCurrentRequestPath());

    response.getWriter().write(objectMapper.writeValueAsString(errorBody));
  }

  /**
   * Obtiene el path de la petición actual
   */
  private String getCurrentRequestPath() {
    HttpServletRequest request =
        (HttpServletRequest) org.springframework.web.context.request.RequestContextHolder
            .getRequestAttributes()
            .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST);

    return request != null ? request.getRequestURI() : "";
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Opción adicional para excluir ciertos requests
    String path = request.getRequestURI();
    return path.contains("/websocket") || path.contains("/ws");
  }
}