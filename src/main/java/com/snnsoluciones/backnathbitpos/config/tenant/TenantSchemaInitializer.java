package com.snnsoluciones.backnathbitpos.config.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Inicializador de schemas de tenant.
 * Se ejecuta al inicio de la aplicación para crear los schemas necesarios
 * antes de que Flyway ejecute las migraciones.
 */
@Component
@Order(1) // Ejecutar antes que otras inicializaciones
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaInitializer implements CommandLineRunner {

    private final DataSource dataSource;
    
    @Value("${app.available-tenants}")
    private List<String> availableTenants;

    @Override
    public void run(String... args) throws Exception {
        log.info("Inicializando schemas de tenant...");
        
        try (Connection connection = dataSource.getConnection()) {
            Set<String> existingSchemas = getExistingSchemas(connection);
            
            for (String tenant : availableTenants) {
                if (!existingSchemas.contains(tenant.toLowerCase())) {
                    createSchema(connection, tenant);
                } else {
                    log.info("Schema '{}' ya existe", tenant);
                }
            }
            
            log.info("Inicialización de schemas completada");
            
        } catch (Exception e) {
            log.error("Error al inicializar schemas de tenant", e);
            throw new RuntimeException("Error en inicialización de tenant schemas", e);
        }
    }
    
    private Set<String> getExistingSchemas(Connection connection) throws Exception {
        Set<String> schemas = new HashSet<>();
        
        String query = "SELECT schema_name FROM information_schema.schemata";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                schemas.add(rs.getString("schema_name").toLowerCase());
            }
        }
        
        return schemas;
    }
    
    private void createSchema(Connection connection, String schemaName) throws Exception {
        String sql = String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName.toLowerCase());
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            log.info("Schema '{}' creado exitosamente", schemaName);
            
            // Otorgar permisos al usuario de la aplicación
            String grantSql = String.format("GRANT ALL ON SCHEMA %s TO CURRENT_USER", schemaName.toLowerCase());
            stmt.execute(grantSql);
        }
    }
}