-- Migración para insertar datos semilla en cada tenant
-- V3__insert_seed_data.sql

-- =====================================================
-- ROLES Y PERMISOS
-- =====================================================

-- Insertar roles básicos
INSERT INTO roles (tenant_id, nombre, descripcion) VALUES
                                                       (CURRENT_SCHEMA, 'ADMIN', 'Administrador del sistema con acceso total'),
                                                       (CURRENT_SCHEMA, 'GERENTE', 'Gerente con acceso a reportes y configuración'),
                                                       (CURRENT_SCHEMA, 'CAJERO', 'Cajero con acceso a ventas y caja'),
                                                       (CURRENT_SCHEMA, 'MESERO', 'Mesero con acceso a toma de pedidos'),
                                                       (CURRENT_SCHEMA, 'COCINA', 'Personal de cocina con vista de órdenes');

-- Insertar permisos básicos
INSERT INTO permisos (tenant_id, nombre, descripcion, modulo) VALUES
                                                                  -- Permisos de Sistema
                                                                  (CURRENT_SCHEMA, 'SISTEMA_CONFIGURAR', 'Configurar parámetros del sistema', 'SISTEMA'),
                                                                  (CURRENT_SCHEMA, 'USUARIOS_VER', 'Ver usuarios', 'USUARIOS'),
                                                                  (CURRENT_SCHEMA, 'USUARIOS_CREAR', 'Crear usuarios', 'USUARIOS'),
                                                                  (CURRENT_SCHEMA, 'USUARIOS_EDITAR', 'Editar usuarios', 'USUARIOS'),
                                                                  (CURRENT_SCHEMA, 'USUARIOS_ELIMINAR', 'Eliminar usuarios', 'USUARIOS'),
                                                                  -- Permisos de Productos
                                                                  (CURRENT_SCHEMA, 'PRODUCTOS_VER', 'Ver productos', 'PRODUCTOS'),
                                                                  (CURRENT_SCHEMA, 'PRODUCTOS_CREAR', 'Crear productos', 'PRODUCTOS'),
                                                                  (CURRENT_SCHEMA, 'PRODUCTOS_EDITAR', 'Editar productos', 'PRODUCTOS'),
                                                                  (CURRENT_SCHEMA, 'PRODUCTOS_ELIMINAR', 'Eliminar productos', 'PRODUCTOS'),
                                                                  -- Permisos de Ventas
                                                                  (CURRENT_SCHEMA, 'ORDENES_VER', 'Ver órdenes', 'VENTAS'),
                                                                  (CURRENT_SCHEMA, 'ORDENES_CREAR', 'Crear órdenes', 'VENTAS'),
                                                                  (CURRENT_SCHEMA, 'ORDENES_EDITAR', 'Editar órdenes', 'VENTAS'),
                                                                  (CURRENT_SCHEMA, 'ORDENES_ANULAR', 'Anular órdenes', 'VENTAS'),
                                                                  (CURRENT_SCHEMA, 'ORDENES_COBRAR', 'Cobrar órdenes', 'VENTAS'),
                                                                  -- Permisos de Caja
                                                                  (CURRENT_SCHEMA, 'CAJA_ABRIR', 'Abrir caja', 'CAJA'),
                                                                  (CURRENT_SCHEMA, 'CAJA_CERRAR', 'Cerrar caja', 'CAJA'),
                                                                  (CURRENT_SCHEMA, 'CAJA_ARQUEO', 'Realizar arqueo de caja', 'CAJA'),
                                                                  -- Permisos de Reportes
                                                                  (CURRENT_SCHEMA, 'REPORTES_VENTAS', 'Ver reportes de ventas', 'REPORTES'),
                                                                  (CURRENT_SCHEMA, 'REPORTES_INVENTARIO', 'Ver reportes de inventario', 'REPORTES'),
                                                                  (CURRENT_SCHEMA, 'REPORTES_FINANCIEROS', 'Ver reportes financieros', 'REPORTES');

-- Asignar todos los permisos al rol ADMIN
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permisos p
WHERE r.nombre = 'ADMIN' AND r.tenant_id = CURRENT_SCHEMA;

-- Asignar permisos específicos a CAJERO
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permisos p
WHERE r.nombre = 'CAJERO'
  AND r.tenant_id = CURRENT_SCHEMA
  AND p.nombre IN ('ORDENES_VER', 'ORDENES_CREAR', 'ORDENES_COBRAR',
                   'CAJA_ABRIR', 'CAJA_CERRAR', 'CAJA_ARQUEO',
                   'PRODUCTOS_VER');

