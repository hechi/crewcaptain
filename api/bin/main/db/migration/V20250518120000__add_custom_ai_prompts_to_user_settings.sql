-- Add customizable AI prompt columns to user_settings
-- These allow managers to override the default system prompts for AI coaching features.
ALTER TABLE user_settings
    ADD COLUMN kudos_refinement_prompt TEXT DEFAULT NULL,
    ADD COLUMN pdp_optimization_prompt TEXT DEFAULT NULL,
    ADD COLUMN agenda_prep_prompt TEXT DEFAULT NULL,
    ADD COLUMN narrative_prompt TEXT DEFAULT NULL;
