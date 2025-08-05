-- V3__migrate_existing_users_to_multi_tenant.sql
-- Migrar usuarios existentes al nuevo modelo multi-tenant

-- =====================================================
-- FUNCIÓN PARA MIGRAR USUARIOS DE UN TENANT
-- =====================================================

CREATE OR REPLACE FUNCTION migrate_tenant_users(p_tenant_id VARCHAR(50), p_tenant_nombre VARCHAR(255), p_tenant_tipo VARCHAR(50))
    RETURNS void AS $$
DECLARE
    v_sql TEXT;
    v_user_count INTEGER;
BEGIN
    -- Construir query dinámico para obtener usuarios del schema tenant
    v_sql := format('
        INSERT INTO public.usuario_tenant (usuario_id, tenant_id, tenant_nombre, tenant_tipo, rol_id, es_propietario, activo)
        SELECT
            u.id,
            %L,
            %L,
            %L,
            u.rol_id,
            CASE
                WHEN r.nombre = ''ADMINISTRADOR'' THEN true
                ELSE false
            END,
            u.activo
        FROM %I.usuarios u
        LEFT JOIN %I.roles r ON u.rol_id = r.id
        WHERE NOT EXISTS (
            SELECT 1 FROM public.usuario_tenant ut
            WHERE ut.usuario_id = u.id AND ut.tenant_id = %L
        )',
                    p_tenant_id, p_tenant_nombre, p_tenant_tipo,
                    p_tenant_id, p_tenant_id, p_tenant_id
             );

    EXECUTE v_sql;

    -- Contar usuarios migrados
    EXECUTE format('SELECT COUNT(*) FROM %I.usuarios', p_tenant_id) INTO v_user_count;

    RAISE NOTICE 'Migrados % usuarios del tenant %', v_user_count, p_tenant_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING 'Error migrando usuarios del tenant %: %', p_tenant_id, SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- MIGRAR USUARIOS DE TODOS LOS TENANTS EXISTENTES
-- =====================================================

DO $$
    DECLARE
        tenant_record RECORD;
        v_tenant_count INTEGER := 0;
    BEGIN
        -- Obtener todos los tenants de la tabla public.tenants
        FOR tenant_record IN
            SELECT id, nombre, tipo
            FROM public.tenants
            WHERE activo = true
            LOOP
                -- Verificar si el schema existe
                IF EXISTS (
                    SELECT 1 FROM information_schema.schemata
                    WHERE schema_name = tenant_record.id
                ) THEN
                    -- Migrar usuarios del tenant
                    PERFORM migrate_tenant_users(
                            tenant_record.id,
                            tenant_record.nombre,
                            COALESCE(tenant_record.tipo, 'RESTAURANTE')
                            );
                    v_tenant_count := v_tenant_count + 1;
                END IF;
            END LOOP;

        RAISE NOTICE 'Migración completada. Procesados % tenants', v_tenant_count;
    END $$;

-- =====================================================
-- LIMPIAR FUNCIÓN TEMPORAL
-- =====================================================

DROP FUNCTION IF EXISTS migrate_tenant_users(VARCHAR, VARCHAR, VARCHAR);

-- =====================================================
-- VERIFICACIÓN POST-MIGRACIÓN
-- =====================================================

DO $$
    DECLARE
        v_total_usuarios INTEGER;
        v_usuarios_migrados INTEGER;
    BEGIN
        -- Contar total de registros en usuario_tenant
        SELECT COUNT(*) INTO v_usuarios_migrados FROM public.usuario_tenant;

        RAISE NOTICE 'Total de relaciones usuario-tenant creadas: %', v_usuarios_migrados;

        -- Mostrar resumen por tenant
        FOR tenant_record IN
            SELECT tenant_id, COUNT(*) as usuario_count
            FROM public.usuario_tenant
            GROUP BY tenant_id
            LOOP
                RAISE NOTICE 'Tenant %: % usuarios', tenant_record.tenant_id, tenant_record.usuario_count;
            END LOOP;
    END $$;