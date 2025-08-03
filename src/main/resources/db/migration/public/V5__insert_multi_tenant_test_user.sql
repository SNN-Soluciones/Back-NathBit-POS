-- V5__insert_multi_tenant_test_user.sql
-- Crear usuario de prueba con acceso a múltiples tenants

-- =====================================================
-- FUNCIÓN AUXILIAR PARA CREAR USUARIO EN TENANT
-- =====================================================

CREATE OR REPLACE FUNCTION create_user_in_tenant(
    p_tenant_id VARCHAR(50),
    p_email VARCHAR(255),
    p_password VARCHAR(255),
    p_nombre VARCHAR(100),
    p_apellidos VARCHAR(100),
    p_rol_nombre VARCHAR(30)
)
    RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
    v_rol_id UUID;
    v_sql TEXT;
BEGIN
    -- Obtener ID del rol
    v_sql := format('SELECT id FROM %I.roles WHERE nombre = %L AND activo = true LIMIT 1',
                    p_tenant_id, p_rol_nombre);
    EXECUTE v_sql INTO v_rol_id;

    -- Verificar si el usuario ya existe
    v_sql := format('SELECT id FROM %I.usuarios WHERE email = %L', p_tenant_id, p_email);
    EXECUTE v_sql INTO v_user_id;

    IF v_user_id IS NULL THEN
        -- Crear nuevo usuario
        v_user_id := gen_random_uuid();
        v_sql := format('
            INSERT INTO %I.usuarios (id, tenant_id, email, password, nombre, apellidos, rol_id, activo)
            VALUES (%L, %L, %L, %L, %L, %L, %L, true)',
                        p_tenant_id, v_user_id, p_tenant_id, p_email, p_password, p_nombre, p_apellidos, v_rol_id
                 );
        EXECUTE v_sql;

        RAISE NOTICE 'Usuario creado en tenant %: %', p_tenant_id, p_email;
    ELSE
        RAISE NOTICE 'Usuario ya existe en tenant %: %', p_tenant_id, p_email;
    END IF;

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- CREAR USUARIO MULTI-TENANT DE PRUEBA
-- =====================================================

DO $$
    DECLARE
        v_email VARCHAR(255) := 'multi@demo.com';
        v_password VARCHAR(255) := '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqOlnjeRnGxVRCEKUGS5vNyPNy'; -- Password: Admin123!
        v_nombre VARCHAR(100) := 'Usuario';
        v_apellidos VARCHAR(100) := 'Multi-Tenant';
        v_user_id UUID;
        v_tenant_record RECORD;
        v_created_count INTEGER := 0;
    BEGIN
        -- Iterar sobre todos los tenants activos
        FOR v_tenant_record IN
            SELECT id, nombre, tipo
            FROM public.tenants
            WHERE activo = true
            ORDER BY id
            LOOP
                -- Verificar si el schema existe
                IF EXISTS (
                    SELECT 1 FROM information_schema.schemata
                    WHERE schema_name = v_tenant_record.id
                ) THEN
                    BEGIN
                        -- Crear usuario en el tenant
                        v_user_id := create_user_in_tenant(
                                v_tenant_record.id,
                                v_email,
                                v_password,
                                v_nombre,
                                v_apellidos,
                                CASE
                                    WHEN v_created_count = 0 THEN 'ADMINISTRADOR'  -- Admin en el primer tenant
                                    WHEN v_created_count = 1 THEN 'CAJERO'        -- Cajero en el segundo
                                    ELSE 'MESERO'                                 -- Mesero en los demás
                                    END
                                     );

                        -- Insertar o actualizar en usuario_tenant
                        INSERT INTO public.usuario_tenant (
                            usuario_id,
                            tenant_id,
                            tenant_nombre,
                            tenant_tipo,
                            es_propietario,
                            activo
                        ) VALUES (
                                     v_user_id,
                                     v_tenant_record.id,
                                     v_tenant_record.nombre,
                                     COALESCE(v_tenant_record.tipo, 'RESTAURANTE'),
                                     (v_created_count = 0), -- Propietario solo del primer tenant
                                     true
                                 )
                        ON CONFLICT (usuario_id, tenant_id)
                            DO UPDATE SET
                                          tenant_nombre = EXCLUDED.tenant_nombre,
                                          tenant_tipo = EXCLUDED.tenant_tipo,
                                          activo = EXCLUDED.activo;

                        v_created_count := v_created_count + 1;

                    EXCEPTION
                        WHEN OTHERS THEN
                            RAISE WARNING 'Error creando usuario en tenant %: %', v_tenant_record.id, SQLERRM;
                    END;
                END IF;
            END LOOP;

        RAISE NOTICE 'Usuario multi-tenant creado en % tenants', v_created_count;
        RAISE NOTICE 'Email: %, Password: Admin123!', v_email;

    END $$;

-- =====================================================
-- CREAR TENANT ADICIONAL DE PRUEBA (OPCIONAL)
-- =====================================================

-- Si solo existe 'demo', crear un segundo tenant para pruebas
INSERT INTO public.tenants (id, nombre, tipo, configuracion, activo)
SELECT 'facturacion', 'Sistema de Facturación', 'FACTURACION',
       '{"theme": "blue", "currency": "CRC"}'::jsonb, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.tenants WHERE id = 'facturacion'
);

-- =====================================================
-- LIMPIAR FUNCIÓN TEMPORAL
-- =====================================================

DROP FUNCTION IF EXISTS create_user_in_tenant(VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR);

-- =====================================================
-- MOSTRAR RESUMEN
-- =====================================================

DO $$
    BEGIN
        RAISE NOTICE '=== RESUMEN DE USUARIOS MULTI-TENANT ===';

        FOR summary IN
            SELECT
                ut.usuario_id,
                COUNT(*) as tenant_count,
                STRING_AGG(ut.tenant_id || ' (' || ut.tenant_nombre || ')', ', ' ORDER BY ut.tenant_id) as tenants
            FROM public.usuario_tenant ut
            WHERE ut.activo = true
            GROUP BY ut.usuario_id
            HAVING COUNT(*) > 1
            LOOP
                RAISE NOTICE 'Usuario % tiene acceso a % tenants: %',
                    summary.usuario_id, summary.tenant_count, summary.tenants;
            END LOOP;
    END $$;