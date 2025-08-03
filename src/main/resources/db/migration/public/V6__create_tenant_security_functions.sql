-- V6__create_tenant_security_functions.sql
-- Funciones de seguridad para validación de acceso multi-tenant
-- Crear en schema public

-- =====================================================
-- FUNCIÓN PARA VALIDAR ACCESO A TENANT
-- =====================================================

CREATE OR REPLACE FUNCTION public.validate_user_tenant_access(
    p_user_id UUID,
    p_tenant_id VARCHAR(50),
    p_required_role VARCHAR(30) DEFAULT NULL
)
    RETURNS BOOLEAN AS $$
DECLARE
    v_has_access BOOLEAN;
    v_user_role VARCHAR(30);
BEGIN
    -- Verificar si el usuario tiene acceso activo al tenant
    SELECT
        true,
        r.nombre
    INTO
        v_has_access,
        v_user_role
    FROM public.usuario_tenant ut
             LEFT JOIN ${p_tenant_id}.roles r ON r.id = ut.rol_id
    WHERE ut.usuario_id = p_user_id
      AND ut.tenant_id = p_tenant_id
      AND ut.activo = true
    LIMIT 1;

    -- Si no tiene acceso, retornar false
    IF NOT COALESCE(v_has_access, false) THEN
        RETURN false;
    END IF;

    -- Si se requiere un rol específico, validarlo
    IF p_required_role IS NOT NULL THEN
        -- Jerarquía de roles: ADMINISTRADOR > CAJERO > MESERO
        CASE p_required_role
            WHEN 'ADMINISTRADOR' THEN
                RETURN v_user_role = 'ADMINISTRADOR';
            WHEN 'CAJERO' THEN
                RETURN v_user_role IN ('ADMINISTRADOR', 'CAJERO');
            WHEN 'MESERO' THEN
                RETURN v_user_role IN ('ADMINISTRADOR', 'CAJERO', 'MESERO');
            ELSE
                RETURN true; -- Si no se reconoce el rol, solo validar acceso
            END CASE;
    END IF;

    RETURN true;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- FUNCIÓN PARA AUDITAR ACCESO CROSS-TENANT
-- =====================================================

CREATE OR REPLACE FUNCTION public.audit_cross_tenant_access(
    p_user_email VARCHAR(255),
    p_from_tenant VARCHAR(50),
    p_to_tenant VARCHAR(50),
    p_action VARCHAR(100),
    p_details JSONB DEFAULT NULL
)
    RETURNS void AS $$
BEGIN
    -- Insertar en tabla de auditoría del tenant destino
    EXECUTE format('
        INSERT INTO %I.audit_events (
            tenant_id, username, event_type, event_date,
            details, success, resource_type
        ) VALUES (
            %L, %L, %L, NOW(),
            %L, true, ''CROSS_TENANT''
        )',
                   p_to_tenant,
                   p_to_tenant, p_user_email, 'CROSS_TENANT_' || p_action,
                   jsonb_build_object(
                           'from_tenant', p_from_tenant,
                           'to_tenant', p_to_tenant,
                           'action', p_action,
                           'additional_details', p_details
                   )::text
            );
EXCEPTION
    WHEN OTHERS THEN
        -- Log error but don't fail the operation
        RAISE WARNING 'Error auditando acceso cross-tenant: %', SQLERRM;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- FUNCIÓN PARA LISTAR USUARIOS CON MULTI-TENANT
-- =====================================================

CREATE OR REPLACE FUNCTION public.get_multi_tenant_users()
    RETURNS TABLE (
                      email VARCHAR(255),
                      nombre_completo TEXT,
                      tenant_count BIGINT,
                      tenants TEXT[],
                      es_propietario_alguno BOOLEAN,
                      ultimo_acceso TIMESTAMP
                  ) AS $$
BEGIN
    RETURN QUERY
        WITH user_tenants AS (
            SELECT
                ut.usuario_id,
                COUNT(*) as tenant_count,
                ARRAY_AGG(
                        ut.tenant_id || ' (' || COALESCE(ut.tenant_nombre, 'Sin nombre') || ')'
                        ORDER BY ut.tenant_id
                ) as tenants,
                BOOL_OR(ut.es_propietario) as es_propietario_alguno,
                MAX(ut.fecha_acceso) as ultimo_acceso
            FROM public.usuario_tenant ut
            WHERE ut.activo = true
            GROUP BY ut.usuario_id
            HAVING COUNT(*) > 1
        )
        SELECT DISTINCT ON (u.email)
            u.email,
            u.nombre || ' ' || COALESCE(u.apellidos, '') as nombre_completo,
            ut.tenant_count,
            ut.tenants,
            ut.es_propietario_alguno,
            ut.ultimo_acceso
        FROM user_tenants ut
                 JOIN public.usuario_tenant ut2 ON ut2.usuario_id = ut.usuario_id
                 JOIN LATERAL (
            SELECT u2.email, u2.nombre, u2.apellidos
            FROM usuarios u2
            WHERE u2.id = ut.usuario_id
            LIMIT 1
            ) u ON true
        ORDER BY u.email, ut.tenant_count DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- POLÍTICAS DE ROW LEVEL SECURITY (RLS)
-- =====================================================

-- Habilitar RLS en la tabla usuario_tenant
ALTER TABLE public.usuario_tenant ENABLE ROW LEVEL SECURITY;

-- Política para que los usuarios solo vean sus propios registros
CREATE POLICY usuario_tenant_self_access ON public.usuario_tenant
    FOR SELECT
    USING (
    usuario_id IN (
        SELECT id FROM usuarios
        WHERE email = current_user
    )
    );

-- Política para administradores del sistema
CREATE POLICY usuario_tenant_admin_access ON public.usuario_tenant
    FOR ALL
    USING (
    EXISTS (
        SELECT 1 FROM usuarios u
                          JOIN roles r ON u.rol_id = r.id
        WHERE u.email = current_user
          AND r.nombre = 'ADMINISTRADOR'
    )
    );

-- =====================================================
-- OTORGAR PERMISOS
-- =====================================================

GRANT EXECUTE ON FUNCTION public.validate_user_tenant_access(UUID, VARCHAR, VARCHAR) TO PUBLIC;
GRANT EXECUTE ON FUNCTION public.audit_cross_tenant_access(VARCHAR, VARCHAR, VARCHAR, VARCHAR, JSONB) TO PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_multi_tenant_users() TO PUBLIC;

-- =====================================================
-- COMENTARIOS DE DOCUMENTACIÓN
-- =====================================================

COMMENT ON FUNCTION public.validate_user_tenant_access IS 'Valida si un usuario tiene acceso a un tenant específico con rol opcional';
COMMENT ON FUNCTION public.audit_cross_tenant_access IS 'Registra accesos entre diferentes tenants para auditoría';
COMMENT ON FUNCTION public.get_multi_tenant_users IS 'Lista usuarios con acceso a múltiples tenants';