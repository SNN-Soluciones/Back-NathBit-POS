package com.snnsoluciones.backnathbitpos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController implements HealthIndicator {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @GetMapping("/actuator/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Verificar conexión a BD
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("status", "UP");
            health.put("database", "Connected");
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("database", "Disconnected");
            health.put("error", e.getMessage());
        }
        
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "nathbit-pos-backend");
        
        return health;
    }
    
    @Override
    public Health health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}