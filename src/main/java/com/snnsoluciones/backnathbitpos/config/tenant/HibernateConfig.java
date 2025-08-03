package com.snnsoluciones.backnathbitpos.config.tenant;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de Hibernate para multi-tenancy.
 * Configura el EntityManagerFactory con las propiedades necesarias
 * para soportar múltiples schemas.
 */
@Configuration
@RequiredArgsConstructor
public class HibernateConfig {

    private final DataSource dataSource;
    private final JpaProperties jpaProperties;
    private final MultiTenantConnectionProvider<String> multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.snnsoluciones.backnathbitpos.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        
        // Configurar propiedades de JPA/Hibernate
        Map<String, Object> properties = new HashMap<>(jpaProperties.getProperties());
        
        // Configuración multi-tenant
        properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        
        // IMPORTANTE: Para PostgreSQL usamos SCHEMA strategy
        properties.put("hibernate.multiTenancy", "SCHEMA");
        
        // Configuraciones adicionales recomendadas
        properties.put(AvailableSettings.FORMAT_SQL, true);
        properties.put(AvailableSettings.SHOW_SQL, false); // En producción debe ser false
        
        em.setJpaPropertyMap(properties);
        
        return em;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}