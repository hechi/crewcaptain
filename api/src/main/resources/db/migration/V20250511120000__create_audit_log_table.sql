CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    person_id UUID REFERENCES persons(id) ON DELETE SET NULL,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_user_id_created_at ON audit_log(user_id, created_at DESC);
CREATE INDEX idx_audit_log_user_id_entity_type ON audit_log(user_id, entity_type);
CREATE INDEX idx_audit_log_user_id_action ON audit_log(user_id, action);
