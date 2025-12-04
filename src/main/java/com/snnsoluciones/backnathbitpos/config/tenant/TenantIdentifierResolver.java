package com.snnsoluciones.backnathbitpos.config.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * TenantIdentifierResolver - Resuelve el identificador del tenant actual.
 * 
 * Hibernate llama a este componente para saber qué tenant usar en cada operación.
 * Lee el valor del TenantContext (ThreadLocal).
 */
@Component
@Slf4j
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String currentTenant = TenantContext.getCurrentTenant();
        log.trace("Resolviendo tenant actual: {}", currentTenant);
        return currentTenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // No validar sesiones existentes, permite cambiar de tenant
        return false;
    }
}
