package com.snnsoluciones.backnathbitpos.config.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resuelve el identificador del tenant actual para Hibernate.
 * Esta clase es llamada por Hibernate cuando necesita determinar
 * qué schema usar para las operaciones de base de datos.
 */
@Component
@Slf4j
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    private static final String DEFAULT_TENANT = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getCurrentTenant();
        
        if (tenantId != null) {
            log.debug("Resolviendo tenant: {}", tenantId);
            return tenantId;
        }
        
        log.debug("No se encontró tenant en el contexto, usando tenant por defecto: {}", DEFAULT_TENANT);
        return DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // true = valida que las sesiones existentes tengan el mismo tenant
        // false = permite cambiar de tenant en la misma sesión
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        // Registrar este resolver en las propiedades de Hibernate
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}