-- Add self_assigned flag to quick_notes table.
-- When true, the note belongs to the manager themselves (not linked to any person they manage).
-- Invariant: self_assigned = true implies person_id IS NULL.
ALTER TABLE quick_notes ADD COLUMN self_assigned BOOLEAN NOT NULL DEFAULT FALSE;

-- Index for efficient lookup of self-assigned notes per user
CREATE INDEX idx_quick_notes_user_self_assigned ON quick_notes(user_id, self_assigned) WHERE self_assigned = TRUE;
