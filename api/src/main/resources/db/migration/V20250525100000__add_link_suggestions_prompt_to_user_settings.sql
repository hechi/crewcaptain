-- Add link_suggestions_prompt column to user_settings table for AI Link Suggestions feature
ALTER TABLE user_settings
ADD COLUMN IF NOT EXISTS link_suggestions_prompt TEXT;
