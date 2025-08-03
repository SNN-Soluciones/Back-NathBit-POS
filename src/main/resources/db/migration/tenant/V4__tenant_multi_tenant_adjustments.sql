-- V4__tenant_multi_tenant_adjustments.sql
-- Ajustes en schemas de tenant para soportar modelo multi-tenant
-- Este archivo debe ir en src/main/resources/db/migration/tenant/

-- =====================================================
-- MODIFICAR CONSTRAINT DE UNICIDAD EN USUARIOS
-- =====================================================

-- Eliminar constraint antiguo si existe
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_tenant_id_email_key;

-- Crear nuevo índice único solo por email dentro del tenant
-- (el tenant_id ya está implícito en el schema)
CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_email_unique ON usuarios(email);

-- =====================================================
-- AGREGAR COLUMNAS PARA TRACKING MULTI-TENANT
-- =====================================================

-- Agregar columna para tracking de último tenant usado
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_tenant_accedido VARCHAR(50);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_acceso_tenant TIMESTAMP;

-- =====================================================
-- FUNCIÓN PARA SINCRONIZAR ROLES
-- =====================================================

CREATE OR REPLACE FUNCTION sync_user_role_with_tenant()
    RETURNS TRIGGER AS $$
DECLARE
    v_current_tenant VARCHAR(50);
BEGIN
    -- Obtener el tenant actual del schema
    v_current_tenant := current_schema();

    -- Si el rol cambió, actualizar en usuario_tenant
    IF TG_OP = 'UPDATE' AND OLD.rol_id IS DISTINCT FROM NEW.rol_id THEN
        UPDATE public.usuario_tenant
        SET rol_id = NEW.rol_id
        WHERE usuario_id = NEW.id
          AND tenant_id = v_current_tenant;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Crear trigger para sincronizar cambios de rol
DROP TRIGGER IF EXISTS trg_sync_user_role ON usuarios;
CREATE TRIGGER trg_sync_user_role
    AFTER UPDATE OF rol_id ON usuarios
    FOR EACH ROW
EXECUTE FUNCTION sync_user_role_with_tenant();

-- =====================================================
-- VISTA PARA FACILITAR CONSULTAS DE USUARIOS
-- =====================================================

CREATE OR REPLACE VIEW v_usuarios_con_acceso AS
SELECT
    u.*,
    CASE
        WHEN ut.es_propietario THEN 'PROPIETARIO'
        WHEN r.nombre = 'ADMINISTRADOR' THEN 'ADMINISTRADOR'
        WHEN r.nombre = 'CAJERO' THEN 'CAJERO'
        WHEN r.nombre = 'MESERO' THEN 'MESERO'
        ELSE 'USUARIO'
        END as tipo_acceso,
    ut.fecha_acceso as ultimo_acceso_registrado
FROM usuarios u
         LEFT JOIN roles r ON u.rol_id = r.id
         LEFT JOIN public.usuario_tenant ut ON ut.usuario_id = u.id AND ut.tenant_id = current_schema()
WHERE u.activo = true;

-- =====================================================
-- ÍNDICES ADICIONALES PARA PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_usuarios_ultimo_acceso ON usuarios(ultimo_acceso);
CREATE INDEX IF NOT EXISTS idx_audit_events_username_date ON audit_events(username, event_date DESC);

-- =====================================================
-- FUNCIÓN HELPER PARA OBTENER TENANTS DE UN USUARIO
-- =====================================================

CREATE OR REPLACE FUNCTION get_user_tenants(p_user_email VARCHAR(255))
    RETURNS TABLE (
                      tenant_id VARCHAR(50),
                      tenant_nombre VARCHAR(255),
                      tenant_tipo VARCHAR(50),
                      rol_nombre VARCHAR(30),
                      es_propietario BOOLEAN,
                      fecha_acceso TIMESTAMP
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT
            ut.tenant_id,
            ut.tenant_nombre,
            ut.tenant_tipo,
            r.nombre::VARCHAR(30),
            ut.es_propietario,
            ut.fecha_acceso
        FROM public.usuario_tenant ut
                 JOIN usuarios u ON u.id = ut.usuario_id
                 LEFT JOIN roles r ON r.id = ut.rol_id
        WHERE u.email = p_user_email
          AND ut.activo = true
          AND u.tenant_id = current_schema()
        ORDER BY ut.es_propietario DESC, ut.fecha_acceso DESC NULLS LAST;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Otorgar permisos de ejecución
GRANT EXECUTE ON FUNCTION get_user_tenants(VARCHAR) TO PUBLIC;

-- =====================================================
-- COMENTARIOS DE DOCUMENTACIÓN
-- =====================================================

COMMENT ON COLUMN usuarios.ultimo_tenant_accedido IS 'Último tenant al que el usuario accedió';
COMMENT ON COLUMN usuarios.ultimo_acceso_tenant IS 'Timestamp del último acceso a cualquier tenant';
COMMENT ON VIEW v_usuarios_con_acceso IS 'Vista consolidada de usuarios con información de acceso multi-tenant';
COMMENT ON FUNCTION get_user_tenants(VARCHAR) IS 'Obtiene todos los tenants a los que un usuario tiene acceso';