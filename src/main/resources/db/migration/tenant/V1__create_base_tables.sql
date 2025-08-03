-- V1__create_base_tables.sql
-- Tablas base para cada schema de tenant

-- =====================================================
-- TABLAS DE SEGURIDAD
-- =====================================================

-- Tabla de Roles
CREATE TABLE IF NOT EXISTS roles (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     tenant_id VARCHAR(50) NOT NULL,
                                     nombre VARCHAR(30) NOT NULL,  -- Para enum RolNombre
                                     descripcion VARCHAR(100),
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
                                        codigo VARCHAR(50) NOT NULL,
                                        nombre VARCHAR(100) NOT NULL,
                                        descripcion VARCHAR(200),
                                        activo BOOLEAN DEFAULT true,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        version BIGINT DEFAULT 0,
                                        UNIQUE(tenant_id, codigo)
);

-- Tabla intermedia Rol-Permiso
CREATE TABLE IF NOT EXISTS roles_permisos (
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
                                          codigo VARCHAR(20) NOT NULL,
                                          nombre VARCHAR(100) NOT NULL,
                                          direccion VARCHAR(500),
                                          telefono VARCHAR(20),
                                          email VARCHAR(100),
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
                                     codigo VARCHAR(20) NOT NULL,
                                     nombre VARCHAR(100) NOT NULL,
                                     descripcion VARCHAR(200),
                                     sucursal_id UUID NOT NULL,
                                     estado VARCHAR(20) DEFAULT 'CERRADA',
                                     usuario_apertura_id UUID,
                                     fecha_apertura TIMESTAMP,
                                     monto_apertura DECIMAL(18,2),
                                     usuario_cierre_id UUID,
                                     fecha_cierre TIMESTAMP,
                                     monto_cierre DECIMAL(18,2),
                                     numero_terminal VARCHAR(5) DEFAULT '00001',
                                     activo BOOLEAN DEFAULT true,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     version BIGINT DEFAULT 0,
                                     FOREIGN KEY (sucursal_id) REFERENCES sucursales(id),
                                     UNIQUE(tenant_id, codigo)
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
                                        forzar_reloging BOOLEAN,  -- Campo que faltaba
                                        activo BOOLEAN DEFAULT true,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        version BIGINT DEFAULT 0,
                                        FOREIGN KEY (rol_id) REFERENCES roles(id),
                                        FOREIGN KEY (sucursal_predeterminada_id) REFERENCES sucursales(id),
                                        UNIQUE(tenant_id, email)
);

-- Tabla intermedia Usuario-Sucursal
CREATE TABLE IF NOT EXISTS usuario_sucursales (
                                                  usuario_id UUID NOT NULL,
                                                  sucursal_id UUID NOT NULL,
                                                  PRIMARY KEY (usuario_id, sucursal_id),
                                                  FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                                  FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) ON DELETE CASCADE
);

-- Tabla intermedia Usuario-Caja
CREATE TABLE IF NOT EXISTS usuario_cajas (
                                             usuario_id UUID NOT NULL,
                                             caja_id UUID NOT NULL,
                                             PRIMARY KEY (usuario_id, caja_id),
                                             FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                                             FOREIGN KEY (caja_id) REFERENCES cajas(id) ON DELETE CASCADE
);

-- Tabla intermedia Caja-Usuario (usuarios autorizados)
CREATE TABLE IF NOT EXISTS cajas_usuarios (
                                              caja_id UUID NOT NULL,
                                              usuario_id UUID NOT NULL,
                                              PRIMARY KEY (caja_id, usuario_id),
                                              FOREIGN KEY (caja_id) REFERENCES cajas(id) ON DELETE CASCADE,
                                              FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
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
                                        identificacion VARCHAR(50) NOT NULL,
                                        tipo_identificacion VARCHAR(20),
                                        nombre VARCHAR(100) NOT NULL,
                                        apellidos VARCHAR(100),
                                        nombre_comercial VARCHAR(200),
                                        telefono VARCHAR(50),
                                        email VARCHAR(255),
                                        direccion TEXT,
                                        credito_limite DECIMAL(10,2),
                                        credito_usado DECIMAL(10,2) DEFAULT 0,
                                        dias_credito INTEGER DEFAULT 0,
                                        activo BOOLEAN DEFAULT true,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        version BIGINT DEFAULT 0,
                                        UNIQUE(tenant_id, identificacion)
);

