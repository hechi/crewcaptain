-- Add a manual display order to quick_notes so a user's personal (self-assigned) notes
-- can be reordered up/down. Lower values sort higher in the list; newest-on-top is the
-- default because new personal notes are inserted above the current minimum order.
ALTER TABLE quick_notes ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

-- Backfill existing notes per user so the current newest-first ordering is preserved:
-- within each user, the newest note (by created_at) gets the lowest order (0).
WITH ordered AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC, id) - 1 AS rn
    FROM quick_notes
)
UPDATE quick_notes q
SET display_order = ordered.rn
FROM ordered
WHERE q.id = ordered.id;

-- Index to make the personal-notes ordered read efficient.
CREATE INDEX idx_quick_notes_user_self_order
    ON quick_notes(user_id, display_order)
    WHERE self_assigned = TRUE;
