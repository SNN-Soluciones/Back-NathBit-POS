-- =====================================================
-- MIGRACIÓN: Estructura de Usuarios Globales y Empresas
-- Archivo: V10__create_global_users_structure.sql
-- Ubicación: src/main/resources/db/migration/public/
-- =====================================================

-- 1. Tabla de Usuarios Globales
CREATE TABLE IF NOT EXISTS public.usuarios_global (
                                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                      email VARCHAR(255) UNIQUE NOT NULL,
                                                      password VARCHAR(255) NOT NULL,
                                                      nombre VARCHAR(100) NOT NULL,
                                                      apellidos VARCHAR(100),
                                                      telefono VARCHAR(20),
                                                      identificacion VARCHAR(50),
                                                      tipo_identificacion VARCHAR(20) CHECK (tipo_identificacion IN ('CEDULA_NACIONAL', 'CEDULA_RESIDENCIA', 'PASAPORTE', 'DIMEX')),
                                                      activo BOOLEAN DEFAULT true,
                                                      bloqueado BOOLEAN DEFAULT false,
                                                      intentos_fallidos INTEGER DEFAULT 0,
                                                      ultimo_acceso TIMESTAMP WITH TIME ZONE,
                                                      fecha_password_expira TIMESTAMP WITH TIME ZONE,
                                                      debe_cambiar_password BOOLEAN DEFAULT false,
                                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                      created_by UUID,
                                                      updated_by UUID
);

-- 2. Tabla de Empresas
CREATE TABLE IF NOT EXISTS public.empresas (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               codigo VARCHAR(50) UNIQUE NOT NULL,
                                               nombre VARCHAR(200) NOT NULL,
                                               nombre_comercial VARCHAR(200),
                                               cedula_juridica VARCHAR(50) UNIQUE,
                                               telefono VARCHAR(20),
                                               email VARCHAR(100),
                                               direccion TEXT,
                                               provincia VARCHAR(50),
                                               canton VARCHAR(50),
                                               distrito VARCHAR(50),
                                               tipo VARCHAR(50) DEFAULT 'RESTAURANTE' CHECK (tipo IN ('RESTAURANTE', 'CAFETERIA', 'BAR', 'COMIDA_RAPIDA', 'FOOD_TRUCK', 'OTRO')),
                                               logo_url VARCHAR(500),
                                               activa BOOLEAN DEFAULT true,
                                               configuracion JSONB DEFAULT '{}',
                                               plan_suscripcion VARCHAR(50) DEFAULT 'BASICO',
                                               fecha_vencimiento_plan DATE,
                                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                               created_by UUID,
                                               updated_by UUID
);

-- 3. Tabla de Sucursales
CREATE TABLE IF NOT EXISTS public.empresas_sucursales (
                                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                          empresa_id UUID NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
                                                          codigo_sucursal VARCHAR(50) NOT NULL,
                                                          nombre_sucursal VARCHAR(200) NOT NULL,
                                                          schema_name VARCHAR(50) UNIQUE NOT NULL, -- nombre del tenant (ej: sucursal_001)
                                                          direccion TEXT,
                                                          telefono VARCHAR(20),
                                                          email VARCHAR(100),
                                                          provincia VARCHAR(50),
                                                          canton VARCHAR(50),
                                                          distrito VARCHAR(50),
                                                          coordenadas_gps JSONB, -- {"lat": 9.9281, "lng": -84.0907}
                                                          activa BOOLEAN DEFAULT true,
                                                          es_principal BOOLEAN DEFAULT false,
                                                          horario_operacion JSONB DEFAULT '{}', -- {"lunes": {"apertura": "08:00", "cierre": "22:00"}, ...}
                                                          configuracion JSONB DEFAULT '{}',
                                                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                          CONSTRAINT uk_empresa_codigo_sucursal UNIQUE(empresa_id, codigo_sucursal)
);

-- 4. Tabla de Relación Usuario-Empresa
CREATE TABLE IF NOT EXISTS public.usuario_empresas (
                                                       usuario_id UUID NOT NULL REFERENCES usuarios_global(id) ON DELETE CASCADE,
                                                       empresa_id UUID NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
                                                       rol VARCHAR(50) NOT NULL CHECK (rol IN ('SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO', 'COCINA', 'CONTADOR')),
                                                       fecha_asignacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                       fecha_expiracion TIMESTAMP WITH TIME ZONE,
                                                       asignado_por UUID REFERENCES usuarios_global(id),
                                                       activo BOOLEAN DEFAULT true,
                                                       es_propietario BOOLEAN DEFAULT false,
                                                       configuracion_rol JSONB DEFAULT '{}', -- permisos específicos del rol
                                                       PRIMARY KEY (usuario_id, empresa_id)
);

