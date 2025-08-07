CREATE TABLE empresas (
                          id BIGSERIAL PRIMARY KEY,
                          codigo VARCHAR(50) UNIQUE NOT NULL,
                          nombre VARCHAR(200) NOT NULL,
                          nombre_comercial VARCHAR(200),
                          cedula_juridica VARCHAR(50),
                          telefono VARCHAR(20),
                          email VARCHAR(100),
                          direccion TEXT,
                          configuracion JSONB DEFAULT '{}'::jsonb,
                          activa BOOLEAN DEFAULT true,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_empresas_codigo ON empresas(codigo);
CREATE INDEX idx_empresas_activa ON empresas(activa);