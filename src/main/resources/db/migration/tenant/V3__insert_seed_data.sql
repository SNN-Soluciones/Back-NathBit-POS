-- V3__insert_seed_data.sql
-- Datos iniciales para cada tenant

-- =====================================================
-- ROLES PREDETERMINADOS
-- =====================================================

INSERT INTO roles (tenant_id, nombre, descripcion) VALUES
                                                       (CURRENT_SCHEMA, 'SUPER_ADMIN', 'Administrador del sistema con acceso completo'),
                                                       (CURRENT_SCHEMA, 'ADMIN', 'Administrador del restaurante'),
                                                       (CURRENT_SCHEMA, 'GERENTE', 'Gerente con acceso a reportes y configuración'),
                                                       (CURRENT_SCHEMA, 'CAJERO', 'Cajero con acceso a ventas y cobros'),
                                                       (CURRENT_SCHEMA, 'MESERO', 'Mesero con acceso a toma de pedidos'),
                                                       (CURRENT_SCHEMA, 'COCINA', 'Personal de cocina')
ON CONFLICT (tenant_id, nombre) DO NOTHING;

-- =====================================================
-- PERMISOS BÁSICOS
-- =====================================================

INSERT INTO permisos (tenant_id, codigo, nombre, descripcion) VALUES
                                                                  -- Permisos de usuarios
                                                                  (CURRENT_SCHEMA, 'USUARIOS_VIEW', 'Ver usuarios', 'Permite ver la lista de usuarios'),
                                                                  (CURRENT_SCHEMA, 'USUARIOS_CREATE', 'Crear usuarios', 'Permite crear nuevos usuarios'),
                                                                  (CURRENT_SCHEMA, 'USUARIOS_EDIT', 'Editar usuarios', 'Permite editar usuarios existentes'),
                                                                  (CURRENT_SCHEMA, 'USUARIOS_DELETE', 'Eliminar usuarios', 'Permite eliminar usuarios'),

                                                                  -- Permisos de productos
                                                                  (CURRENT_SCHEMA, 'PRODUCTOS_VIEW', 'Ver productos', 'Permite ver el catálogo de productos'),
                                                                  (CURRENT_SCHEMA, 'PRODUCTOS_CREATE', 'Crear productos', 'Permite crear nuevos productos'),
                                                                  (CURRENT_SCHEMA, 'PRODUCTOS_EDIT', 'Editar productos', 'Permite editar productos existentes'),
                                                                  (CURRENT_SCHEMA, 'PRODUCTOS_DELETE', 'Eliminar productos', 'Permite eliminar productos'),

                                                                  -- Permisos de órdenes
                                                                  (CURRENT_SCHEMA, 'ORDENES_VIEW', 'Ver órdenes', 'Permite ver las órdenes'),
                                                                  (CURRENT_SCHEMA, 'ORDENES_CREATE', 'Crear órdenes', 'Permite crear nuevas órdenes'),
                                                                  (CURRENT_SCHEMA, 'ORDENES_EDIT', 'Editar órdenes', 'Permite editar órdenes'),
                                                                  (CURRENT_SCHEMA, 'ORDENES_CANCEL', 'Cancelar órdenes', 'Permite cancelar órdenes'),

                                                                  -- Permisos de caja
                                                                  (CURRENT_SCHEMA, 'CAJA_OPEN', 'Abrir caja', 'Permite abrir caja'),
                                                                  (CURRENT_SCHEMA, 'CAJA_CLOSE', 'Cerrar caja', 'Permite cerrar caja'),
                                                                  (CURRENT_SCHEMA, 'CAJA_VIEW_MOVEMENTS', 'Ver movimientos', 'Permite ver movimientos de caja'),

                                                                  -- Permisos de reportes
                                                                  (CURRENT_SCHEMA, 'REPORTES_VENTAS', 'Reportes de ventas', 'Permite ver reportes de ventas'),
                                                                  (CURRENT_SCHEMA, 'REPORTES_INVENTARIO', 'Reportes de inventario', 'Permite ver reportes de inventario'),
                                                                  (CURRENT_SCHEMA, 'REPORTES_FINANCIEROS', 'Reportes financieros', 'Permite ver reportes financieros')
