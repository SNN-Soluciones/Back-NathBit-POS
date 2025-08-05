-- =====================================================
-- MIGRACIÓN V12: Limpieza de Estructuras Obsoletas
-- Archivo: src/main/resources/db/migration/public/V7__cleanup_obsolete_structures.sql
-- =====================================================

-- 1. Guardar datos importantes antes de eliminar
CREATE TABLE IF NOT EXISTS public.usuario_tenant_backup AS
SELECT * FROM public.usuario_tenant;

-- 2. Eliminar funciones obsoletas
DROP FUNCTION IF EXISTS public.validate_user_tenant_access CASCADE;
DROP FUNCTION IF EXISTS public.audit_cross_tenant_access CASCADE;
DROP FUNCTION IF EXISTS public.get_multi_tenant_users CASCADE;

-- 3. Eliminar políticas RLS obsoletas
DROP POLICY IF EXISTS usuario_tenant_self_access ON public.usuario_tenant;
DROP POLICY IF EXISTS usuario_tenant_admin_access ON public.usuario_tenant;

-- 4. Eliminar tabla usuario_tenant (después de backup)
DROP TABLE IF EXISTS public.usuario_tenant CASCADE;

-- 5. Crear función helper para la migración
CREATE OR REPLACE FUNCTION public.get_tenant_users()
    RETURNS TABLE (
                      tenant_schema TEXT,
                      user_count BIGINT
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT
            s.schema_name::TEXT,
            COUNT(DISTINCT u.id)::BIGINT
        FROM information_schema.schemata s
                 CROSS JOIN LATERAL (
            SELECT 1 as id
            FROM information_schema.tables t
            WHERE t.table_schema = s.schema_name
              AND t.table_name = 'usuarios'
            ) u
        WHERE s.schema_name NOT IN ('pg_catalog', 'information_schema', 'public')
        GROUP BY s.schema_name;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.get_tenant_users() IS 'Función temporal para ayudar en la migración de usuarios';