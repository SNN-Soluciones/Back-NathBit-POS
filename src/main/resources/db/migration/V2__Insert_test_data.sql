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
INSERT INTO usuarios (email, username, password, nombre, apellidos, telefono, identificacion, tipo_identificacion, tipo_usuario, activo, bloqueado, intentos_fallidos, created_at, updated_at) VALUES
-- Usuarios SISTEMA (no requieren empresa)
('root@nathbit.com', 'root', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Root', 'System', '8888-8888', '000000000', 'CEDULA_FISICA', 'SISTEMA', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('soporte@snnsoluciones.com', 'soporte', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Soporte', 'SNN', '7777-7777', '111111111', 'CEDULA_FISICA', 'SISTEMA', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Usuarios EMPRESARIAL
-- Super Admin (puede no tener empresas inicialmente)
('superadmin@nathbit.com', 'superadmin', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Super', 'Admin', '8888-0000', '222222222', 'CEDULA_FISICA', 'EMPRESARIAL', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Admins de cada restaurante
('admin@elsabor.cr', 'admin_elsabor', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Juan', 'Pérez', '8888-1111', '105430789', 'CEDULA_FISICA', 'EMPRESARIAL', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admin@pizzaexpress.cr', 'admin_pizza', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'María', 'González', '8888-2222', '204560123', 'CEDULA_FISICA', 'EMPRESARIAL', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Usuarios OPERATIVO (para probar flujo completo)
('cajero@elsabor.cr', 'cajero_elsabor', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Pedro', 'Rodríguez', '8888-3333', '305670234', 'CEDULA_FISICA', 'OPERATIVO', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('mesero@pizzaexpress.cr', 'mesero_pizza', '$2a$10$Xw8gQCThYa4LQrZxDpqJYeHhX6eU4LxFZ8V5L7BhRqUqBQzG0HfZy', 'Ana', 'Jiménez', '8888-4444', '406780345', 'CEDULA_FISICA', 'OPERATIVO', true, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. ASIGNAR ROLES
-- NOTA: Usuarios SISTEMA (root, soporte) NO tienen roles en empresas
INSERT INTO usuarios_empresas_roles (usuario_id, empresa_id, sucursal_id, rol, permisos, es_principal, activo, fecha_asignacion, created_at, updated_at) VALUES
-- Super Admin con ambas empresas (tiene múltiples empresas bajo su control)
(3, 1, NULL, 'SUPER_ADMIN', '{"acceso_total": true}'::jsonb, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 2, NULL, 'SUPER_ADMIN', '{"acceso_total": true}'::jsonb, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Admin El Sabor (solo su empresa)
(4, 1, NULL, 'ADMIN', '{"acceso_total": true}'::jsonb, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Admin Pizza Express (solo su empresa)
(5, 2, NULL, 'ADMIN', '{"acceso_total": true}'::jsonb, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Cajero El Sabor (sucursal específica)
(6, 1, 1, 'CAJERO', '{"caja": {"abrir": true, "cerrar": true, "arqueo": true}, "ordenes": {"ver": true, "crear": true, "editar": true}}'::jsonb, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Mesero Pizza Express (sucursal específica)
(7, 2, 3, 'MESERO', '{"ordenes": {"ver": true, "crear": true, "editar": true}, "mesas": {"ver": true, "asignar": true}}'::jsonb, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO usuarios (email, username, password, nombre, apellidos, tipo_usuario, activo, bloqueado, intentos_fallidos) VALUES
('socio1@elsabor.cr', 'socio1_elsabor', '$2a$10$...', 'Carlos', 'Mendez', 'EMPRESARIAL', true, false, 0),
('socio2@elsabor.cr', 'socio2_elsabor', '$2a$10$...', 'Laura', 'Vargas', 'EMPRESARIAL', true, false, 0),
('socio3@elsabor.cr', 'socio3_elsabor', '$2a$10$...', 'Roberto', 'Solis', 'EMPRESARIAL', true, false, 0);

-- 2. Asignar SUPER_ADMIN a los 3 socios en El Sabor (empresa_id = 1)
INSERT INTO usuarios_empresas_roles (usuario_id, empresa_id, sucursal_id, rol, es_principal, activo) VALUES
(8, 1, NULL, 'SUPER_ADMIN', true, true),
(9, 1, NULL, 'SUPER_ADMIN', true, true),
(10, 1, NULL, 'SUPER_ADMIN',  true, true);
-- Datos de ejemplo adicionales se pueden agregar después
-- FIN