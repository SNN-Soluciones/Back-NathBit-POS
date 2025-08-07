CREATE TABLE sucursales (
                            id BIGSERIAL PRIMARY KEY,
                            empresa_id BIGINT NOT NULL,
                            codigo VARCHAR(50) NOT NULL,
                            nombre VARCHAR(200) NOT NULL,
                            direccion TEXT,
                            telefono VARCHAR(20),
                            email VARCHAR(100),
                            configuracion JSONB DEFAULT '{}'::jsonb,
                            activa BOOLEAN DEFAULT true,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_sucursal_empresa FOREIGN KEY (empresa_id)
                                REFERENCES empresas(id) ON DELETE CASCADE,
                            CONSTRAINT uk_sucursal_codigo_empresa UNIQUE (empresa_id, codigo)
);

CREATE INDEX idx_sucursales_empresa ON sucursales(empresa_id);
CREATE INDEX idx_sucursales_codigo ON sucursales(codigo);
CREATE INDEX idx_sucursales_activa ON sucursales(activa);
