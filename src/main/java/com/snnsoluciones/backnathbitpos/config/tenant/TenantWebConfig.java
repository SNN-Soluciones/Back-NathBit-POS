package com.snnsoluciones.backnathbitpos.config.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * TenantWebConfig - Registra el TenantInterceptor en Spring MVC.
 * 
 * El interceptor se ejecuta para todos los endpoints excepto los públicos.
 */
@Configuration
@RequiredArgsConstructor
public class TenantWebConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
            // Aplicar a todos los endpoints
            .addPathPatterns("/api/**")
            // Excluir endpoints públicos que no necesitan tenant
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/empresa",
                "/api/auth/dispositivo/**",
                "/api/test/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            );
    }
}
