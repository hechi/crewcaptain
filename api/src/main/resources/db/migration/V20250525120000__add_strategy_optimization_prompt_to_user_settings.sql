-- Add strategy optimization prompt column to user_settings table
ALTER TABLE user_settings
    ADD COLUMN strategy_optimization_prompt TEXT;
