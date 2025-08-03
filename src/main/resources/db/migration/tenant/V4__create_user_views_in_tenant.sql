-- =====================================================
-- MIGRACIÓN: Vistas de Usuarios en Schema Tenant
-- Archivo: V4__create_user_views_in_tenant.sql
-- Ubicación: src/main/resources/db/migration/tenant/
-- =====================================================

-- Esta migración se ejecuta en CADA schema de tenant (sucursal)
-- Crea vistas que permiten acceder a los usuarios globales
-- filtrando solo los que tienen acceso a esta sucursal

-- 1. Vista de Usuarios con acceso a esta sucursal
CREATE OR REPLACE VIEW usuarios AS
SELECT
    ug.id,
    ug.email,
    ug.password,
    ug.nombre,
    ug.apellidos,
    ug.telefono,
    ug.identificacion,
    ug.tipo_identificacion,
    ug.activo,
    ug.bloqueado,
    ug.intentos_fallidos,
    ug.ultimo_acceso,
    ue.rol,
    ue.es_propietario,
    us.puede_leer,
    us.puede_escribir,
    us.puede_eliminar,
    us.puede_aprobar,
    us.es_principal as es_sucursal_principal,
    current_schema() as tenant_id,
    es.id as sucursal_id,
    es.empresa_id,
    e.nombre as empresa_nombre,
    e.codigo as empresa_codigo,
    es.nombre_sucursal,
    es.codigo_sucursal
FROM public.usuarios_global ug
         INNER JOIN public.usuario_empresas ue ON ug.id = ue.usuario_id
         INNER JOIN public.empresas e ON ue.empresa_id = e.id
         INNER JOIN public.empresas_sucursales es ON es.empresa_id = e.id
         INNER JOIN public.usuario_sucursales us ON us.usuario_id = ug.id AND us.sucursal_id = es.id
WHERE es.schema_name = current_schema()
  AND ue.activo = true
  AND us.activo = true
  AND ug.activo = true;

-- 2. Vista simplificada para autenticación
CREATE OR REPLACE VIEW usuarios_auth AS
SELECT
    u.id,
    u.email,
    u.password,
    u.activo,
    u.bloqueado,
    u.rol,
    u.tenant_id,
    u.sucursal_id,
    u.empresa_id
FROM usuarios u;

-- 3. Vista de permisos detallados
CREATE OR REPLACE VIEW usuario_permisos AS
SELECT
    u.id as usuario_id,
    u.email,
    u.rol,
    u.puede_leer,
    u.puede_escribir,
    u.puede_eliminar,
    u.puede_aprobar,
    -- Permisos especiales basados en rol
    CASE
        WHEN u.rol IN ('SUPER_ADMIN', 'ADMIN') THEN true
        ELSE false
        END as es_administrador,
    CASE
        WHEN u.rol IN ('SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS') THEN true
        ELSE false
        END as puede_ver_reportes,
    CASE
        WHEN u.rol IN ('SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO') THEN true
        ELSE false
        END as puede_cobrar,
    CASE
        WHEN u.rol IN ('SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO') THEN true
        ELSE false
        END as puede_tomar_ordenes
FROM usuarios u;

-- 4. Función helper para verificar permisos
CREATE OR REPLACE FUNCTION tiene_permiso(
    p_usuario_id UUID,
    p_accion VARCHAR
) RETURNS BOOLEAN AS $$
DECLARE
    v_puede BOOLEAN;
BEGIN
    SELECT CASE p_accion
               WHEN 'LEER' THEN puede_leer
               WHEN 'ESCRIBIR' THEN puede_escribir
               WHEN 'ELIMINAR' THEN puede_eliminar
               WHEN 'APROBAR' THEN puede_aprobar
               WHEN 'VER_REPORTES' THEN puede_ver_reportes
               WHEN 'COBRAR' THEN puede_cobrar
               WHEN 'TOMAR_ORDENES' THEN puede_tomar_ordenes
               ELSE false
               END INTO v_puede
    FROM usuario_permisos
    WHERE usuario_id = p_usuario_id;

    RETURN COALESCE(v_puede, false);
END;
$$ LANGUAGE plpgsql;

-- 5. Vista de configuración de acceso para esta sucursal
CREATE OR REPLACE VIEW configuracion_acceso_local AS
SELECT
    ca.*,
    es.nombre_sucursal,
    es.codigo_sucursal
FROM public.configuracion_acceso ca
         JOIN public.empresas_sucursales es ON ca.sucursal_id = es.id
WHERE es.schema_name = current_schema()
  AND ca.activo = true
ORDER BY ca.prioridad DESC;

-- 6. Índices en las vistas materializadas (si se necesita performance)
-- NOTA: Descomentar si se convierten en vistas materializadas
-- CREATE INDEX idx_usuarios_email ON usuarios(email);
-- CREATE INDEX idx_usuarios_activo ON usuarios(activo);
-- CREATE INDEX idx_usuario_permisos_usuario ON usuario_permisos(usuario_id);

-- 7. Comentarios
COMMENT ON VIEW usuarios IS 'Vista de usuarios con acceso a esta sucursal, incluye información de empresa y permisos';
COMMENT ON VIEW usuarios_auth IS 'Vista simplificada para procesos de autenticación';
COMMENT ON VIEW usuario_permisos IS 'Vista detallada de permisos por usuario en esta sucursal';
COMMENT ON VIEW configuracion_acceso_local IS 'Configuración de detección automática para esta sucursal';