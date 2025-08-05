-- =====================================================
-- MIGRACIÓN V13: Preparación de Datos para Multi-Empresa
-- Archivo: src/main/resources/db/migration/public/V8__prepare_migration_data.sql
-- =====================================================

-- 1. Crear empresa por defecto si no existe
INSERT INTO public.empresas (
    id,
    codigo,
    nombre,
    nombre_comercial,
    tipo,
    activa,
    created_at,
    updated_at
)
SELECT
    '00000000-0000-0000-0000-000000000001'::UUID,
    'DEFAULT',
    'Empresa Por Defecto',
    'Mi Restaurante',
    'RESTAURANTE',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM public.empresas WHERE codigo = 'DEFAULT'
);

-- 2. Crear función para migrar usuarios de cada tenant
CREATE OR REPLACE FUNCTION public.migrate_users_from_tenant(
    p_tenant_schema TEXT,
    p_empresa_id UUID DEFAULT '00000000-0000-0000-0000-000000000001'::UUID
) RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER := 0;
    v_sql TEXT;
    v_sucursal_id UUID;
    v_role_sql TEXT;
BEGIN
    -- Verificar si el tenant existe
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.schemata
        WHERE schema_name = p_tenant_schema
    ) THEN
        RAISE NOTICE 'Schema % no existe', p_tenant_schema;
        RETURN 0;
    END IF;

    -- Verificar si ya existe la sucursal
    SELECT id INTO v_sucursal_id
    FROM public.empresas_sucursales
    WHERE schema_name = p_tenant_schema;

    IF v_sucursal_id IS NULL THEN
        -- Crear sucursal para este tenant
        INSERT INTO public.empresas_sucursales (
            empresa_id,
            codigo_sucursal,
            nombre_sucursal,
            schema_name,
            activa,
            es_principal
        ) VALUES (
                     p_empresa_id,
                     UPPER(p_tenant_schema),
                     'Sucursal ' || p_tenant_schema,
                     p_tenant_schema,
                     true,
                     false
                 ) RETURNING id INTO v_sucursal_id;
    END IF;

    -- Migrar usuarios
    v_sql := format($MIGRATE$
        WITH usuarios_migrados AS (
            INSERT INTO public.usuarios_global (
                email,
                password,
                nombre,
                apellidos,
                telefono,
                identificacion,
                tipo_identificacion,
                activo,
                bloqueado,
                intentos_fallidos,
                created_at,
                updated_at
            )
            SELECT
                u.email,
                u.password,
                u.nombre,
                u.apellidos,
                u.telefono,
                u.identificacion,
                u.tipo_identificacion::text,
                u.activo,
                u.bloqueado,
                u.intentos_fallidos,
                COALESCE(u.created_at, CURRENT_TIMESTAMP),
                CURRENT_TIMESTAMP
            FROM %I.usuarios u
            WHERE NOT EXISTS (
                SELECT 1 FROM public.usuarios_global ug
                WHERE ug.email = u.email
            )
            RETURNING id, email
        )
        SELECT COUNT(*) FROM usuarios_migrados
    $MIGRATE$, p_tenant_schema);

    EXECUTE v_sql INTO v_count;

    -- Obtener roles del tenant
    v_role_sql := format($ROLE$
        INSERT INTO public.usuario_empresas (
            usuario_id,
            empresa_id,
            rol,
            activo,
            fecha_asignacion
        )
        SELECT
            ug.id,
            %L::UUID,
            COALESCE(
                (SELECT r.nombre::text FROM %I.roles r WHERE r.id = u.rol_id LIMIT 1),
                'CAJERO'
            ),
            true,
            CURRENT_TIMESTAMP
        FROM %I.usuarios u
        JOIN public.usuarios_global ug ON ug.email = u.email
        WHERE NOT EXISTS (
            SELECT 1 FROM public.usuario_empresas ue
            WHERE ue.usuario_id = ug.id AND ue.empresa_id = %L::UUID
        )
    $ROLE$, p_empresa_id, p_tenant_schema, p_tenant_schema, p_empresa_id);

    EXECUTE v_role_sql;

    -- Asignar permisos en sucursal
    v_sql := format($PERMS$
        INSERT INTO public.usuario_sucursales (
            usuario_id,
            empresa_id,
            sucursal_id,
            puede_leer,
            puede_escribir,
            puede_eliminar,
            puede_aprobar,
            es_principal,
            activo
        )
        SELECT
            ug.id,
            %L::UUID,
            %L::UUID,
            true,
            true,
            CASE
                WHEN COALESCE(r.nombre::text, 'CAJERO') IN ('ADMIN', 'SUPER_ADMIN')
                THEN true
                ELSE false
            END,
            CASE
                WHEN COALESCE(r.nombre::text, 'CAJERO') IN ('ADMIN', 'SUPER_ADMIN', 'JEFE_CAJAS')
                THEN true
                ELSE false
            END,
            true,
            true
        FROM %I.usuarios u
        JOIN public.usuarios_global ug ON ug.email = u.email
        LEFT JOIN %I.roles r ON r.id = u.rol_id
        WHERE NOT EXISTS (
            SELECT 1 FROM public.usuario_sucursales us
            WHERE us.usuario_id = ug.id AND us.sucursal_id = %L::UUID
        )
    $PERMS$, p_empresa_id, v_sucursal_id, p_tenant_schema, p_tenant_schema, v_sucursal_id);

    EXECUTE v_sql;

    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- 3. Log de migración
CREATE TABLE IF NOT EXISTS public.migration_log (
                                                    id SERIAL PRIMARY KEY,
                                                    migration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                    tenant_schema TEXT,
                                                    usuarios_migrados INTEGER,
                                                    success BOOLEAN,
                                                    error_message TEXT
);

-- 4. Ejecutar migración automáticamente para todos los tenants
DO $$
    DECLARE
        v_tenant RECORD;
        v_count INTEGER;
        v_total INTEGER := 0;
    BEGIN
        -- Migrar usuarios de cada tenant
        FOR v_tenant IN
            SELECT schema_name
            FROM information_schema.schemata
            WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'public')
              AND EXISTS (
                SELECT 1 FROM information_schema.tables
                WHERE table_schema = schema_name
                  AND table_name = 'usuarios'
            )
            LOOP
                BEGIN
                    v_count := public.migrate_users_from_tenant(v_tenant.schema_name);
                    v_total := v_total + v_count;

                    INSERT INTO public.migration_log (
                        tenant_schema,
                        usuarios_migrados,
                        success
                    ) VALUES (
                                 v_tenant.schema_name,
                                 v_count,
                                 true
                             );

                    RAISE NOTICE 'Migrados % usuarios de %', v_count, v_tenant.schema_name;
                EXCEPTION WHEN OTHERS THEN
                    INSERT INTO public.migration_log (
                        tenant_schema,
                        usuarios_migrados,
                        success,
                        error_message
                    ) VALUES (
                                 v_tenant.schema_name,
                                 0,
                                 false,
                                 SQLERRM
                             );
                    RAISE WARNING 'Error migrando %: %', v_tenant.schema_name, SQLERRM;
                END;
            END LOOP;

        RAISE NOTICE 'Total usuarios migrados: %', v_total;
    END $$;