ON CONFLICT (tenant_id, codigo) DO NOTHING;

-- =====================================================
-- ASIGNAR PERMISOS A ROLES
-- =====================================================

-- Super Admin - Todos los permisos
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permisos p
WHERE r.nombre = 'SUPER_ADMIN' AND r.tenant_id = CURRENT_SCHEMA
ON CONFLICT DO NOTHING;

-- Admin - Todos los permisos
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permisos p
WHERE r.nombre = 'ADMIN' AND r.tenant_id = CURRENT_SCHEMA
ON CONFLICT DO NOTHING;

-- Cajero - Permisos específicos
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permisos p
WHERE r.nombre = 'CAJERO'
  AND r.tenant_id = CURRENT_SCHEMA
  AND p.codigo IN ('ORDENES_VIEW', 'ORDENES_CREATE', 'ORDENES_EDIT',
                   'CAJA_OPEN', 'CAJA_CLOSE', 'CAJA_VIEW_MOVEMENTS',
                   'PRODUCTOS_VIEW')
ON CONFLICT DO NOTHING;

-- Mesero - Permisos específicos
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permisos p
WHERE r.nombre = 'MESERO'
  AND r.tenant_id = CURRENT_SCHEMA
  AND p.codigo IN ('ORDENES_VIEW', 'ORDENES_CREATE', 'ORDENES_EDIT',
                   'PRODUCTOS_VIEW')
ON CONFLICT DO NOTHING;

-- =====================================================
-- SUCURSAL PRINCIPAL
-- =====================================================

INSERT INTO sucursales (tenant_id, codigo, nombre, direccion, telefono, email) VALUES
    (CURRENT_SCHEMA, 'PRINCIPAL', 'Sucursal Principal', 'Dirección principal', '0000-0000', 'principal@demo.com')
ON CONFLICT (tenant_id, codigo) DO NOTHING;

-- =====================================================
-- CAJAS PREDETERMINADAS
-- =====================================================

INSERT INTO cajas (tenant_id, codigo, nombre, sucursal_id, numero_terminal)
SELECT
            CURRENT_SCHEMA,
            'CAJA01',
            'Caja Principal',
            s.id,
            '00001'
FROM sucursales s
WHERE s.tenant_id = CURRENT_SCHEMA AND s.codigo = 'PRINCIPAL'
ON CONFLICT (tenant_id, codigo) DO NOTHING;

-- =====================================================
-- USUARIO ADMINISTRADOR
-- =====================================================

INSERT INTO usuarios (tenant_id, email, password, nombre, apellidos, rol_id, sucursal_predeterminada_id)
SELECT
            CURRENT_SCHEMA,
            'admin@demo.com',
            '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqOlnjeRnGxVRCEKUGS5vNyPNy', -- Password: Admin123!
            'Administrador',
            'Demo',
            r.id,
            s.id
FROM roles r, sucursales s
WHERE r.tenant_id = CURRENT_SCHEMA
  AND r.nombre = 'ADMIN'
  AND s.tenant_id = CURRENT_SCHEMA
  AND s.codigo = 'PRINCIPAL'
ON CONFLICT (tenant_id, email) DO NOTHING;

-- Asignar sucursal al usuario admin
INSERT INTO usuario_sucursales (usuario_id, sucursal_id)
SELECT u.id, s.id
FROM usuarios u, sucursales s
WHERE u.tenant_id = CURRENT_SCHEMA
  AND u.email = 'admin@demo.com'
  AND s.tenant_id = CURRENT_SCHEMA
  AND s.codigo = 'PRINCIPAL'
ON CONFLICT DO NOTHING;

-- Asignar caja al usuario admin
INSERT INTO usuario_cajas (usuario_id, caja_id)
SELECT u.id, c.id
FROM usuarios u, cajas c
WHERE u.tenant_id = CURRENT_SCHEMA
  AND u.email = 'admin@demo.com'
  AND c.tenant_id = CURRENT_SCHEMA
  AND c.codigo = 'CAJA01'
