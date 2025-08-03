package com.snnsoluciones.backnathbitpos.config.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Proveedor de conexiones multi-tenant para Hibernate.
 * Gestiona las conexiones a la base de datos cambiando el schema
 * según el tenant actual.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaBasedMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String>, HibernatePropertiesCustomizer {

    private final DataSource dataSource;
    private static final String DEFAULT_SCHEMA = "public";

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        final Connection connection = getAnyConnection();
        
        try {
            // Cambiar al schema del tenant
            String schema = (tenantIdentifier != null && !tenantIdentifier.isEmpty()) 
                ? tenantIdentifier.toLowerCase() 
                : DEFAULT_SCHEMA;
                
            log.debug("Cambiando al schema: {} para tenant: {}", schema, tenantIdentifier);
            
            // PostgreSQL usa SET search_path para cambiar el schema
            connection.createStatement().execute(String.format("SET search_path TO %s", schema));
            
        } catch (SQLException e) {
            log.error("Error al cambiar al schema del tenant: {}", tenantIdentifier, e);
            throw new SQLException("No se pudo cambiar al schema: " + tenantIdentifier, e);
        }
        
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Resetear al schema por defecto antes de liberar la conexión
            connection.createStatement().execute(String.format("SET search_path TO %s", DEFAULT_SCHEMA));
        } catch (SQLException e) {
            log.warn("Error al resetear el schema al liberar la conexión", e);
        }
        
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // false = mantiene la conexión durante toda la transacción
        // true = libera la conexión después de cada statement
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        // Registrar este provider en las propiedades de Hibernate
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}