-- V1__create_tenant_schema.sql
-- Crear esquema base para multi-tenant

-- Extensión para UUIDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabla de tenants en esquema public
CREATE TABLE IF NOT EXISTS public.tenants (
                                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    ruc VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    direccion TEXT,
    schema_name VARCHAR(50) NOT NULL,
    plan VARCHAR(50) DEFAULT 'BASICO',
    fecha_vencimiento_plan DATE,
    activo BOOLEAN DEFAULT true,
    configuracion JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Índices para búsqueda rápida
CREATE INDEX idx_tenants_codigo ON public.tenants(codigo);
CREATE INDEX idx_tenants_activo ON public.tenants(activo);

-- Función para actualizar updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para actualizar updated_at
CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE
    ON public.tenants FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Tabla de logs de tenant
CREATE TABLE IF NOT EXISTS public.tenant_logs (
                                                  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES public.tenants(id),
    accion VARCHAR(100) NOT NULL,
    detalles JSONB,
    usuario VARCHAR(100),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Función para crear schema por tenant
CREATE OR REPLACE FUNCTION public.create_tenant_schema(tenant_code VARCHAR)
RETURNS void AS $$
DECLARE
schema_name VARCHAR;
BEGIN
    -- Generar nombre de schema
    schema_name := 'tenant_' || LOWER(REGEXP_REPLACE(tenant_code, '[^a-zA-Z0-9]', '', 'g'));

    -- Crear el schema
EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

-- Dar permisos al usuario de la aplicación
EXECUTE format('GRANT ALL ON SCHEMA %I TO CURRENT_USER', schema_name);

-- Log de la acción
INSERT INTO public.tenant_logs (tenant_id, accion, detalles)
SELECT id, 'SCHEMA_CREATED', jsonb_build_object('schema_name', schema_name)
FROM public.tenants WHERE codigo = tenant_code;

END;
$$ LANGUAGE plpgsql;

-- Insertar tenant de prueba para desarrollo
INSERT INTO public.tenants (codigo, nombre, ruc, email, schema_name)
VALUES ('DEMO', 'Restaurant Demo', '1234567890', 'demo@restaurant.com', 'tenant_demo')
    ON CONFLICT (codigo) DO NOTHING;

-- Crear schema para tenant demo
SELECT public.create_tenant_schema('DEMO');