-- Feedback links: public, shareable, unauthenticated feedback collectors for a person.
-- The questions column holds a JSON snapshot of the template at link creation time,
-- so later template edits/deletion never alter collected responses.
CREATE TABLE feedback_links (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    person_id UUID NOT NULL REFERENCES persons(id),
    token VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    questions TEXT NOT NULL DEFAULT '[]',
    label VARCHAR(200),
    request_submitter_info BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    source_template_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Token must be globally unique so public resolution can look up a single link.
CREATE UNIQUE INDEX idx_feedback_links_token ON feedback_links(token);
CREATE INDEX idx_feedback_links_user_person ON feedback_links(user_id, person_id, created_at DESC);

-- Feedback responses: submissions against a link. Answers and free text are stored
-- as JSON and encrypted at rest by the adapter (third-party opinion is sensitive).
CREATE TABLE feedback_responses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    person_id UUID NOT NULL REFERENCES persons(id),
    link_id UUID NOT NULL REFERENCES feedback_links(id),
    submitter_name VARCHAR(200),
    submitter_email VARCHAR(320),
    anonymous BOOLEAN NOT NULL DEFAULT TRUE,
    answers TEXT NOT NULL DEFAULT '[]',
    additional_comments TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    converted_to_type VARCHAR(20),
    converted_to_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feedback_responses_user_person ON feedback_responses(user_id, person_id, created_at DESC);
CREATE INDEX idx_feedback_responses_link ON feedback_responses(link_id);
