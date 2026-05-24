-- Add trend_radar_prompt column to user_settings for customizable AI Trend Radar system prompt
ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS trend_radar_prompt TEXT;
