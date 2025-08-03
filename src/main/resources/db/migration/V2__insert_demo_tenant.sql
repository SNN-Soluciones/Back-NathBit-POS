-- Migración para insertar tenant de demostración
-- V2__insert_demo_tenant.sql

-- Insertar tenant DEMO
INSERT INTO tenants (
    codigo,
    nombre,
    nombre_comercial,
    identificacion,
    tipo_identificacion,
    email,
    telefono,
    direccion,
    provincia,
    canton,
    distrito,
    activo
) VALUES (
             'demo',
             'Restaurante Demo S.A.',
             'La Casa del Sabor',
             '3-101-123456',
             'JURIDICA',
             'demo@nathbitpos.com',
             '2222-2222',
             'Avenida Central, Local 123',
             'San José',
             'Central',
             'Carmen',
             true
         );

-- Insertar tenant para pruebas adicionales
INSERT INTO tenants (
    codigo,
    nombre,
    nombre_comercial,
    identificacion,
    tipo_identificacion,
    email,
    telefono,
    direccion,
    provincia,
    canton,
    distrito,
    activo
) VALUES (
             'tenant1',
             'Comidas Rápidas XYZ',
             'Burger Palace',
             '3-101-789012',
             'JURIDICA',
             'tenant1@nathbitpos.com',
             '2333-3333',
             'Centro Comercial Plaza, Local 45',
             'San José',
             'Escazú',
             'San Rafael',
             true
         );