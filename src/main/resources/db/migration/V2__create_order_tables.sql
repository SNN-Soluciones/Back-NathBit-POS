-- Migración para tablas de órdenes y operaciones
-- V2__create_order_tables.sql

-- =====================================================
-- TABLAS DE OPERACIONES
-- =====================================================

-- Tabla de Aperturas de Caja
CREATE TABLE IF NOT EXISTS aperturas_caja (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    caja_id UUID NOT NULL,
    usuario_apertura_id UUID NOT NULL,
    usuario_cierre_id UUID,
    fecha_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre TIMESTAMP,
    monto_apertura DECIMAL(10,2) NOT NULL,
    monto_cierre_efectivo DECIMAL(10,2),
    monto_cierre_tarjeta DECIMAL(10,2),
    monto_cierre_transferencia DECIMAL(10,2),
    monto_cierre_otros DECIMAL(10,2),
    diferencia DECIMAL(10,2),
    observaciones TEXT,
    estado VARCHAR(20) DEFAULT 'ABIERTA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (caja_id) REFERENCES cajas(id),
    FOREIGN KEY (usuario_apertura_id) REFERENCES usuarios(id),
    FOREIGN KEY (usuario_cierre_id) REFERENCES usuarios(id)
    );

-- Tabla de Órdenes
CREATE TABLE IF NOT EXISTS ordenes (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    numero_orden VARCHAR(50) NOT NULL,
    tipo_orden VARCHAR(20) NOT NULL, -- MESA, LLEVAR, DOMICILIO
    sucursal_id UUID NOT NULL,
    caja_id UUID,
    usuario_id UUID NOT NULL,
    cliente_id UUID,
    mesa_id UUID,
    apertura_caja_id UUID,
    -- Montos
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
    descuento_porcentaje DECIMAL(5,2) DEFAULT 0,
    descuento_monto DECIMAL(10,2) DEFAULT 0,
    impuesto DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    -- Estados y fechas
    estado VARCHAR(20) DEFAULT 'PENDIENTE',
    fecha_orden TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega TIMESTAMP,
    -- Información adicional
    nombre_cliente VARCHAR(255),
    telefono_cliente VARCHAR(50),
    direccion_entrega TEXT,
    notas TEXT,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (sucursal_id) REFERENCES sucursales(id),
    FOREIGN KEY (caja_id) REFERENCES cajas(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    FOREIGN KEY (apertura_caja_id) REFERENCES aperturas_caja(id),
    UNIQUE(tenant_id, numero_orden)
    );

-- Tabla de Detalles de Orden
CREATE TABLE IF NOT EXISTS orden_detalles (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    orden_id UUID NOT NULL,
    producto_id UUID NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    descuento_porcentaje DECIMAL(5,2) DEFAULT 0,
    descuento_monto DECIMAL(10,2) DEFAULT 0,
    impuesto DECIMAL(10,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    notas TEXT,
    estado VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (orden_id) REFERENCES ordenes(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
    );

-- Tabla de Pagos
CREATE TABLE IF NOT EXISTS pagos (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    orden_id UUID NOT NULL,
    tipo_pago VARCHAR(20) NOT NULL, -- EFECTIVO, TARJETA, TRANSFERENCIA, SINPE, CREDITO
    monto DECIMAL(10,2) NOT NULL,
    referencia VARCHAR(100),
    numero_autorizacion VARCHAR(100),
    cambio DECIMAL(10,2) DEFAULT 0,
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (orden_id) REFERENCES ordenes(id)
    );

-- Tabla de Facturas (información de facturación electrónica)
CREATE TABLE IF NOT EXISTS facturas (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    orden_id UUID NOT NULL,
    tipo_documento VARCHAR(20) NOT NULL, -- FE, TE, NC, ND
    numero_consecutivo VARCHAR(50) NOT NULL,
    clave_numerica VARCHAR(50),
    fecha_emision TIMESTAMP NOT NULL,
    -- Receptor
    receptor_tipo_identificacion VARCHAR(20),
    receptor_identificacion VARCHAR(50),
    receptor_nombre VARCHAR(255),
    receptor_email VARCHAR(255),
    -- Montos
    total_gravado DECIMAL(10,2),
    total_exento DECIMAL(10,2),
    total_impuesto DECIMAL(10,2),
    total_comprobante DECIMAL(10,2),
    -- Estado Hacienda
    estado_hacienda VARCHAR(20) DEFAULT 'PENDIENTE',
    mensaje_hacienda TEXT,
    xml_enviado TEXT,
    xml_respuesta TEXT,
    fecha_respuesta TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (orden_id) REFERENCES ordenes(id),
    UNIQUE(tenant_id, numero_consecutivo)
    );

-- =====================================================
-- ÍNDICES ADICIONALES
-- =====================================================

CREATE INDEX idx_ordenes_numero ON ordenes(numero_orden);
CREATE INDEX idx_ordenes_fecha ON ordenes(fecha_orden);
CREATE INDEX idx_ordenes_estado ON ordenes(estado);
CREATE INDEX idx_ordenes_mesa ON ordenes(mesa_id);
CREATE INDEX idx_ordenes_cliente ON ordenes(cliente_id);
CREATE INDEX idx_orden_detalles_orden ON orden_detalles(orden_id);
CREATE INDEX idx_pagos_orden ON pagos(orden_id);
CREATE INDEX idx_facturas_orden ON facturas(orden_id);
CREATE INDEX idx_facturas_consecutivo ON facturas(numero_consecutivo);
CREATE INDEX idx_aperturas_caja_fecha ON aperturas_caja(fecha_apertura);
CREATE INDEX idx_aperturas_caja_estado ON aperturas_caja(estado);

-- =====================================================
-- TRIGGERS PARA updated_at
-- =====================================================

CREATE TRIGGER update_aperturas_caja_updated_at BEFORE UPDATE ON aperturas_caja
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ordenes_updated_at BEFORE UPDATE ON ordenes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orden_detalles_updated_at BEFORE UPDATE ON orden_detalles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pagos_updated_at BEFORE UPDATE ON pagos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_facturas_updated_at BEFORE UPDATE ON facturas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();