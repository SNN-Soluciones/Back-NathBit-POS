package com.snnsoluciones.backnathbitpos.config.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * TenantConnectionProvider - Proveedor de conexiones multi-tenant para Hibernate.
 * 
 * Este componente intercepta las conexiones a la base de datos y cambia el
 * search_path de PostgreSQL al schema del tenant actual.
 * 
 * Estrategia: SCHEMA per tenant
 * - Todos los tenants comparten la misma base de datos
 * - Cada tenant tiene su propio schema
 * - El schema 'public' se usa para datos compartidos y sistema legacy
 */
@Component
@Slf4j
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String>, HibernatePropertiesCustomizer {

    @Autowired
    private DataSource dataSource;

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
        log.debug("Obteniendo conexión para tenant: {}", tenantIdentifier);
        
        Connection connection = dataSource.getConnection();
        
        try {
            // Cambiar search_path al schema del tenant
            // Incluimos 'public' para acceder a tablas compartidas (catálogos, etc.)
            String schema = tenantIdentifier != null ? tenantIdentifier : TenantContext.DEFAULT_SCHEMA;
            String searchPath = schema.equals(TenantContext.DEFAULT_SCHEMA) 
                ? "public" 
                : schema + ", public";
            
            connection.createStatement().execute("SET search_path TO " + searchPath);
            log.debug("Search path establecido a: {}", searchPath);
            
        } catch (SQLException e) {
            log.error("Error al establecer search_path para tenant {}: {}", tenantIdentifier, e.getMessage());
            throw e;
        }
        
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Restaurar search_path a public antes de liberar
            connection.createStatement().execute("SET search_path TO public");
        } catch (SQLException e) {
            log.warn("Error al restaurar search_path: {}", e.getMessage());
        }
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
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

    /**
     * Personaliza las propiedades de Hibernate para multi-tenancy
     */
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new TenantIdentifierResolver());
    }
}
