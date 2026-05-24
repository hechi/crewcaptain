CREATE TABLE persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    preferred_name VARCHAR(255),
    role_title VARCHAR(255),
    timezone VARCHAR(100),
    start_date DATE,
    email VARCHAR(255),
    tags TEXT[] DEFAULT '{}',
    morale_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    morale_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_persons_user_id ON persons(user_id);
CREATE INDEX idx_persons_morale_status ON persons(user_id, morale_status);
