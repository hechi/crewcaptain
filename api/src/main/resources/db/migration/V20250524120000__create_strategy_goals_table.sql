CREATE TABLE strategy_goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    target_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_strategy_goal_status CHECK (status IN ('ACTIVE', 'ACHIEVED', 'DROPPED')),
    CONSTRAINT chk_title_not_blank CHECK (trim(title) != '')
);

CREATE INDEX idx_strategy_goals_user_id ON strategy_goals(user_id);
CREATE INDEX idx_strategy_goals_user_id_status ON strategy_goals(user_id, status);
CREATE INDEX idx_strategy_goals_user_id_created_at ON strategy_goals(user_id, created_at DESC);
