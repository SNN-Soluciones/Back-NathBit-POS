package com.snnsoluciones.backnathbitpos.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
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
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extraer token del header
            String jwt = getJwtFromRequest(request);

            // Validar token y establecer autenticación
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                
                // Obtener username del token
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                
                // Cargar usuario
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Crear autenticación con el contexto del token
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Establecer en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Agregar información adicional al request para uso posterior
                request.setAttribute("empresa_id", jwtTokenProvider.getEmpresaIdFromToken(jwt));
                request.setAttribute("sucursal_id", jwtTokenProvider.getSucursalIdFromToken(jwt));
                request.setAttribute("rol", jwtTokenProvider.getRolFromToken(jwt));
                
                log.debug("Autenticación establecida para usuario: {}", username);
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