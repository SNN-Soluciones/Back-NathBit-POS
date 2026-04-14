package com.snnsoluciones.backnathbitpos.config;

import com.snnsoluciones.backnathbitpos.security.ApiKeyAuthenticationFilter;
import com.snnsoluciones.backnathbitpos.security.ContextHeaderFilter;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtAuthenticationEntryPoint;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
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

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter authenticationFilter;
    private final ContextHeaderFilter contextHeaderFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Habilitar CORS con nuestra configuración personalizada
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Deshabilitar CSRF ya que usamos JWT
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Configurar manejo de excepciones
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            // Configurar sesiones como stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Configurar autorización de endpoints
            .authorizeHttpRequests(authz -> authz
                // Endpoints públicos
                .requestMatchers(
                    // Auth legacy
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/nathbit/api/auth/login",
                    "/api/dispositivos/registrar",
                    "/api/auth/login-pdv",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/dispositivos/usuarios",

                    // Auth multi-tenant (NUEVOS)
                    "/api/auth/global/login",
                    "/api/auth/empresa",
                    "/api/auth/dispositivo/solicitar",
                    "/api/auth/empresa/*/sucursales",
                    "/api/auth/dispositivo/verificar",
                    "/api/auth/dispositivo/reenviar",
                    "/api/auth/dispositivo/usuarios",  // <-- AGREGAR
                    "/api/auth/pin",

                    // Otros públicos
                    "/api/facturas-recepcion/procesar-email",
                    "/api/empresas/*/identificacion",
                    "/api/test/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // Endpoints de sistema - Solo ROOT y SOPORTE
                .requestMatchers("/api/sistema/**")
                .hasAnyRole("ROOT", "SOPORTE")
                // Endpoints empresariales - Solo SUPER_ADMIN
                .requestMatchers("/api/empresarial/**")
                .hasRole("SUPER_ADMIN")
                // El resto requiere autenticación
                .anyRequest().authenticated()
            );

        // Agregar filtro JWT antes del filtro de autenticación
        http.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(contextHeaderFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    // En una @Configuration o donde definas beans
    @Bean
    public RestTemplate haciendaRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Headers permitidos
        configuration.setAllowedHeaders(List.of("*"));

        // Headers expuestos (que el cliente puede leer)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Content-Disposition"
        ));

        // Permitir credenciales (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Tiempo de caché para las respuestas preflight
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}