-- 5. Tabla de Permisos por Sucursal
CREATE TABLE IF NOT EXISTS public.usuario_sucursales (
                                                         usuario_id UUID NOT NULL,
                                                         empresa_id UUID NOT NULL,
                                                         sucursal_id UUID NOT NULL REFERENCES empresas_sucursales(id) ON DELETE CASCADE,
                                                         puede_leer BOOLEAN DEFAULT true,
                                                         puede_escribir BOOLEAN DEFAULT true,
                                                         puede_eliminar BOOLEAN DEFAULT false,
                                                         puede_aprobar BOOLEAN DEFAULT false,
                                                         es_principal BOOLEAN DEFAULT false, -- sucursal por defecto para este usuario
                                                         activo BOOLEAN DEFAULT true,
                                                         fecha_asignacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                         asignado_por UUID REFERENCES usuarios_global(id),
                                                         PRIMARY KEY (usuario_id, sucursal_id),
                                                         FOREIGN KEY (usuario_id, empresa_id) REFERENCES usuario_empresas(usuario_id, empresa_id) ON DELETE CASCADE
);

-- 6. Tabla de Configuración de Acceso (IPs, Terminales)
CREATE TABLE IF NOT EXISTS public.configuracion_acceso (
                                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                           sucursal_id UUID NOT NULL REFERENCES empresas_sucursales(id) ON DELETE CASCADE,
                                                           tipo_deteccion VARCHAR(50) NOT NULL CHECK (tipo_deteccion IN ('IP_RANGE', 'TERMINAL_ID', 'MANUAL')),
                                                           configuracion JSONB NOT NULL, -- {"ip_inicio": "192.168.1.1", "ip_fin": "192.168.1.255"} o {"terminal_id": "POS-001"}
                                                           descripcion VARCHAR(200),
                                                           activo BOOLEAN DEFAULT true,
                                                           prioridad INTEGER DEFAULT 100,
                                                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Índices para Performance
CREATE INDEX idx_usuarios_global_email ON public.usuarios_global(email);
CREATE INDEX idx_usuarios_global_activo ON public.usuarios_global(activo) WHERE activo = true;
CREATE INDEX idx_empresas_codigo ON public.empresas(codigo);
CREATE INDEX idx_empresas_activa ON public.empresas(activa) WHERE activa = true;
CREATE INDEX idx_empresas_sucursales_empresa ON public.empresas_sucursales(empresa_id);
CREATE INDEX idx_empresas_sucursales_schema ON public.empresas_sucursales(schema_name);
CREATE INDEX idx_usuario_empresas_usuario ON public.usuario_empresas(usuario_id);
CREATE INDEX idx_usuario_empresas_empresa ON public.usuario_empresas(empresa_id);
CREATE INDEX idx_usuario_empresas_activo ON public.usuario_empresas(usuario_id, activo) WHERE activo = true;
CREATE INDEX idx_usuario_sucursales_usuario ON public.usuario_sucursales(usuario_id);
CREATE INDEX idx_usuario_sucursales_sucursal ON public.usuario_sucursales(sucursal_id);
CREATE INDEX idx_configuracion_acceso_sucursal ON public.configuracion_acceso(sucursal_id, activo) WHERE activo = true;

-- 8. Triggers para updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_usuarios_global_updated_at BEFORE UPDATE ON public.usuarios_global
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_empresas_updated_at BEFORE UPDATE ON public.empresas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_empresas_sucursales_updated_at BEFORE UPDATE ON public.empresas_sucursales
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 9. Función para validar acceso de usuario
CREATE OR REPLACE FUNCTION public.validar_acceso_usuario(
    p_usuario_id UUID,
    p_empresa_id UUID,
    p_sucursal_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_tiene_acceso BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM usuario_empresas ue
                 JOIN usuario_sucursales us ON ue.usuario_id = us.usuario_id AND ue.empresa_id = us.empresa_id
        WHERE ue.usuario_id = p_usuario_id
          AND ue.empresa_id = p_empresa_id
          AND us.sucursal_id = p_sucursal_id
          AND ue.activo = true
          AND us.activo = true
    ) INTO v_tiene_acceso;

    RETURN v_tiene_acceso;
END;
$$ LANGUAGE plpgsql;

-- 10. Comentarios de documentación
COMMENT ON TABLE public.usuarios_global IS 'Tabla central de usuarios del sistema, compartida entre todas las empresas y sucursales';
COMMENT ON TABLE public.empresas IS 'Catálogo de empresas/franquicias que usan el sistema';
COMMENT ON TABLE public.empresas_sucursales IS 'Sucursales de cada empresa, cada una con su propio schema/tenant';
COMMENT ON TABLE public.usuario_empresas IS 'Relación usuario-empresa con el rol principal del usuario en esa empresa';
COMMENT ON TABLE public.usuario_sucursales IS 'Permisos específicos del usuario en cada sucursal';
COMMENT ON TABLE public.configuracion_acceso IS 'Reglas para detección automática de sucursal (IP, terminal, etc)';

COMMENT ON COLUMN public.empresas_sucursales.schema_name IS 'Nombre del schema PostgreSQL para esta sucursal (tenant)';
COMMENT ON COLUMN public.usuario_empresas.es_propietario IS 'Indica si el usuario es dueño de la empresa';
COMMENT ON COLUMN public.usuario_sucursales.es_principal IS 'Sucursal por defecto para usuarios operativos (cajeros/meseros)';