-- Migración para el schema public
-- V1__create_tenant_table.sql

-- Tabla de Tenants (empresas/restaurantes)
CREATE TABLE IF NOT EXISTS tenants (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    nombre_comercial VARCHAR(255),
    identificacion VARCHAR(50) NOT NULL,
    tipo_identificacion VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telefono VARCHAR(50),
    direccion TEXT,
    provincia VARCHAR(100),
    canton VARCHAR(100),
    distrito VARCHAR(100),
    -- Configuración de facturación electrónica
    certificado_p12 BYTEA,
    certificado_password VARCHAR(255),
    api_usuario_token VARCHAR(500),
    consecutivo_fe INTEGER DEFAULT 1,
    consecutivo_nc INTEGER DEFAULT 1,
    consecutivo_te INTEGER DEFAULT 1,
    -- Metadata
    activo BOOLEAN DEFAULT true,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
    );

-- Índices
CREATE INDEX idx_tenants_codigo ON tenants(codigo);
CREATE INDEX idx_tenants_activo ON tenants(activo);

-- Tabla de configuración del sistema (global)
CREATE TABLE IF NOT EXISTS system_config (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clave VARCHAR(100) UNIQUE NOT NULL,
    valor TEXT,
    descripcion TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Insertar configuraciones iniciales
INSERT INTO system_config (clave, valor, descripcion) VALUES
                                                          ('system.version', '1.0.0', 'Versión del sistema'),
                                                          ('system.maintenance', 'false', 'Modo de mantenimiento'),
                                                          ('system.max_tenants', '100', 'Número máximo de tenants permitidos');

-- Trigger para actualizar fecha_actualizacion
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();