CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_id VARCHAR(255),
    person_id UUID REFERENCES persons(id) ON DELETE SET NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for listing notifications by user (most common query)
CREATE INDEX idx_notifications_user_id_created_at ON notifications(user_id, created_at DESC);

-- Index for unread count query
CREATE INDEX idx_notifications_user_id_unread ON notifications(user_id) WHERE read_at IS NULL;

-- Index for deduplication check
CREATE INDEX idx_notifications_user_id_type_reference_id ON notifications(user_id, type, reference_id, created_at);
