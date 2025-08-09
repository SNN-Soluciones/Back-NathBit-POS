package com.snnsoluciones.backnathbitpos.security;

import com.snnsoluciones.backnathbitpos.dto.auth.ContextoDTO;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.service.auth.ContextoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro para inyectar el contexto de empresa/sucursal en cada request
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextoFilter extends OncePerRequestFilter {
    
    private final ContextoService contextoService;
    private final AuthService authService;
    
    // Thread local para almacenar el contexto actual
    private static final ThreadLocal<ContextoDTO> contextoHolder = new ThreadLocal<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Obtener el usuario autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                
                // Extraer token
                String token = extractTokenFromRequest(request);
                
                if (token != null) {
                    // Obtener el contexto del usuario
                    Long usuarioId = authService.obtenerUsuarioIdDesdeToken(token);
                    ContextoDTO contexto = contextoService.obtenerContextoActual(usuarioId);
                    
                    if (contexto != null) {
                        // Guardar en thread local
                        contextoHolder.set(contexto);
                        
                        // Agregar headers de contexto a la respuesta (útil para el frontend)
                        response.setHeader("X-Contexto-Empresa-Id", 
                            contexto.getEmpresaId() != null ? contexto.getEmpresaId().toString() : "");
                        response.setHeader("X-Contexto-Sucursal-Id", 
                            contexto.getSucursalId() != null ? contexto.getSucursalId().toString() : "");
                        
                        // Actualizar actividad
                        contextoService.actualizarActividad(usuarioId);
                        
                        log.debug("Contexto establecido - Empresa: {}, Sucursal: {}", 
                                 contexto.getEmpresaId(), contexto.getSucursalId());
                    }
                }
            }
            
            // Continuar con la cadena de filtros
            filterChain.doFilter(request, response);
            
        } finally {
            // Limpiar thread local
            contextoHolder.remove();
        }
    }
    
    /**
     * Obtiene el contexto actual del thread
     */
    public static ContextoDTO getContextoActual() {
        return contextoHolder.get();
    }
    
    /**
     * Obtiene el ID de la empresa del contexto actual
     */
    public static Long getEmpresaIdActual() {
        ContextoDTO contexto = contextoHolder.get();
        return contexto != null ? contexto.getEmpresaId() : null;
    }
    
    /**
     * Obtiene el ID de la sucursal del contexto actual
     */
    public static Long getSucursalIdActual() {
        ContextoDTO contexto = contextoHolder.get();
        return contexto != null ? contexto.getSucursalId() : null;
    }
    
    /**
     * Verifica si hay un contexto establecido
     */
    public static boolean hayContextoEstablecido() {
        return contextoHolder.get() != null;
    }
    
    /**
     * Extrae el token JWT del header Authorization
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // No filtrar rutas públicas
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/auth/recuperar-password") ||
               path.startsWith("/api/auth/restablecer-password") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs");
    }
}