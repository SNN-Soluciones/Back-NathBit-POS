CREATE TABLE usuarios_empresas_roles (
                                         id BIGSERIAL PRIMARY KEY,
                                         usuario_id BIGINT NOT NULL,
                                         empresa_id BIGINT NOT NULL,
                                         sucursal_id BIGINT,
                                         rol VARCHAR(30) NOT NULL,
                                         permisos JSONB DEFAULT '{}'::jsonb,
                                         es_principal BOOLEAN DEFAULT false,
                                         activo BOOLEAN DEFAULT true,
                                         fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         asignado_por BIGINT,
                                         fecha_vencimiento TIMESTAMP,
                                         notas TEXT,
                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT fk_uer_usuario FOREIGN KEY (usuario_id)
                                             REFERENCES usuarios(id) ON DELETE CASCADE,
                                         CONSTRAINT fk_uer_empresa FOREIGN KEY (empresa_id)
                                             REFERENCES empresas(id) ON DELETE CASCADE,
                                         CONSTRAINT fk_uer_sucursal FOREIGN KEY (sucursal_id)
                                             REFERENCES sucursales(id) ON DELETE CASCADE,
                                         CONSTRAINT fk_uer_asignado_por FOREIGN KEY (asignado_por)
                                             REFERENCES usuarios(id) ON DELETE SET NULL,
                                         CONSTRAINT uk_usuario_empresa_sucursal UNIQUE (usuario_id, empresa_id, sucursal_id),
                                         CONSTRAINT chk_rol_valido CHECK (rol IN ('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO', 'COCINA'))
);

CREATE INDEX idx_uer_usuario ON usuarios_empresas_roles(usuario_id);
CREATE INDEX idx_uer_empresa ON usuarios_empresas_roles(empresa_id);
CREATE INDEX idx_uer_sucursal ON usuarios_empresas_roles(sucursal_id);
CREATE INDEX idx_uer_rol ON usuarios_empresas_roles(rol);
CREATE INDEX idx_uer_activo ON usuarios_empresas_roles(activo);
CREATE INDEX idx_uer_principal ON usuarios_empresas_roles(es_principal);