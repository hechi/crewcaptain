-- Add AI writing style preference column to user_settings
ALTER TABLE user_settings
    ADD COLUMN ai_writing_style VARCHAR(50) NOT NULL DEFAULT 'NARRATIVE';
