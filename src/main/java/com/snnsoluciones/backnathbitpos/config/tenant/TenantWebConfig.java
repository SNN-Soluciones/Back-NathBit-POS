package com.snnsoluciones.backnathbitpos.config.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class TenantWebConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/empresa",
                "/api/auth/dispositivo/**",

                // ESTOS SON LOS IMPORTANTES - Endpoints PDV nuevos:
                "/api/dispositivos/**",           // ← ESTE
                "/api/auth/login-pdv",            // ← ESTE
                "/api/auth/cambiar-pin",          // ← ESTE
                "/api/auth/generar-pin/**",       // ← ESTE
                "/api/auth/resetear-pin/**",      // ← ESTE
                "/api/asistencia/**",             // ← ESTE
                "/api/admin/dispositivos/**",     // ← ESTE

                "/api/test/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            );
    }
}