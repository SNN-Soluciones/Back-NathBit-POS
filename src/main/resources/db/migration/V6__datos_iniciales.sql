-- Empresa del sistema
INSERT INTO empresas (codigo, nombre, nombre_comercial, activa)
VALUES ('SISTEMA', 'SNN Soluciones', 'SNN Soluciones S.A.', true);

-- Empresa demo
INSERT INTO empresas (codigo, nombre, nombre_comercial, cedula_juridica, activa)
VALUES ('DEMO', 'Restaurante Demo', 'El Sabor Costarricense', '3-101-123456', true);

-- Sucursales demo
INSERT INTO sucursales (empresa_id, codigo, nombre, direccion, telefono, activa)
SELECT e.id, 'CENTRO', 'Sucursal Centro', 'San José, Avenida Central', '2222-3333', true
FROM empresas e WHERE e.codigo = 'DEMO';

INSERT INTO sucursales (empresa_id, codigo, nombre, direccion, telefono, activa)
SELECT e.id, 'ESCAZU', 'Sucursal Escazú', 'Escazú, Multiplaza', '2228-9999', true
FROM empresas e WHERE e.codigo = 'DEMO';

-- Usuario ROOT inicial
-- IMPORTANTE: Cambiar la contraseña en producción
-- Contraseña por defecto: Root@2024!
INSERT INTO usuarios (email, password, nombre, apellidos, telefono, activo)
VALUES (
           'root@snnsoluciones.com',
           '$2a$10$8K1p/a0dL0KmlGkteVW2CuVJJs0q3wG9Y.6VXh0UMT2F5MJ7yD7.6',
           'Root',
           'Sistema',
           '8888-8888',
           true
       );

-- Asignar rol ROOT
INSERT INTO usuarios_empresas_roles (usuario_id, empresa_id, rol, es_principal, activo, permisos)
SELECT
    u.id,
    e.id,
    'ROOT',
    true,
    true,
    '{
      "usuarios": {"ver": true, "crear": true, "editar": true, "eliminar": true},
      "empresas": {"ver": true, "crear": true, "editar": true, "eliminar": true},
      "sucursales": {"ver": true, "crear": true, "editar": true, "eliminar": true},
      "productos": {"ver": true, "crear": true, "editar": true, "eliminar": true},
      "ordenes": {"ver": true, "crear": true, "editar": true, "eliminar": true},
      "reportes": {"ver": true, "crear": true, "editar": true, "eliminar": true},
      "configuracion": {"ver": true, "crear": true, "editar": true, "eliminar": true}
    }'::jsonb
FROM usuarios u, empresas e
WHERE u.email = 'root@snnsoluciones.com' AND e.codigo = 'SISTEMA';