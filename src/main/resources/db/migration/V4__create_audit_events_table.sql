-- V4__create_audit_events_table.sql
-- Crear tabla de eventos de auditoría

CREATE TABLE IF NOT EXISTS audit_events (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            tenant_id VARCHAR(50) NOT NULL,
                                            username VARCHAR(100) NOT NULL,
                                            event_type VARCHAR(50) NOT NULL,
                                            event_date TIMESTAMP NOT NULL,
                                            ip_address VARCHAR(45),
                                            user_agent TEXT,
                                            details TEXT,
                                            success BOOLEAN NOT NULL DEFAULT true,
                                            error_message TEXT,
                                            resource_type VARCHAR(50),
                                            resource_id VARCHAR(255),
                                            old_value TEXT,
                                            new_value TEXT,
                                            request_method VARCHAR(10),
                                            request_url TEXT,
                                            response_status INTEGER,
                                            execution_time_ms BIGINT,
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            version BIGINT DEFAULT 0
);

-- Índices para mejorar performance en búsquedas comunes
CREATE INDEX idx_audit_events_username ON audit_events(username);
CREATE INDEX idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_events_event_date ON audit_events(event_date DESC);
CREATE INDEX idx_audit_events_success ON audit_events(success);
CREATE INDEX idx_audit_events_resource ON audit_events(resource_type, resource_id);
CREATE INDEX idx_audit_events_tenant_date ON audit_events(tenant_id, event_date DESC);

-- Índice compuesto para búsquedas de login fallidos recientes
CREATE INDEX idx_audit_events_failed_logins ON audit_events(username, event_type, event_date)
    WHERE event_type = 'LOGIN_FAILED' AND success = false;

-- Comentarios
COMMENT ON TABLE audit_events IS 'Tabla de eventos de auditoría del sistema';
COMMENT ON COLUMN audit_events.username IS 'Usuario que realizó la acción';
COMMENT ON COLUMN audit_events.event_type IS 'Tipo de evento (LOGIN, LOGOUT, CREATE, UPDATE, DELETE, etc.)';
COMMENT ON COLUMN audit_events.event_date IS 'Fecha y hora del evento';
COMMENT ON COLUMN audit_events.ip_address IS 'Dirección IP del cliente';
COMMENT ON COLUMN audit_events.user_agent IS 'User-Agent del navegador/cliente';
COMMENT ON COLUMN audit_events.details IS 'Detalles adicionales del evento';
COMMENT ON COLUMN audit_events.success IS 'Indica si la operación fue exitosa';
COMMENT ON COLUMN audit_events.error_message IS 'Mensaje de error si la operación falló';
COMMENT ON COLUMN audit_events.resource_type IS 'Tipo de recurso afectado (USUARIO, PRODUCTO, ORDEN, etc.)';
COMMENT ON COLUMN audit_events.resource_id IS 'ID del recurso afectado';
COMMENT ON COLUMN audit_events.old_value IS 'Valor anterior (para actualizaciones)';
COMMENT ON COLUMN audit_events.new_value IS 'Valor nuevo (para actualizaciones)';
COMMENT ON COLUMN audit_events.request_method IS 'Método HTTP (GET, POST, PUT, DELETE, etc.)';
COMMENT ON COLUMN audit_events.request_url IS 'URL de la petición';
COMMENT ON COLUMN audit_events.response_status IS 'Código de estado HTTP de la respuesta';
COMMENT ON COLUMN audit_events.execution_time_ms IS 'Tiempo de ejecución en milisegundos';