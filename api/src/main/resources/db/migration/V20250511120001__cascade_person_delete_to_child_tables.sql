-- Update FK constraints on child tables of persons to CASCADE on delete.
-- Without this, permanently deleting a person from trash would fail with
-- FK violations when the person has associated action items, PDP goals,
-- kudos, or quick notes. Note: pinned_remember_items, one_on_one_series,
-- and one_on_one_entries already cascade. notifications and audit_log
-- already set null. pdp_updates cascades via pdp_goals.

-- action_items.person_id
ALTER TABLE action_items DROP CONSTRAINT action_items_person_id_fkey;
ALTER TABLE action_items
    ADD CONSTRAINT action_items_person_id_fkey
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE;

-- pdp_goals.person_id
ALTER TABLE pdp_goals DROP CONSTRAINT pdp_goals_person_id_fkey;
ALTER TABLE pdp_goals
    ADD CONSTRAINT pdp_goals_person_id_fkey
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE;

-- kudos.person_id
ALTER TABLE kudos DROP CONSTRAINT kudos_person_id_fkey;
ALTER TABLE kudos
    ADD CONSTRAINT kudos_person_id_fkey
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE;

-- quick_notes.person_id (nullable FK - CASCADE also deletes the note)
ALTER TABLE quick_notes DROP CONSTRAINT quick_notes_person_id_fkey;
ALTER TABLE quick_notes
    ADD CONSTRAINT quick_notes_person_id_fkey
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE;
