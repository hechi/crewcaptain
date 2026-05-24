CREATE TABLE quick_notes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    person_id UUID REFERENCES persons(id),
    text TEXT NOT NULL,
    sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'INBOX',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_quick_notes_user_id ON quick_notes(user_id);
CREATE INDEX idx_quick_notes_user_status ON quick_notes(user_id, status);
CREATE INDEX idx_quick_notes_user_person ON quick_notes(user_id, person_id);
CREATE INDEX idx_quick_notes_user_created ON quick_notes(user_id, created_at DESC);
