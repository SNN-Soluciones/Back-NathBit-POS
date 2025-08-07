package com.snnsoluciones.backnathbitpos.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración CORS para permitir peticiones desde el frontend.
 */
@Configuration
public class CorsConfig {

  @Bean
  @Qualifier("customCors")
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Orígenes permitidos
    configuration.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:*",
        "https://localhost:*",
        "http://127.0.0.1:*",
        "https://*.snnsoluciones.com",
        "https://*.nathbit.com"
    ));

    // Métodos HTTP permitidos
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
    ));

    // Headers permitidos
    configuration.setAllowedHeaders(List.of("*"));

    // Headers expuestos al cliente
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Total-Count",
        "X-Page-Number",
        "X-Page-Size",
        "X-Request-ID"
    ));

    // Permitir credenciales (cookies, authorization headers)
    configuration.setAllowCredentials(true);

    // Tiempo de cache para preflight requests (1 hora)
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}