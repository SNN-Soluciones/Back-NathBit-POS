CREATE TABLE usuarios (
                          id BIGSERIAL PRIMARY KEY,
                          email VARCHAR(100) UNIQUE NOT NULL,
                          password VARCHAR(255) NOT NULL,
                          nombre VARCHAR(100) NOT NULL,
                          apellidos VARCHAR(100) NOT NULL,
                          telefono VARCHAR(20),
                          identificacion VARCHAR(50),
                          ultimo_acceso TIMESTAMP,
                          activo BOOLEAN DEFAULT true,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_identificacion ON usuarios(identificacion);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);