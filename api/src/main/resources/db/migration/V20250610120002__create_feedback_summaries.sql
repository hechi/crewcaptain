-- Saved, manager-editable AI narrative summaries of a person's feedback over a period.
-- The most recent summary for a person is included in their Review Packet.
CREATE TABLE feedback_summaries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    person_id UUID NOT NULL REFERENCES persons(id),
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    content TEXT NOT NULL,
    response_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feedback_summaries_user_person ON feedback_summaries(user_id, person_id, created_at DESC);

-- Custom AI prompt for generating a narrative feedback summary.
ALTER TABLE user_settings ADD COLUMN feedback_summary_prompt TEXT;
