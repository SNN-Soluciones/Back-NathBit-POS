package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantFilter;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final TenantFilter tenantFilter;
  private final UserDetailsService userDetailsService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Deshabilitar CSRF para APIs REST
        .csrf(AbstractHttpConfigurer::disable)

        // Configurar CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // Configurar manejo de excepciones
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
        )

        // Configurar política de sesiones
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )

        // Configurar autorización de requests
        .authorizeHttpRequests(authz -> authz
            // Rutas públicas - sin autenticación
            .requestMatchers(
                "/api/v2/auth/login",
                "/api/v2/auth/forgot-password",
                "/api/v2/auth/reset-password",
                "/api/v1/auth/login", // Compatibilidad
                "/api/public/**",
                "/actuator/health",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/error"
            ).permitAll()

            // Rutas que requieren autenticación pero no tenant específico
            .requestMatchers(
                "/api/v2/auth/select-context",
                "/api/v2/auth/refresh",
                "/api/v2/auth/validate",
                "/api/v2/auth/logout"
            ).authenticated()

            // Rutas administrativas - requieren rol ADMIN o SUPER_ADMIN
            .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

            // Rutas de caja - requieren roles específicos
            .requestMatchers("/api/cajas/**").hasAnyRole("CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN")

            // Rutas de órdenes - múltiples roles
            .requestMatchers("/api/ordenes/**").hasAnyRole("MESERO", "CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN")

            // Rutas de reportes
            .requestMatchers("/api/reportes/**").hasAnyRole("JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "CONTADOR")

            // Cualquier otra ruta requiere autenticación
            .anyRequest().authenticated()
        )

        // Agregar filtros personalizados
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Orígenes permitidos
    configuration.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:3000",
        "http://localhost:5173",
        "https://*.nathbitpos.com"
    ));

    // Métodos permitidos
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    ));

    // Headers permitidos
    configuration.setAllowedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Requested-With",
        "Accept",
        "Origin",
        "Access-Control-Request-Method",
        "Access-Control-Request-Headers",
        "X-Terminal-Id" // Para detección de terminal
    ));

    // Headers expuestos
    configuration.setExposedHeaders(Arrays.asList(
        "Access-Control-Allow-Origin",
        "Access-Control-Allow-Credentials",
        "Authorization"
    ));

    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}