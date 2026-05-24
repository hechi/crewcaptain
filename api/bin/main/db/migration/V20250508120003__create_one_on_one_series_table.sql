CREATE TABLE one_on_one_series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    cadence_type VARCHAR(20) NOT NULL,
    custom_interval_days INTEGER,
    template_markdown TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_one_on_one_series_user_person UNIQUE (user_id, person_id)
);

CREATE INDEX idx_one_on_one_series_user_person ON one_on_one_series(user_id, person_id);