-- Asignar permisos a MESERO
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permisos p
WHERE r.nombre = 'MESERO'
  AND r.tenant_id = CURRENT_SCHEMA
  AND p.nombre IN ('ORDENES_VER', 'ORDENES_CREAR', 'ORDENES_EDITAR', 'PRODUCTOS_VER');

-- =====================================================
-- SUCURSAL Y CAJA
-- =====================================================

-- Insertar sucursal principal
INSERT INTO sucursales (tenant_id, codigo, nombre, direccion, telefono, email, horario_apertura, horario_cierre)
VALUES (
           CURRENT_SCHEMA,
           'PRINCIPAL',
           'Sucursal Principal',
           'Avenida Central, Local 123',
           '2222-2222',
           'principal@' || CURRENT_SCHEMA || '.com',
           '08:00:00',
           '22:00:00'
       );

-- Insertar cajas para la sucursal principal
INSERT INTO cajas (tenant_id, sucursal_id, numero_caja, nombre, estado)
SELECT
    CURRENT_SCHEMA,
    s.id,
    1,
    'Caja Principal',
    'CERRADA'
FROM sucursales s
WHERE s.codigo = 'PRINCIPAL' AND s.tenant_id = CURRENT_SCHEMA;

INSERT INTO cajas (tenant_id, sucursal_id, numero_caja, nombre, estado)
SELECT
    CURRENT_SCHEMA,
    s.id,
    2,
    'Caja Secundaria',
    'CERRADA'
FROM sucursales s
WHERE s.codigo = 'PRINCIPAL' AND s.tenant_id = CURRENT_SCHEMA;

-- =====================================================
-- USUARIO ADMINISTRADOR
-- =====================================================

-- Insertar usuario administrador
-- Password: Admin123! (deberá ser hasheado con BCrypt en la aplicación)
INSERT INTO usuarios (
    tenant_id,
    email,
    password,
    nombre,
    apellidos,
    telefono,
    identificacion,
    tipo_identificacion,
    rol_id,
    sucursal_predeterminada_id
)
SELECT
    CURRENT_SCHEMA,
    'admin@' || CURRENT_SCHEMA || '.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqOlnjeRnGxVRCEKUGS5vNyPNy', -- BCrypt de "Admin123!"
    'Administrador',
    'Sistema',
    '8888-8888',
    '1-1234-5678',
    'FISICA',
    r.id,
    s.id
FROM roles r, sucursales s
WHERE r.nombre = 'ADMIN'
  AND r.tenant_id = CURRENT_SCHEMA
  AND s.codigo = 'PRINCIPAL'
  AND s.tenant_id = CURRENT_SCHEMA;

-- Asignar usuario a sucursal
INSERT INTO usuario_sucursales (usuario_id, sucursal_id)
SELECT u.id, s.id
FROM usuarios u, sucursales s
WHERE u.email = 'admin@' || CURRENT_SCHEMA || '.com'
  AND u.tenant_id = CURRENT_SCHEMA
  AND s.codigo = 'PRINCIPAL'
  AND s.tenant_id = CURRENT_SCHEMA;

-- Asignar usuario a cajas
INSERT INTO usuario_cajas (usuario_id, caja_id)
SELECT u.id, c.id
FROM usuarios u
         JOIN sucursales s ON s.tenant_id = u.tenant_id
         JOIN cajas c ON c.sucursal_id = s.id
WHERE u.email = 'admin@' || CURRENT_SCHEMA || '.com'
  AND u.tenant_id = CURRENT_SCHEMA;

-- =====================================================
-- CATEGORÍAS Y PRODUCTOS DE EJEMPLO
-- =====================================================

-- Insertar categorías
INSERT INTO categorias (tenant_id, nombre, descripcion, orden) VALUES
                                                                   (CURRENT_SCHEMA, 'Bebidas', 'Bebidas frías y calientes', 1),
                                                                   (CURRENT_SCHEMA, 'Entradas', 'Aperitivos y entradas', 2),
                                                                   (CURRENT_SCHEMA, 'Platos Fuertes', 'Platos principales', 3),
                                                                   (CURRENT_SCHEMA, 'Postres', 'Postres y dulces', 4),
                                                                   (CURRENT_SCHEMA, 'Extras', 'Acompañamientos y extras', 5);

