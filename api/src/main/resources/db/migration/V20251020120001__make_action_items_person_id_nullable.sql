-- Allow person-less action items so managers can convert self-assigned quick notes
-- into personal TODOs that are not tied to any managed person.
-- A NULL person_id denotes a manager's own task. The existing foreign key to
-- persons(id) is preserved and simply permits NULL values.
ALTER TABLE action_items ALTER COLUMN person_id DROP NOT NULL;