ON CONFLICT DO NOTHING;

-- =====================================================
-- ZONAS PREDETERMINADAS
-- =====================================================

INSERT INTO zonas (tenant_id, codigo, nombre, sucursal_id, orden, color)
SELECT
            CURRENT_SCHEMA,
            'SALON',
            'Salón Principal',
            s.id,
            1,
            '#3B82F6'
FROM sucursales s
WHERE s.tenant_id = CURRENT_SCHEMA AND s.codigo = 'PRINCIPAL'
ON CONFLICT (sucursal_id, codigo) DO NOTHING;

INSERT INTO zonas (tenant_id, codigo, nombre, sucursal_id, orden, color)
SELECT
            CURRENT_SCHEMA,
            'TERRAZA',
            'Terraza',
            s.id,
            2,
            '#10B981'
FROM sucursales s
WHERE s.tenant_id = CURRENT_SCHEMA AND s.codigo = 'PRINCIPAL'
ON CONFLICT (sucursal_id, codigo) DO NOTHING;

-- =====================================================
-- MESAS DE EJEMPLO
-- =====================================================

-- Mesas del salón principal
INSERT INTO mesas (tenant_id, numero, nombre, sucursal_id, zona_id, capacidad_personas)
SELECT
            CURRENT_SCHEMA,
            'M01',
            'Mesa 1',
            s.id,
            z.id,
            4
FROM sucursales s, zonas z
WHERE s.tenant_id = CURRENT_SCHEMA
  AND s.codigo = 'PRINCIPAL'
  AND z.tenant_id = CURRENT_SCHEMA
  AND z.codigo = 'SALON'
ON CONFLICT (sucursal_id, numero) DO NOTHING;

INSERT INTO mesas (tenant_id, numero, nombre, sucursal_id, zona_id, capacidad_personas)
SELECT
            CURRENT_SCHEMA,
            'M02',
            'Mesa 2',
            s.id,
            z.id,
            2
FROM sucursales s, zonas z
WHERE s.tenant_id = CURRENT_SCHEMA
  AND s.codigo = 'PRINCIPAL'
  AND z.tenant_id = CURRENT_SCHEMA
  AND z.codigo = 'SALON'
ON CONFLICT (sucursal_id, numero) DO NOTHING;

-- =====================================================
-- CATEGORÍAS DE PRODUCTOS
-- =====================================================

INSERT INTO categorias (tenant_id, nombre, descripcion, orden) VALUES
                                                                   (CURRENT_SCHEMA, 'Bebidas', 'Bebidas en general', 1),
                                                                   (CURRENT_SCHEMA, 'Entradas', 'Entradas y aperitivos', 2),
                                                                   (CURRENT_SCHEMA, 'Platos Principales', 'Platos fuertes', 3),
                                                                   (CURRENT_SCHEMA, 'Postres', 'Postres y dulces', 4)
ON CONFLICT (tenant_id, nombre) DO NOTHING;

-- =====================================================
-- PRODUCTOS DE EJEMPLO
-- =====================================================

INSERT INTO productos (tenant_id, codigo, nombre, descripcion, categoria_id, precio_venta, precio_costo, stock_actual)
SELECT
            CURRENT_SCHEMA,
            'BEB001',
            'Coca Cola',
            'Coca Cola 350ml',
            c.id,
            1500.00,
            800.00,
            100
FROM categorias c
WHERE c.tenant_id = CURRENT_SCHEMA AND c.nombre = 'Bebidas'
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO productos (tenant_id, codigo, nombre, descripcion, categoria_id, precio_venta, precio_costo, stock_actual)
SELECT
            CURRENT_SCHEMA,
            'ENT001',
            'Nachos con Queso',
            'Nachos con queso cheddar derretido',
            c.id,
            3500.00,
            1500.00,
            50
FROM categorias c
WHERE c.tenant_id = CURRENT_SCHEMA AND c.nombre = 'Entradas'
ON CONFLICT (tenant_id, codigo) DO NOTHING;