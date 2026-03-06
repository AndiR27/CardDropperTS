-- Seed 5 default reusable PackSlots

-- Fixed rarity slots
INSERT INTO pack_slot (id, name, fixed_rarity) VALUES
  (1, 'fixedCommon',    'COMMON'),
  (2, 'fixedRare',      'RARE'),
  (3, 'fixedEpic',      'EPIC'),
  (4, 'fixedLegendary', 'LEGENDARY');

-- Random weighted slot
INSERT INTO pack_slot (id, name) VALUES
  (5, 'randomClassic');

INSERT INTO pack_slot_rarity_weights (pack_slot_id, rarity, weight) VALUES
  (5, 'COMMON',    70.0),
  (5, 'RARE',      25.0),
  (5, 'EPIC',       4.0),
  (5, 'LEGENDARY',  1.0);

-- Advance the sequence past seeded IDs
ALTER SEQUENCE pack_slot_seq RESTART WITH 51;
