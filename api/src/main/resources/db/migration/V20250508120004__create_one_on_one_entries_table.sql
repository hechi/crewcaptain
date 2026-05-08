CREATE TABLE one_on_one_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    meeting_date TIMESTAMP WITH TIME ZONE NOT NULL,
    notes_markdown TEXT,
    outcomes_markdown TEXT,
    sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_one_on_one_entries_user_person ON one_on_one_entries(user_id, person_id);
CREATE INDEX idx_one_on_one_entries_person_date ON one_on_one_entries(person_id, meeting_date DESC);
