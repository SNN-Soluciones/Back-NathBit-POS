package com.snnsoluciones.backnathbitpos.config;

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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter authenticationFilter;
    
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
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize -> authorize
                // Públicos
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()

                // Dashboard Sistema - Solo ROOT y SOPORTE
                .requestMatchers("/api/sistema/**").hasAnyRole("ROOT", "SOPORTE")

                // Gestión de empresas - ROOT, SOPORTE y SUPER_ADMIN
                .requestMatchers("/api/empresas/**").hasAnyRole("ROOT", "SOPORTE", "SUPER_ADMIN")

                // Gestión de sucursales - ROOT, SOPORTE, SUPER_ADMIN y ADMIN
                .requestMatchers("/api/sucursales/**").hasAnyRole("ROOT", "SOPORTE", "SUPER_ADMIN", "ADMIN")

                // Sistema operativo (POS, órdenes, etc.) - TODOS los autenticados
                .requestMatchers("/api/pos/**").authenticated()
                .requestMatchers("/api/ordenes/**").authenticated()
                .requestMatchers("/api/productos/**").authenticated()

                .anyRequest().authenticated()
            );
        // Agregar filtro JWT
        http.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}