-- Add auto-execute AI commands toggle and command terminal custom prompt to user_settings
ALTER TABLE user_settings ADD COLUMN ai_auto_execute_commands BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user_settings ADD COLUMN command_terminal_prompt TEXT;
