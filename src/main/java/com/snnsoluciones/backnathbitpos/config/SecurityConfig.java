package com.snnsoluciones.backnathbitpos.config;

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
                    "/api/auth/login",
                    "/nathbit/api/auth/login",
                    "/api/auth/refresh",
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
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",      // Angular desarrollo
            "http://localhost:4201",      // Angular testing
            "http://localhost:8080",
            "http://127.0.0.1:4200",
            "http://192.168.0.79:4200",
            "https://nathbit.com",
            "https://www.nathbit.com",
            "http://45.32.164.243:4200"// Por si pruebas desde otro puerto
        ));

        // Métodos HTTP permitidos
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
            "Access-Control-Request-Headers"
        ));

        // Headers expuestos (que el cliente puede leer)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
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