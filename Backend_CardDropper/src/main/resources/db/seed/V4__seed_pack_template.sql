-- ===========================================
--   V4 : Seed a default pack template
-- ===========================================

INSERT INTO pack_template (id, name) VALUES
  (1, 'Pack Classique');

INSERT INTO pack_template_slot (id, pack_template_id, pack_slot_id, count) VALUES
  (1, 1, 5, 3),  -- 3x randomClassic (weighted)
  (2, 1, 2, 1);  -- 1x fixedRare (guaranteed rare)

ALTER SEQUENCE pack_template_seq RESTART WITH 101;
ALTER SEQUENCE pack_template_slot_seq RESTART WITH 101;
