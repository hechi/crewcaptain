-- Link an action item back to the quick note it was converted from, so a converted
-- TODO can be traced to its originating note (mirrors originating_entry_id for 1:1s).
ALTER TABLE action_items
    ADD COLUMN originating_quick_note_id UUID REFERENCES quick_notes(id) ON DELETE SET NULL;

CREATE INDEX idx_action_items_originating_quick_note
    ON action_items(originating_quick_note_id) WHERE originating_quick_note_id IS NOT NULL;
