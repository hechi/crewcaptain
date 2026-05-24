-- Add AI Assistant configuration columns to user_settings
ALTER TABLE user_settings
    ADD COLUMN ai_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN ai_api_base_url VARCHAR(500) DEFAULT NULL,
    ADD COLUMN ai_api_key VARCHAR(1000) DEFAULT NULL,
    ADD COLUMN ai_model_name VARCHAR(200) DEFAULT NULL,
    ADD COLUMN ai_privacy_mode BOOLEAN NOT NULL DEFAULT TRUE;
