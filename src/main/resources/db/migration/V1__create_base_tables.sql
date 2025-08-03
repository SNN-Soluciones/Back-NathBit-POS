-- Migración base para schemas de tenant
-- V1__create_base_tables.sql

-- =====================================================
-- TABLAS DE SEGURIDAD
-- =====================================================

-- Tabla de Roles
CREATE TABLE IF NOT EXISTS roles (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, nombre)
    );

-- Tabla de Permisos
CREATE TABLE IF NOT EXISTS permisos (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    modulo VARCHAR(50),
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, nombre)
    );

-- Tabla intermedia Rol-Permiso
CREATE TABLE IF NOT EXISTS rol_permisos (
                                            rol_id UUID NOT NULL,
                                            permiso_id UUID NOT NULL,
                                            PRIMARY KEY (rol_id, permiso_id),
    FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permiso_id) REFERENCES permisos(id) ON DELETE CASCADE
    );

-- =====================================================
-- TABLAS DE ORGANIZACIÓN
-- =====================================================

-- Tabla de Sucursales
CREATE TABLE IF NOT EXISTS sucursales (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    direccion TEXT,
    telefono VARCHAR(50),
    email VARCHAR(255),
    horario_apertura TIME,
    horario_cierre TIME,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, codigo)
    );

-- Tabla de Cajas
CREATE TABLE IF NOT EXISTS cajas (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    sucursal_id UUID NOT NULL,
    numero_caja INTEGER NOT NULL,
    nombre VARCHAR(100),
    estado VARCHAR(20) DEFAULT 'CERRADA',
    saldo_inicial DECIMAL(10,2) DEFAULT 0,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (sucursal_id) REFERENCES sucursales(id),
    UNIQUE(sucursal_id, numero_caja)
    );

-- Tabla de Usuarios
CREATE TABLE IF NOT EXISTS usuarios (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100),
    telefono VARCHAR(50),
    identificacion VARCHAR(50),
    tipo_identificacion VARCHAR(20),
    rol_id UUID,
    sucursal_predeterminada_id UUID,
    ultimo_acceso TIMESTAMP,
    intentos_fallidos INTEGER DEFAULT 0,
    bloqueado BOOLEAN DEFAULT false,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (rol_id) REFERENCES roles(id),
    FOREIGN KEY (sucursal_predeterminada_id) REFERENCES sucursales(id),
    UNIQUE(tenant_id, email)
    );

-- Tabla intermedia Usuario-Sucursal (un usuario puede acceder a múltiples sucursales)
CREATE TABLE IF NOT EXISTS usuario_sucursales (
                                                  usuario_id UUID NOT NULL,
                                                  sucursal_id UUID NOT NULL,
                                                  PRIMARY KEY (usuario_id, sucursal_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) ON DELETE CASCADE
    );

-- Tabla intermedia Usuario-Caja (un usuario puede operar múltiples cajas)
CREATE TABLE IF NOT EXISTS usuario_cajas (
                                             usuario_id UUID NOT NULL,
                                             caja_id UUID NOT NULL,
                                             PRIMARY KEY (usuario_id, caja_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (caja_id) REFERENCES cajas(id) ON DELETE CASCADE
    );

-- =====================================================
-- TABLAS DE CATÁLOGO
-- =====================================================

-- Tabla de Categorías
CREATE TABLE IF NOT EXISTS categorias (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    categoria_padre_id UUID,
    orden INTEGER DEFAULT 0,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (categoria_padre_id) REFERENCES categorias(id),
    UNIQUE(tenant_id, nombre)
    );

-- Tabla de Productos
CREATE TABLE IF NOT EXISTS productos (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    codigo_barras VARCHAR(100),
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    categoria_id UUID,
    precio_venta DECIMAL(10,2) NOT NULL,
    precio_costo DECIMAL(10,2),
    aplica_impuesto BOOLEAN DEFAULT true,
    porcentaje_impuesto DECIMAL(5,2) DEFAULT 13.00,
    unidad_medida VARCHAR(20) DEFAULT 'UNIDAD',
    stock_minimo INTEGER DEFAULT 0,
    stock_actual INTEGER DEFAULT 0,
    es_servicio BOOLEAN DEFAULT false,
    imagen_url VARCHAR(500),
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    UNIQUE(tenant_id, codigo)
    );

-- Tabla de Clientes
CREATE TABLE IF NOT EXISTS clientes (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    tipo_identificacion VARCHAR(20) NOT NULL,
    identificacion VARCHAR(50) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    telefono VARCHAR(50),
    direccion TEXT,
    provincia VARCHAR(100),
    canton VARCHAR(100),
    distrito VARCHAR(100),
    descuento_predeterminado DECIMAL(5,2) DEFAULT 0,
    limite_credito DECIMAL(10,2) DEFAULT 0,
    saldo_actual DECIMAL(10,2) DEFAULT 0,
    notas TEXT,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, identificacion)
    );

-- =====================================================
-- TABLAS DE RESTAURANTE
-- =====================================================

-- Tabla de Zonas
CREATE TABLE IF NOT EXISTS zonas (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    sucursal_id UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color_hex VARCHAR(7),
    orden INTEGER DEFAULT 0,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (sucursal_id) REFERENCES sucursales(id),
    UNIQUE(sucursal_id, nombre)
    );

-- Tabla de Mesas
CREATE TABLE IF NOT EXISTS mesas (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    zona_id UUID NOT NULL,
    numero INTEGER NOT NULL,
    capacidad INTEGER NOT NULL DEFAULT 4,
    estado VARCHAR(20) DEFAULT 'LIBRE',
    posicion_x INTEGER,
    posicion_y INTEGER,
    forma VARCHAR(20) DEFAULT 'CUADRADA',
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (zona_id) REFERENCES zonas(id),
    UNIQUE(zona_id, numero)
    );

-- =====================================================
-- ÍNDICES
-- =====================================================

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_tenant ON usuarios(tenant_id);
CREATE INDEX idx_productos_codigo ON productos(codigo);
CREATE INDEX idx_productos_tenant ON productos(tenant_id);
CREATE INDEX idx_clientes_identificacion ON clientes(identificacion);
CREATE INDEX idx_clientes_tenant ON clientes(tenant_id);
CREATE INDEX idx_sucursales_tenant ON sucursales(tenant_id);
CREATE INDEX idx_cajas_sucursal ON cajas(sucursal_id);
CREATE INDEX idx_mesas_zona ON mesas(zona_id);
CREATE INDEX idx_mesas_estado ON mesas(estado);

-- =====================================================
-- TRIGGER PARA ACTUALIZAR updated_at
-- =====================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Aplicar trigger a todas las tablas
CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_permisos_updated_at BEFORE UPDATE ON permisos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_usuarios_updated_at BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sucursales_updated_at BEFORE UPDATE ON sucursales
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cajas_updated_at BEFORE UPDATE ON cajas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categorias_updated_at BEFORE UPDATE ON categorias
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_productos_updated_at BEFORE UPDATE ON productos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_clientes_updated_at BEFORE UPDATE ON clientes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_zonas_updated_at BEFORE UPDATE ON zonas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_mesas_updated_at BEFORE UPDATE ON mesas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();