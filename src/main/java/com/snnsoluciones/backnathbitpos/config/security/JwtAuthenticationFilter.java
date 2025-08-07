package com.snnsoluciones.backnathbitpos.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que se ejecuta en cada request para validar el token JWT.
 * Extrae el token del header Authorization y establece la autenticación en el contexto.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extraer token del header
            String jwt = getJwtFromRequest(request);

            // Validar token y establecer autenticación
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // Obtener ID del usuario del token
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);

                // En lugar de cargar por username, usar el ID directamente
                // Esto es más eficiente y preciso
                CustomUserDetailsService customUserDetailsService =
                    (CustomUserDetailsService) userDetailsService;

                UserDetails userDetails;
                try {
                    // Intentar cargar por ID si tenemos CustomUserDetailsService
                    userDetails = customUserDetailsService.loadUserById(userId);
                } catch (Exception e) {
                    // Fallback: cargar por username
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    userDetails = userDetailsService.loadUserByUsername(username);
                }

                // Crear autenticación
                // Importante: Usamos el userId como principal para facilitar el acceso en los servicios
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userId,  // Usamos el ID como principal
                        null,
                        userDetails.getAuthorities()
                    );

                // Agregar detalles adicionales
                Map<String, Object> details = new HashMap<>();
                details.put("email", userDetails.getUsername());
                details.put("empresaId", jwtTokenProvider.getEmpresaIdFromToken(jwt));
                details.put("sucursalId", jwtTokenProvider.getSucursalIdFromToken(jwt));
                details.put("rol", jwtTokenProvider.getRolFromToken(jwt));

                authentication.setDetails(details);

                // Establecer en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // También agregar al request para fácil acceso
                request.setAttribute("usuario_id", userId);
                request.setAttribute("empresa_id", jwtTokenProvider.getEmpresaIdFromToken(jwt));
                request.setAttribute("sucursal_id", jwtTokenProvider.getSucursalIdFromToken(jwt));
                request.setAttribute("rol", jwtTokenProvider.getRolFromToken(jwt));

                log.debug("Autenticación establecida para usuario ID: {}", userId);
            }
        } catch (Exception ex) {
            log.error("No se pudo establecer la autenticación del usuario", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del header Authorization.
     * Espera el formato: Bearer <token>
     * 
     * @param request la petición HTTP
     * @return el token JWT o null si no está presente
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}