-- Carry a note's tag over to the action item made from it, so the tag is not lost on
-- "make action item". Mirrors quick_notes.tag (max 30 chars).
ALTER TABLE action_items ADD COLUMN tag VARCHAR(30);

-- Backfill items that were already made from a tagged note.
UPDATE action_items a
SET tag = q.tag
FROM quick_notes q
WHERE a.originating_quick_note_id = q.id
  AND q.tag IS NOT NULL;
