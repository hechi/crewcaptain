CREATE TABLE pdp_goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    person_id UUID NOT NULL REFERENCES persons(id),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    target_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_pdp_goal_status CHECK (status IN ('ACTIVE', 'ACHIEVED', 'PAUSED', 'DROPPED'))
);

CREATE INDEX idx_pdp_goals_user_id ON pdp_goals(user_id);
CREATE INDEX idx_pdp_goals_user_person ON pdp_goals(user_id, person_id);
CREATE INDEX idx_pdp_goals_user_status ON pdp_goals(user_id, status);
