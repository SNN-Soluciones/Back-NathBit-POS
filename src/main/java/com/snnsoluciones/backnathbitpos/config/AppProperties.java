package com.snnsoluciones.backnathbitpos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private String name;
    private String version;
    private String description;
    private List<String> availableTenants;
    
    @Data
    public static class Facturacion {
        private String apiFUrl;
        private Integer timeout;
        private Integer retryAttempts;
    }
    
    private Facturacion facturacion;
}