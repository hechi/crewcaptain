CREATE TABLE kudos (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    person_id UUID NOT NULL REFERENCES persons(id),
    date DATE NOT NULL,
    text TEXT NOT NULL,
    tags TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kudos_user_id ON kudos(user_id);
CREATE INDEX idx_kudos_user_person ON kudos(user_id, person_id);
CREATE INDEX idx_kudos_user_date ON kudos(user_id, date DESC);
