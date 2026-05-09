CREATE TABLE action_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    person_id UUID NOT NULL REFERENCES persons(id),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    owner_type VARCHAR(20) NOT NULL DEFAULT 'MANAGER',
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    originating_entry_id UUID REFERENCES one_on_one_entries(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_owner_type CHECK (owner_type IN ('MANAGER', 'PERSON')),
    CONSTRAINT chk_status CHECK (status IN ('OPEN', 'DONE', 'CANCELED'))
);

CREATE INDEX idx_action_items_user_id ON action_items(user_id);
CREATE INDEX idx_action_items_user_person ON action_items(user_id, person_id);
CREATE INDEX idx_action_items_user_status ON action_items(user_id, status);
CREATE INDEX idx_action_items_due_date ON action_items(user_id, status, due_date);
