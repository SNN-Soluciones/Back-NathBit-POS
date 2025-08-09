package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.security.ContextoFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad actualizada para el nuevo modelo
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final UserDetailsService userDetailsService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ContextoFilter contextoFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> auth
            // Rutas públicas
            .requestMatchers(
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/recuperar-password",
                "/api/auth/restablecer-password",
                "/api/public/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**"
            ).permitAll()

            // Rutas del sistema (ROOT y SOPORTE)
            .requestMatchers("/api/sistema/**")
            .hasAnyRole("ROOT", "SOPORTE")

            // Dashboard sistema
            .requestMatchers("/api/dashboard-sistema/**")
            .hasAnyRole("ROOT", "SOPORTE")

            // Gestión de empresas (requiere contexto)
            .requestMatchers("/api/empresas/**")
            .hasAnyRole("ROOT", "SOPORTE", "SUPER_ADMIN", "ADMIN")

            // Gestión de sucursales (requiere contexto)
            .requestMatchers("/api/sucursales/**")
            .hasAnyRole("ROOT", "SOPORTE", "SUPER_ADMIN", "ADMIN")

            // Gestión de usuarios
            .requestMatchers("/api/usuarios/perfil/**")
            .authenticated()
            .requestMatchers("/api/usuarios/**")
            .hasAnyRole("ROOT", "SOPORTE", "SUPER_ADMIN", "ADMIN")

            // Operaciones del POS
            .requestMatchers("/api/pos/**")
            .hasAnyRole("ADMIN", "JEFE_CAJAS", "CAJERO", "MESERO")

            // Reportes
            .requestMatchers("/api/reportes/**")
            .hasAnyRole("ROOT", "SOPORTE", "SUPER_ADMIN", "ADMIN", "JEFE_CAJAS")

            // Todas las demás rutas requieren autenticación
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(contextoFilter, JwtAuthenticationFilter.class); // Contexto después de JWT

    return http.build();
  }
}