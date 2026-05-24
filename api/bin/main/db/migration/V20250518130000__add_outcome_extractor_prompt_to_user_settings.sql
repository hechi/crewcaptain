-- Add outcome_extractor_prompt column to user_settings for customizable AI outcome extraction prompt
ALTER TABLE user_settings ADD COLUMN outcome_extractor_prompt TEXT;
