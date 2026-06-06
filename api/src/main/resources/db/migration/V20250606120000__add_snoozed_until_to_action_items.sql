-- Add snoozed_until column for triage queue snooze functionality
ALTER TABLE action_items ADD COLUMN snoozed_until TIMESTAMPTZ;

-- Index for efficiently filtering snoozed items
CREATE INDEX idx_action_items_snoozed_until
    ON action_items (user_id, snoozed_until)
    WHERE snoozed_until IS NOT NULL AND status = 'OPEN';

-- Add triage AI hint prompt to user settings
ALTER TABLE user_settings ADD COLUMN triage_hint_prompt TEXT;
