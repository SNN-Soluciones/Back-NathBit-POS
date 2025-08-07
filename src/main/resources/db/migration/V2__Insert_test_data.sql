-- V2__Insert_test_data.sql
-- Datos de prueba mínimos para NathBit POS
-- Contraseña para todos: Password123!
-- Hash BCrypt: $2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy

-- 1. EMPRESAS (2 restaurantes)
INSERT INTO empresas (codigo, nombre, nombre_comercial, cedula_juridica, telefono, email, activa, plan, created_at, updated_at) VALUES
('REST001', 'Restaurante El Sabor S.A.', 'El Sabor', '3101234567', '2222-3333', 'info@elsabor.cr', true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('REST002', 'Pizzería Italiana S.A.', 'Pizza Express', '3102345678', '2555-4444', 'info@pizzaexpress.cr', true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. SUCURSALES (2 por restaurante)
INSERT INTO sucursales (empresa_id, codigo, nombre, direccion, telefono, es_principal, activa, created_at, updated_at) VALUES
-- El Sabor
(1, 'SUC001', 'Sucursal Central', 'San José Centro', '2222-3333', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'SUC002', 'Sucursal Escazú', 'Escazú Multiplaza', '2222-4444', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Pizza Express
(2, 'SUC001', 'Sucursal Heredia', 'Heredia Centro', '2555-4444', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'SUC002', 'Sucursal Alajuela', 'Alajuela Centro', '2555-5555', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. USUARIOS
INSERT INTO usuarios (email, password, nombre, apellidos, telefono, identificacion, tipo_identificacion, activo, bloqueado, intentos_fallidos, created_at, updated_at) VALUES
-- Root y Developer
('root@nathbit.com', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Root', 'System', '8888-8888', '000000000', 'CEDULA_FISICA', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('developer@snnsoluciones.com', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Developer', 'SNN', '7777-7777', '111111111', 'CEDULA_FISICA', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Admins de cada restaurante
('admin@elsabor.cr', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Juan', 'Pérez', '8888-1111', '105430789', 'CEDULA_FISICA', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admin@pizzaexpress.cr', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'María', 'González', '8888-2222', '204560123', 'CEDULA_FISICA', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. ASIGNAR ROLES
INSERT INTO usuarios_empresas_roles (usuario_id, empresa_id, sucursal_id, rol, permisos, es_principal, activo, fecha_asignacion, created_at, updated_at) VALUES
-- Developer tiene acceso SUPER_ADMIN a ambos restaurantes
(2, 1, NULL, 'SUPER_ADMIN', '{"acceso_total": true}'::jsonb, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, NULL, 'SUPER_ADMIN', '{"acceso_total": true}'::jsonb, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Admin El Sabor
(3, 1, NULL, 'ADMIN', '{"acceso_total": true}'::jsonb, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Admin Pizza Express
(4, 2, NULL, 'ADMIN', '{"acceso_total": true}'::jsonb, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- FIN