-- =====================================================
-- TABLAS DE OPERACIÓN
-- =====================================================

-- Tabla de Zonas
CREATE TABLE IF NOT EXISTS zonas (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     tenant_id VARCHAR(50) NOT NULL,
                                     codigo VARCHAR(20) NOT NULL,
                                     nombre VARCHAR(100) NOT NULL,
                                     descripcion VARCHAR(200),
                                     sucursal_id UUID NOT NULL,
                                     orden INTEGER DEFAULT 0,
                                     color VARCHAR(7),
                                     icono VARCHAR(50),
                                     activo BOOLEAN DEFAULT true,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     version BIGINT DEFAULT 0,
                                     FOREIGN KEY (sucursal_id) REFERENCES sucursales(id),
                                     UNIQUE(sucursal_id, codigo)
);

-- Tabla de Mesas
CREATE TABLE IF NOT EXISTS mesas (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     tenant_id VARCHAR(50) NOT NULL,
                                     numero VARCHAR(20) NOT NULL,
                                     nombre VARCHAR(100) NOT NULL,
                                     sucursal_id UUID NOT NULL,
                                     zona_id UUID NOT NULL,
                                     estado VARCHAR(20) DEFAULT 'LIBRE',
                                     capacidad_personas INTEGER DEFAULT 4,
                                     es_unible BOOLEAN DEFAULT true,
                                     es_activa BOOLEAN DEFAULT true,
                                     mesero_asignado_id UUID,
                                     fecha_apertura TIMESTAMP,
                                     activo BOOLEAN DEFAULT true,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     version BIGINT DEFAULT 0,
                                     FOREIGN KEY (sucursal_id) REFERENCES sucursales(id),
                                     FOREIGN KEY (zona_id) REFERENCES zonas(id),
                                     FOREIGN KEY (mesero_asignado_id) REFERENCES usuarios(id),
                                     UNIQUE(sucursal_id, numero)
);

-- =====================================================
-- TABLAS DE SEGURIDAD Y AUDITORÍA
-- =====================================================

-- Tabla para tokens en blacklist
CREATE TABLE IF NOT EXISTS token_blacklist (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               tenant_id VARCHAR(50) NOT NULL,
                                               token VARCHAR(500) NOT NULL UNIQUE,
                                               username VARCHAR(255) NOT NULL,
                                               expiration_date TIMESTAMP NOT NULL,
                                               blacklisted_at TIMESTAMP NOT NULL,
                                               reason VARCHAR(50),
                                               description VARCHAR(500),
                                               activo BOOLEAN DEFAULT true,
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               version BIGINT DEFAULT 0
);

-- Tabla de eventos de auditoría (AuditEvent)
CREATE TABLE IF NOT EXISTS audit_events (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            tenant_id VARCHAR(50) NOT NULL,
                                            username VARCHAR(100) NOT NULL,
                                            event_type VARCHAR(50) NOT NULL,
                                            event_date TIMESTAMP NOT NULL,
                                            ip_address VARCHAR(45),
                                            user_agent TEXT,
                                            details TEXT,
                                            success BOOLEAN NOT NULL,
                                            error_message TEXT,
                                            resource_type VARCHAR(50),
                                            resource_id VARCHAR(255),
                                            old_value TEXT,
                                            new_value TEXT,
                                            request_method VARCHAR(10),
                                            request_url TEXT,
                                            response_status INTEGER,
                                            execution_time_ms BIGINT,
                                            activo BOOLEAN DEFAULT true,
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            version BIGINT DEFAULT 0
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
CREATE INDEX idx_token_blacklist_token ON token_blacklist(token);
CREATE INDEX idx_token_blacklist_expiration ON token_blacklist(expiration_date);
CREATE INDEX idx_audit_username ON audit_events(username);
CREATE INDEX idx_audit_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_event_date ON audit_events(event_date);

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

CREATE TRIGGER update_token_blacklist_updated_at BEFORE UPDATE ON token_blacklist
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_audit_events_updated_at BEFORE UPDATE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();