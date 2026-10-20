-- Add an optional lightweight tag to quick_notes for organizing and filtering notes.
-- Mirrors the tag concept already used by pinned_remember_items (max 30 chars).
ALTER TABLE quick_notes ADD COLUMN tag VARCHAR(30);

-- Index for efficient filtering of a user's notes by tag.
CREATE INDEX idx_quick_notes_user_tag ON quick_notes(user_id, tag) WHERE tag IS NOT NULL;
