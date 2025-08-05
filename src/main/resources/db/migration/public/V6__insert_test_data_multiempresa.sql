-- =====================================================
-- MIGRACIÓN: Datos de Prueba Multi-Empresa
-- Archivo: V6__insert_test_data_multiempresa.sql
-- Ubicación: src/main/resources/db/migration/public/
-- =====================================================

-- 1. Crear empresas de prueba
INSERT INTO public.empresas (id, codigo, nombre, nombre_comercial, cedula_juridica, tipo, activa) VALUES
                                                                                                      ('550e8400-e29b-41d4-a716-446655440001', 'REST001', 'Grupo Gastronómico CR', 'Sabores de Costa Rica', '3-101-123456', 'RESTAURANTE', true),
                                                                                                      ('550e8400-e29b-41d4-a716-446655440002', 'CAFE001', 'Café del Valle S.A.', 'Café del Valle', '3-101-654321', 'CAFETERIA', true);

-- 2. Crear sucursales (cada una es un tenant)
INSERT INTO public.empresas_sucursales (id, empresa_id, codigo_sucursal, nombre_sucursal, schema_name, es_principal, activa) VALUES
-- Sucursales de Grupo Gastronómico CR
('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'SUC001', 'Sabores San José Centro', 'tenant_sj_centro', true, true),
('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'SUC002', 'Sabores Escazú', 'tenant_escazu', false, true),
('650e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', 'SUC003', 'Sabores Heredia', 'tenant_heredia', false, true),
-- Sucursales de Café del Valle
('650e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440002', 'CAFE01', 'Café del Valle - Multiplaza', 'tenant_cafe_mp', true, true),
('650e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440002', 'CAFE02', 'Café del Valle - Lincoln', 'tenant_cafe_lincoln', false, true);

-- 3. Crear usuarios globales con diferentes roles
INSERT INTO public.usuarios_global (id, email, password, nombre, apellidos, identificacion, activo) VALUES
-- Super Admin con acceso a múltiples empresas
('750e8400-e29b-41d4-a716-446655440001', 'admin@nathbit.com', '$2a$10$YourHashedPasswordHere', 'Ana', 'Rodríguez', '1-1234-5678', true),
-- Admin de una empresa específica
('750e8400-e29b-41d4-a716-446655440002', 'gerente@saborescr.com', '$2a$10$YourHashedPasswordHere', 'Carlos', 'Mendez', '2-0987-6543', true),
-- Jefe de Cajas con acceso a 2 sucursales
('750e8400-e29b-41d4-a716-446655440003', 'jefecajas@saborescr.com', '$2a$10$YourHashedPasswordHere', 'María', 'González', '3-1111-2222', true),
-- Cajeros (acceso a 1 sucursal)
('750e8400-e29b-41d4-a716-446655440004', 'cajero1@saborescr.com', '$2a$10$YourHashedPasswordHere', 'Juan', 'Pérez', '4-3333-4444', true),
('750e8400-e29b-41d4-a716-446655440005', 'cajero2@saborescr.com', '$2a$10$YourHashedPasswordHere', 'Laura', 'Jiménez', '5-5555-6666', true),
-- Meseros
('750e8400-e29b-41d4-a716-446655440006', 'mesero1@saborescr.com', '$2a$10$YourHashedPasswordHere', 'Pedro', 'Vargas', '6-7777-8888', true),
-- Admin de Café del Valle
('750e8400-e29b-41d4-a716-446655440007', 'admin@cafedelvalle.com', '$2a$10$YourHashedPasswordHere', 'Roberto', 'Mora', '7-9999-0000', true);

-- 4. Asignar usuarios a empresas con roles
INSERT INTO public.usuario_empresas (usuario_id, empresa_id, rol, es_propietario, activo) VALUES
-- Ana: Super Admin en ambas empresas
('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'SUPER_ADMIN', true, true),
('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', 'SUPER_ADMIN', true, true),
-- Carlos: Admin solo en Grupo Gastronómico
('750e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'ADMIN', false, true),
-- María: Jefe de Cajas en Grupo Gastronómico
('750e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', 'JEFE_CAJAS', false, true),
-- Cajeros en Grupo Gastronómico
('750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440001', 'CAJERO', false, true),
('750e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440001', 'CAJERO', false, true),
-- Mesero en Grupo Gastronómico
('750e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440001', 'MESERO', false, true),
-- Roberto: Admin en Café del Valle
('750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440002', 'ADMIN', true, true);

-- 5. Asignar permisos por sucursal
INSERT INTO public.usuario_sucursales (usuario_id, empresa_id, sucursal_id, puede_leer, puede_escribir, puede_eliminar, puede_aprobar, es_principal, activo) VALUES
-- Ana: Acceso total a todas las sucursales
('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440001', true, true, true, true, false, true),
('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440002', true, true, true, true, false, true),
('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440003', true, true, true, true, false, true),
('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', '650e8400-e29b-41d4-a716-446655440004', true, true, true, true, false, true),
('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440002', '650e8400-e29b-41d4-a716-446655440005', true, true, true, true, false, true),
-- Carlos: Admin con acceso a todas las sucursales de su empresa
('750e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440001', true, true, true, true, false, true),
('750e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440002', true, true, true, true, false, true),
('750e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440003', true, true, true, true, false, true),
-- María: Jefe de Cajas con acceso a 2 sucursales
('750e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440001', true, true, false, true, true, true),
('750e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440002', true, true, false, true, false, true),
-- Juan: Cajero solo en San José Centro
('750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440001', true, true, false, false, true, true),
-- Laura: Cajera solo en Escazú
('750e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440002', true, true, false, false, true, true),
-- Pedro: Mesero en Heredia
('750e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440003', true, true, false, false, true, true),
-- Roberto: Admin de Café del Valle
('750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440002', '650e8400-e29b-41d4-a716-446655440004', true, true, true, true, true, true),
('750e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440002', '650e8400-e29b-41d4-a716-446655440005', true, true, true, true, false, true);

-- 6. Configuración de detección automática por IP
INSERT INTO public.configuracion_acceso (sucursal_id, tipo_deteccion, configuracion, descripcion, prioridad) VALUES
-- IPs para San José Centro
('650e8400-e29b-41d4-a716-446655440001', 'IP_RANGE', '{"ip_inicio": "192.168.1.1", "ip_fin": "192.168.1.254"}', 'Red local San José Centro', 100),
-- IPs para Escazú
('650e8400-e29b-41d4-a716-446655440002', 'IP_RANGE', '{"ip_inicio": "192.168.2.1", "ip_fin": "192.168.2.254"}', 'Red local Escazú', 100),
-- Terminal específico en Heredia
('650e8400-e29b-41d4-a716-446655440003', 'TERMINAL_ID', '{"terminal_id": "POS-HER-001", "mac_address": "AA:BB:CC:DD:EE:01"}', 'Terminal POS principal Heredia', 150),
-- IPs para Café Multiplaza
('650e8400-e29b-41d4-a716-446655440004', 'IP_RANGE', '{"ip_inicio": "10.0.1.1", "ip_fin": "10.0.1.254"}', 'Red Café Multiplaza', 100);

-- 7. Crear los schemas/tenants si no existen
DO $$
    DECLARE
        r RECORD;
    BEGIN
        FOR r IN (SELECT DISTINCT schema_name FROM public.empresas_sucursales WHERE activa = true) LOOP
                EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', r.schema_name);
                RAISE NOTICE 'Schema % creado o verificado', r.schema_name;
            END LOOP;
    END $$;

-- 8. Actualizar la tabla de tenants existente para compatibilidad
INSERT INTO public.tenants (id, nombre, tipo, activo)
SELECT
    schema_name as id,
    nombre_sucursal as nombre,
    'SUCURSAL' as tipo,
    activa as activo
FROM public.empresas_sucursales
ON CONFLICT (id) DO UPDATE SET
                               nombre = EXCLUDED.nombre,
                               tipo = EXCLUDED.tipo,
                               activo = EXCLUDED.activo;

-- 9. Mensaje de resumen
DO $$
    BEGIN
        RAISE NOTICE '=== DATOS DE PRUEBA CREADOS ===';
        RAISE NOTICE '';
        RAISE NOTICE 'USUARIOS DE PRUEBA:';
        RAISE NOTICE '1. admin@nathbit.com - Super Admin (acceso a todas las empresas)';
        RAISE NOTICE '2. gerente@saborescr.com - Admin de Grupo Gastronómico';
        RAISE NOTICE '3. jefecajas@saborescr.com - Jefe de Cajas (2 sucursales)';
        RAISE NOTICE '4. cajero1@saborescr.com - Cajero San José Centro';
        RAISE NOTICE '5. cajero2@saborescr.com - Cajero Escazú';
        RAISE NOTICE '6. mesero1@saborescr.com - Mesero Heredia';
        RAISE NOTICE '7. admin@cafedelvalle.com - Admin Café del Valle';
        RAISE NOTICE '';
        RAISE NOTICE 'Password para todos: $2a$10$YourHashedPasswordHere';
        RAISE NOTICE '(Recuerda generar un hash real con BCrypt)';
    END $$;