-- Insertar productos de ejemplo
INSERT INTO productos (tenant_id, codigo, nombre, descripcion, categoria_id, precio_venta, precio_costo, aplica_impuesto)
SELECT
    CURRENT_SCHEMA,
    'BEB001',
    'Coca Cola 350ml',
    'Refresco de cola en lata',
    c.id,
    1500.00,
    800.00,
    true
FROM categorias c
WHERE c.nombre = 'Bebidas' AND c.tenant_id = CURRENT_SCHEMA;

INSERT INTO productos (tenant_id, codigo, nombre, descripcion, categoria_id, precio_venta, precio_costo, aplica_impuesto)
SELECT
    CURRENT_SCHEMA,
    'BEB002',
    'Café Americano',
    'Café negro americano',
    c.id,
    1200.00,
    300.00,
    true
FROM categorias c
WHERE c.nombre = 'Bebidas' AND c.tenant_id = CURRENT_SCHEMA;

INSERT INTO productos (tenant_id, codigo, nombre, descripcion, categoria_id, precio_venta, precio_costo, aplica_impuesto)
SELECT
    CURRENT_SCHEMA,
    'ENT001',
    'Nachos con Queso',
    'Nachos con queso cheddar derretido',
    c.id,
    3500.00,
    1200.00,
    true
FROM categorias c
WHERE c.nombre = 'Entradas' AND c.tenant_id = CURRENT_SCHEMA;

INSERT INTO productos (tenant_id, codigo, nombre, descripcion, categoria_id, precio_venta, precio_costo, aplica_impuesto)
SELECT
    CURRENT_SCHEMA,
    'PLA001',
    'Hamburguesa Clásica',
    'Hamburguesa de res con lechuga, tomate y cebolla',
    c.id,
    5500.00,
    2000.00,
    true
FROM categorias c
WHERE c.nombre = 'Platos Fuertes' AND c.tenant_id = CURRENT_SCHEMA;

INSERT INTO productos (tenant_id, codigo, nombre, descripcion, categoria_id, precio_venta, precio_costo, aplica_impuesto)
SELECT
    CURRENT_SCHEMA,
    'PLA002',
    'Pizza Margarita',
    'Pizza con salsa de tomate, mozzarella y albahaca',
    c.id,
    7500.00,
    2500.00,
    true
FROM categorias c
WHERE c.nombre = 'Platos Fuertes' AND c.tenant_id = CURRENT_SCHEMA;

-- =====================================================
-- ZONAS Y MESAS
-- =====================================================

-- Insertar zonas
INSERT INTO zonas (tenant_id, sucursal_id, nombre, descripcion, color_hex, orden)
SELECT
    CURRENT_SCHEMA,
    s.id,
    'Salón Principal',
    'Área principal del restaurante',
    '#3498db',
    1
FROM sucursales s
WHERE s.codigo = 'PRINCIPAL' AND s.tenant_id = CURRENT_SCHEMA;

INSERT INTO zonas (tenant_id, sucursal_id, nombre, descripcion, color_hex, orden)
SELECT
    CURRENT_SCHEMA,
    s.id,
    'Terraza',
    'Área exterior con vista',
    '#2ecc71',
    2
FROM sucursales s
WHERE s.codigo = 'PRINCIPAL' AND s.tenant_id = CURRENT_SCHEMA;

-- Insertar mesas para el salón principal
INSERT INTO mesas (tenant_id, zona_id, numero, capacidad, estado, posicion_x, posicion_y)
SELECT
    CURRENT_SCHEMA,
    z.id,
    generate_series(1, 10),
    4,
    'LIBRE',
    (generate_series(1, 10) - 1) % 5 * 100,
    floor((generate_series(1, 10) - 1) / 5) * 100
FROM zonas z
WHERE z.nombre = 'Salón Principal' AND z.tenant_id = CURRENT_SCHEMA;

-- Insertar mesas para la terraza
INSERT INTO mesas (tenant_id, zona_id, numero, capacidad, estado, posicion_x, posicion_y)
SELECT
    CURRENT_SCHEMA,
    z.id,
    generate_series(11, 15),
    6,
    'LIBRE',
    (generate_series(1, 5) - 1) * 120,
    0
FROM zonas z
WHERE z.nombre = 'Terraza' AND z.tenant_id = CURRENT_SCHEMA;

-- =====================================================
-- CLIENTE GENÉRICO
-- =====================================================

INSERT INTO clientes (
    tenant_id,
    tipo_identificacion,
    identificacion,
    nombre,
    email
) VALUES (
             CURRENT_SCHEMA,
             'FISICA',
             '0-0000-0000',
             'Cliente Genérico',
             'cliente@generico.com'
         );