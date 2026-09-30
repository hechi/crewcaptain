-- Feedback templates: manager-owned, reusable feedback survey definitions.
-- Questions (including type, labels, required flag, and single-level showIf rules)
-- are stored as a JSON document in the questions column, serialized by the adapter.
CREATE TABLE feedback_templates (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    questions TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feedback_templates_user_id ON feedback_templates(user_id);
CREATE INDEX idx_feedback_templates_user_updated ON feedback_templates(user_id, updated_at DESC);

-- Custom AI prompt for generating feedback templates from a manager's brief.
ALTER TABLE user_settings ADD COLUMN feedback_template_prompt TEXT;
