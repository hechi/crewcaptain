ALTER TABLE quick_notes ADD COLUMN attached_entry_id UUID REFERENCES one_on_one_entries(id);

CREATE INDEX idx_quick_notes_attached_entry ON quick_notes(attached_entry_id) WHERE attached_entry_id IS NOT NULL;
