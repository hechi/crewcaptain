CREATE TABLE pdp_updates (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES pdp_goals(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    text_markdown TEXT NOT NULL,
    sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pdp_updates_goal_id ON pdp_updates(goal_id);
CREATE INDEX idx_pdp_updates_user_id ON pdp_updates(user_id);
