package com.snnsoluciones.backnathbitpos.config;

import com.snnsoluciones.backnathbitpos.config.security.JwtAuthenticationEntryPoint;
import com.snnsoluciones.backnathbitpos.config.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración principal de Spring Security.
 * Define las reglas de seguridad, autenticación y autorización.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,  // Habilita @PreAuthorize y @PostAuthorize
    securedEnabled = true,  // Habilita @Secured
    jsr250Enabled = true    // Habilita @RolesAllowed
)
public class SecurityConfig {

  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final CorsConfigurationSource corsConfigurationSource;


  public SecurityConfig(
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      @Qualifier("customCors") CorsConfigurationSource corsConfigurationSource // 👈 este es el fix
  ) {
    this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.corsConfigurationSource = corsConfigurationSource;
  }

  /**
   * Configuración principal de la cadena de filtros de seguridad.
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Configuración CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource))

        // Deshabilitar CSRF (no necesario con JWT)
        .csrf(AbstractHttpConfigurer::disable)

        // Configurar manejo de excepciones
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
        )

        // Configurar política de sesión (STATELESS para REST API)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )

        // Configurar autorizaciones
        .authorizeHttpRequests(authz -> authz
            // Endpoints públicos
            .requestMatchers(
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/forgot-password",
                "/api/auth/reset-password",
                "/api/health",
                "/api/version"
            ).permitAll()

            // Swagger/OpenAPI
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/webjars/**",
                "/api/test/**"
            ).permitAll()

            // Archivos estáticos
            .requestMatchers(
                "/",
                "/favicon.ico",
                "/**/*.png",
                "/**/*.gif",
                "/**/*.svg",
                "/**/*.jpg",
                "/**/*.html",
                "/**/*.css",
                "/**/*.js"
            ).permitAll()

            // Endpoints de administración - Solo SUPER_ADMIN y ROOT
            .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ROOT")

            // Endpoints de empresa - Admin y superiores
            .requestMatchers("/api/empresas/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "ROOT")

            // Endpoints de gestión de usuarios - Según jerarquía
            .requestMatchers("/api/usuarios/*/roles").hasAnyRole("ADMIN", "SUPER_ADMIN", "ROOT")

            // Endpoints operativos - Roles específicos
            .requestMatchers("/api/pos/**").hasAnyRole("CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT")
            .requestMatchers("/api/ordenes/**").hasAnyRole("MESERO", "CAJERO", "JEFE_CAJAS", "ADMIN", "SUPER_ADMIN", "ROOT")

            // Todos los demás endpoints requieren autenticación
            .anyRequest().authenticated()
        )

        // Agregar filtro JWT antes del filtro de autenticación por username/password
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

//  /**
//   * Proveedor de autenticación que usa UserDetailsService y PasswordEncoder.
//   */
//  @Bean
//  public DaoAuthenticationProvider authenticationProvider() {
//    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//    authProvider.setUserDetailsService(userDetailsService);
//    authProvider.setPasswordEncoder(passwordEncoder);
//    return authProvider;
//  }

  /**
   * Authentication Manager para manejar el proceso de autenticación.
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }
}