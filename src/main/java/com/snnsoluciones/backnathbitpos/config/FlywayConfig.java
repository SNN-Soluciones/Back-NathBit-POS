package com.snnsoluciones.backnathbitpos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Configuración de Flyway para manejar migraciones multi-tenant.
 * Ejecuta las migraciones en el schema public y luego en cada schema de tenant.
 */
@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
@RequiredArgsConstructor
@Slf4j
public class FlywayConfig {

    private final FlywayProperties flywayProperties;
    
    @Value("${app.available-tenants}")
    private List<String> availableTenants;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        // Primero ejecutar migraciones en el schema public (schema base)
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .schemas("public")
            .locations("classpath:db/migration/public")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load();
        
        log.info("Ejecutando migraciones Flyway para schema: public");
        flyway.migrate();
        
        // Luego ejecutar migraciones para cada tenant
        for (String tenant : availableTenants) {
            migrateTenantsSchema(dataSource, tenant);
        }
        
        return flyway;
    }
    
    private void migrateTenantsSchema(DataSource dataSource, String schema) {
        try {
            log.info("Ejecutando migraciones Flyway para tenant schema: {}", schema);
            
            Flyway tenantFlyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .createSchemas(true) // Crear el schema si no existe
                .load();
            
            tenantFlyway.migrate();
            
            log.info("Migraciones completadas para schema: {}", schema);
            
        } catch (Exception e) {
            log.error("Error al migrar schema del tenant: {}", schema, e);
            throw new RuntimeException("Error en migración de tenant: " + schema, e);
        }
    }
}