package com.snnsoluciones.backnathbitpos.config.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * TenantContext - Mantiene el contexto del tenant actual usando ThreadLocal.
 * 
 * Este componente es fundamental para el sistema multi-tenant:
 * - Almacena el schema del tenant actual en el hilo de ejecución
 * - Permite que los repositorios sepan a qué schema conectarse
 * - Debe limpiarse al final de cada request para evitar memory leaks
 * 
 * Uso típico:
 * 1. TenantInterceptor extrae el tenant del request y llama setCurrentTenant()
 * 2. TenantConnectionProvider lee getCurrentTenant() para cambiar el schema
 * 3. Al finalizar el request, se llama clear()
 */
@Slf4j
public class TenantContext {

    /**
     * ThreadLocal que almacena el schema del tenant actual
     */
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * ThreadLocal que almacena el ID del tenant actual
     */
    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

    /**
     * Schema por defecto (public) para cuando no hay tenant definido
     */
    public static final String DEFAULT_SCHEMA = "public";

    /**
     * Prefijo de los schemas de tenant
     */
    public static final String TENANT_SCHEMA_PREFIX = "tenant_";

    // ==================== Getters/Setters para Schema ====================

    /**
     * Establece el schema del tenant actual
     * @param schemaName Nombre del schema (ej: "tenant_inversiones_jr")
     */
    public static void setCurrentTenant(String schemaName) {
        log.debug("Estableciendo tenant schema: {}", schemaName);
        CURRENT_TENANT.set(schemaName);
    }

    /**
     * Obtiene el schema del tenant actual
     * @return Nombre del schema o DEFAULT_SCHEMA si no hay tenant
     */
    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null) {
            return DEFAULT_SCHEMA;
        }
        return tenant;
    }

    /**
     * Verifica si hay un tenant establecido
     */
    public static boolean hasTenant() {
        String tenant = CURRENT_TENANT.get();
        return tenant != null && !tenant.equals(DEFAULT_SCHEMA);
    }

    // ==================== Getters/Setters para ID ====================

    /**
     * Establece el ID del tenant actual
     * @param tenantId ID del tenant
     */
    public static void setCurrentTenantId(Long tenantId) {
        log.debug("Estableciendo tenant ID: {}", tenantId);
        CURRENT_TENANT_ID.set(tenantId);
    }

    /**
     * Obtiene el ID del tenant actual
     * @return ID del tenant o null si no hay tenant
     */
    public static Long getCurrentTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    // ==================== Métodos de conveniencia ====================

    /**
     * Establece tanto el schema como el ID del tenant
     * @param tenantId ID del tenant
     * @param schemaName Nombre del schema
     */
    public static void setTenant(Long tenantId, String schemaName) {
        setCurrentTenantId(tenantId);
        setCurrentTenant(schemaName);
    }

    /**
     * Establece el tenant usando el código
     * Genera el nombre del schema automáticamente
     * @param tenantId ID del tenant
     * @param codigo Código del tenant (ej: "inversiones_jr")
     */
    public static void setTenantByCodigo(Long tenantId, String codigo) {
        setCurrentTenantId(tenantId);
        setCurrentTenant(TENANT_SCHEMA_PREFIX + codigo);
    }

    // ==================== Limpieza ====================

    /**
     * Limpia el contexto del tenant actual
     * IMPORTANTE: Llamar al final de cada request para evitar memory leaks
     */
    public static void clear() {
        log.debug("Limpiando contexto de tenant");
        CURRENT_TENANT.remove();
        CURRENT_TENANT_ID.remove();
    }

    // ==================== Validación ====================

    /**
     * Verifica si el schema es válido (tiene formato de tenant)
     */
    public static boolean isValidTenantSchema(String schema) {
        return schema != null && schema.startsWith(TENANT_SCHEMA_PREFIX);
    }

    /**
     * Extrae el código del tenant desde el nombre del schema
     * @param schemaName Nombre del schema (ej: "tenant_inversiones_jr")
     * @return Código del tenant (ej: "inversiones_jr") o null si no es válido
     */
    public static String extractCodigoFromSchema(String schemaName) {
        if (schemaName == null || !schemaName.startsWith(TENANT_SCHEMA_PREFIX)) {
            return null;
        }
        return schemaName.substring(TENANT_SCHEMA_PREFIX.length());
    }

    /**
     * Genera el nombre del schema a partir del código
     * @param codigo Código del tenant (ej: "inversiones_jr")
     * @return Nombre del schema (ej: "tenant_inversiones_jr")
     */
    public static String generateSchemaName(String codigo) {
        if (codigo == null) {
            return DEFAULT_SCHEMA;
        }
        return TENANT_SCHEMA_PREFIX + codigo.toLowerCase();
    }
}
