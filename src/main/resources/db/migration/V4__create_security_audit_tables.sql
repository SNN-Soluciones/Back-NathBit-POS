-- Tabla para tokens en blacklist
CREATE TABLE IF NOT EXISTS token_blacklist (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    expiration_date TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL,
    reason VARCHAR(50),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT true,
    version BIGINT DEFAULT 0
    );

-- Índices para token_blacklist
CREATE INDEX idx_token_blacklist_token ON token_blacklist(token);
CREATE INDEX idx_token_blacklist_expiration ON token_blacklist(expiration_date);
CREATE INDEX idx_token_blacklist_username ON token_blacklist(username);

-- Tabla para eventos de auditoría
CREATE TABLE IF NOT EXISTS audit_events (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    username VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    details TEXT,
    success BOOLEAN NOT NULL DEFAULT true,
    error_message VARCHAR(500),
    affected_resource VARCHAR(255),
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT true,
    version BIGINT DEFAULT 0
    );

-- Índices para audit_events
CREATE INDEX idx_audit_username ON audit_events(username);
CREATE INDEX idx_audit_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_event_date ON audit_events(event_date);
CREATE INDEX idx_audit_tenant_date ON audit_events(tenant_id, event_date);

-- Comentarios
COMMENT ON TABLE token_blacklist IS 'Almacena tokens JWT invalidados por logout o revocación';
COMMENT ON TABLE audit_events IS 'Registro de eventos de auditoría del sistema';