-- V2__create_order_tables.sql
-- Tablas de órdenes y operaciones para cada schema de tenant

-- =====================================================
-- TABLAS DE ÓRDENES
-- =====================================================

-- Tabla de Órdenes
CREATE TABLE IF NOT EXISTS ordenes (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       tenant_id VARCHAR(50) NOT NULL,
                                       numero_orden VARCHAR(50) NOT NULL,
                                       tipo VARCHAR(20) NOT NULL, -- MESA, LLEVAR, DELIVERY, etc
                                       estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                                       mesa_id UUID,
                                       cliente_id UUID,
                                       mesero_id UUID,
                                       caja_id UUID,
                                       fecha_orden TIMESTAMP NOT NULL,
                                       cantidad_personas INTEGER,
                                       subtotal DECIMAL(18,2) NOT NULL DEFAULT 0,
                                       total_descuentos DECIMAL(18,2) DEFAULT 0,
                                       total_impuestos DECIMAL(18,2) DEFAULT 0,
                                       total DECIMAL(18,2) NOT NULL DEFAULT 0,
                                       observaciones TEXT,
    -- Campos para delivery/llevar
                                       nombre_cliente_delivery VARCHAR(100),
                                       telefono_delivery VARCHAR(20),
                                       direccion_delivery TEXT,
                                       hora_entrega_estimada TIMESTAMP,
                                       activo BOOLEAN DEFAULT true,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       version BIGINT DEFAULT 0,
                                       FOREIGN KEY (mesa_id) REFERENCES mesas(id),
                                       FOREIGN KEY (cliente_id) REFERENCES clientes(id),
                                       FOREIGN KEY (mesero_id) REFERENCES usuarios(id),
                                       FOREIGN KEY (caja_id) REFERENCES cajas(id),
                                       UNIQUE(tenant_id, numero_orden)
);

-- Tabla de Detalle de Órdenes
CREATE TABLE IF NOT EXISTS orden_detalles (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                              tenant_id VARCHAR(50) NOT NULL,
                                              orden_id UUID NOT NULL,
                                              producto_id UUID NOT NULL,
                                              cantidad DECIMAL(10,2) NOT NULL,
                                              precio_unitario DECIMAL(10,2) NOT NULL,
                                              subtotal DECIMAL(18,2) NOT NULL,
                                              descuento_porcentaje DECIMAL(5,2) DEFAULT 0,
                                              descuento_monto DECIMAL(10,2) DEFAULT 0,
                                              impuesto_porcentaje DECIMAL(5,2),
                                              impuesto_monto DECIMAL(10,2),
                                              total DECIMAL(18,2) NOT NULL,
                                              notas TEXT,
                                              estado VARCHAR(20) DEFAULT 'PENDIENTE',
                                              activo BOOLEAN DEFAULT true,
                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              version BIGINT DEFAULT 0,
                                              FOREIGN KEY (orden_id) REFERENCES ordenes(id) ON DELETE CASCADE,
                                              FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- =====================================================
-- TABLAS DE CAJA Y PAGOS
-- =====================================================

-- Tabla de Aperturas/Cierres de Caja
CREATE TABLE IF NOT EXISTS caja_movimientos (
                                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                tenant_id VARCHAR(50) NOT NULL,
                                                caja_id UUID NOT NULL,
                                                tipo_movimiento VARCHAR(20) NOT NULL, -- APERTURA, CIERRE, INGRESO, EGRESO
                                                monto DECIMAL(18,2) NOT NULL,
                                                concepto VARCHAR(100),
                                                descripcion TEXT,
                                                usuario_id UUID NOT NULL,
                                                fecha_movimiento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                activo BOOLEAN DEFAULT true,
                                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                version BIGINT DEFAULT 0,
                                                FOREIGN KEY (caja_id) REFERENCES cajas(id),
                                                FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tabla de Pagos
CREATE TABLE IF NOT EXISTS pagos (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     tenant_id VARCHAR(50) NOT NULL,
                                     orden_id UUID NOT NULL,
                                     caja_id UUID NOT NULL,
                                     tipo_pago VARCHAR(20) NOT NULL, -- EFECTIVO, TARJETA, TRANSFERENCIA, etc
                                     monto DECIMAL(18,2) NOT NULL,
                                     monto_recibido DECIMAL(18,2),
                                     cambio DECIMAL(18,2),
                                     referencia VARCHAR(100),
                                     observaciones TEXT,
                                     usuario_id UUID NOT NULL,
                                     fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     activo BOOLEAN DEFAULT true,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     version BIGINT DEFAULT 0,
                                     FOREIGN KEY (orden_id) REFERENCES ordenes(id),
                                     FOREIGN KEY (caja_id) REFERENCES cajas(id),
                                     FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- =====================================================
-- TABLAS DE INVENTARIO
-- =====================================================

-- Tabla de Movimientos de Inventario
CREATE TABLE IF NOT EXISTS inventario_movimientos (
                                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                      tenant_id VARCHAR(50) NOT NULL,
                                                      producto_id UUID NOT NULL,
                                                      tipo_movimiento VARCHAR(20) NOT NULL, -- ENTRADA, SALIDA, AJUSTE
                                                      cantidad DECIMAL(10,2) NOT NULL,
                                                      stock_anterior DECIMAL(10,2),
                                                      stock_nuevo DECIMAL(10,2),
                                                      costo_unitario DECIMAL(10,2),
                                                      costo_total DECIMAL(18,2),
                                                      referencia_tipo VARCHAR(50), -- COMPRA, VENTA, AJUSTE_MANUAL
                                                      referencia_id UUID,
                                                      observaciones TEXT,
                                                      usuario_id UUID NOT NULL,
                                                      sucursal_id UUID NOT NULL,
                                                      fecha_movimiento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                      activo BOOLEAN DEFAULT true,
                                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                      version BIGINT DEFAULT 0,
                                                      FOREIGN KEY (producto_id) REFERENCES productos(id),
                                                      FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
                                                      FOREIGN KEY (sucursal_id) REFERENCES sucursales(id)
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
CREATE INDEX idx_pagos_fecha ON pagos(fecha_pago);
CREATE INDEX idx_caja_movimientos_caja ON caja_movimientos(caja_id);
CREATE INDEX idx_caja_movimientos_fecha ON caja_movimientos(fecha_movimiento);
CREATE INDEX idx_inventario_producto ON inventario_movimientos(producto_id);
CREATE INDEX idx_inventario_fecha ON inventario_movimientos(fecha_movimiento);

-- =====================================================
-- TRIGGERS PARA updated_at
-- =====================================================

CREATE TRIGGER update_ordenes_updated_at BEFORE UPDATE ON ordenes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orden_detalles_updated_at BEFORE UPDATE ON orden_detalles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_caja_movimientos_updated_at BEFORE UPDATE ON caja_movimientos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pagos_updated_at BEFORE UPDATE ON pagos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_inventario_movimientos_updated_at BEFORE UPDATE ON inventario_movimientos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();