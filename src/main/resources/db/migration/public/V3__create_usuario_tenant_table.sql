-- V3__create_usuario_tenant_table.sql
-- Crear tabla de relación usuario-tenant en el schema público
-- Esta tabla permite que un usuario tenga acceso a múltiples tenants

-- =====================================================
-- TABLA USUARIO_TENANT
-- =====================================================

CREATE TABLE IF NOT EXISTS public.usuario_tenant (
                                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                     usuario_id UUID NOT NULL,
                                                     tenant_id VARCHAR(50) NOT NULL,
                                                     tenant_nombre VARCHAR(255),
                                                     tenant_tipo VARCHAR(50),
                                                     rol_id UUID,
                                                     es_propietario BOOLEAN DEFAULT false,
                                                     activo BOOLEAN DEFAULT true,
                                                     fecha_acceso TIMESTAMP,
                                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraint único para evitar duplicados
                                                     CONSTRAINT uk_usuario_tenant UNIQUE (usuario_id, tenant_id)
);

-- =====================================================
-- ÍNDICES PARA PERFORMANCE
-- =====================================================

CREATE INDEX idx_usuario_tenant_usuario_id ON public.usuario_tenant(usuario_id);
CREATE INDEX idx_usuario_tenant_tenant_id ON public.usuario_tenant(tenant_id);
CREATE INDEX idx_usuario_tenant_activo ON public.usuario_tenant(activo);
CREATE INDEX idx_usuario_tenant_fecha_acceso ON public.usuario_tenant(fecha_acceso);

-- =====================================================
-- COMENTARIOS DE DOCUMENTACIÓN
-- =====================================================

COMMENT ON TABLE public.usuario_tenant IS 'Tabla de relación entre usuarios y tenants, permite acceso multi-empresa';
COMMENT ON COLUMN public.usuario_tenant.usuario_id IS 'ID del usuario (referencia a usuarios en cada schema tenant)';
COMMENT ON COLUMN public.usuario_tenant.tenant_id IS 'ID del tenant (schema)';
COMMENT ON COLUMN public.usuario_tenant.tenant_nombre IS 'Nombre legible del tenant para mostrar en UI';
COMMENT ON COLUMN public.usuario_tenant.tenant_tipo IS 'Tipo de tenant: RESTAURANTE, FACTURACION, etc.';
COMMENT ON COLUMN public.usuario_tenant.rol_id IS 'ID del rol del usuario en este tenant específico';
COMMENT ON COLUMN public.usuario_tenant.es_propietario IS 'Indica si el usuario es propietario/administrador del tenant';
COMMENT ON COLUMN public.usuario_tenant.fecha_acceso IS 'Última fecha de acceso al tenant';