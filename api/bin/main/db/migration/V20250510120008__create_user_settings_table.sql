CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    due_soon_days INTEGER NOT NULL DEFAULT 3,
    stale_one_on_one_days INTEGER NOT NULL DEFAULT 14,
    anniversary_lookahead_days INTEGER NOT NULL DEFAULT 30,
    theme VARCHAR(10) NOT NULL DEFAULT 'DARK',
    show_achievements BOOLEAN NOT NULL DEFAULT TRUE,
    notify_action_item_overdue BOOLEAN NOT NULL DEFAULT TRUE,
    notify_action_item_due_soon BOOLEAN NOT NULL DEFAULT TRUE,
    notify_stale_one_on_one BOOLEAN NOT NULL DEFAULT TRUE,
    notify_upcoming_anniversary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
