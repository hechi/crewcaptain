CREATE TABLE strategy_goal_pdp_goal_links (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    strategy_goal_id UUID NOT NULL REFERENCES strategy_goals(id) ON DELETE CASCADE,
    pdp_goal_id UUID NOT NULL REFERENCES pdp_goals(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_strategy_pdp_link UNIQUE (strategy_goal_id, pdp_goal_id)
);

CREATE INDEX idx_links_user_id ON strategy_goal_pdp_goal_links(user_id);
CREATE INDEX idx_links_strategy_goal_id ON strategy_goal_pdp_goal_links(strategy_goal_id);
CREATE INDEX idx_links_pdp_goal_id ON strategy_goal_pdp_goal_links(pdp_goal_id);
CREATE INDEX idx_links_person_id ON strategy_goal_pdp_goal_links(person_id);
