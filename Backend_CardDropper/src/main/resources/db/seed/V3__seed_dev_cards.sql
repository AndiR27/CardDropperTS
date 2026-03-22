-- ===========================================
--   V3 : Seed cards and initial user
-- ===========================================

-- ====== INITIAL USER (update keycloak_id to match your Keycloak admin) ======
INSERT INTO ts_user (id, keycloak_id, username, email) VALUES
  (1, 'ab0e9a68-e0be-45b1-bdf8-ee2e944af9c5', 'zackrey', 'andi_ramiqi95@hotmail.com'),
  (2, '8387557f-7e60-4f54-a171-44e3cf22c195', 'admin', 'admin@admin.ch');

ALTER SEQUENCE ts_user_seq RESTART WITH 101;

-- ====== CARDS ======
INSERT INTO card (id, name, image_url, rarity, description, drop_rate, is_unique, creator_id, target_user_id) VALUES
  (1,  'Stofonde',       'dev/Stofonde_card.png', 'LEGENDARY', 'Le fond des abysses, incarné.',               0.5,  true, 1, NULL),
  (2,  'Duo Queue',      'dev/duoQ.png',          'RARE',      'Deux âmes liées par la ranked.',               5.0,  false, 1, NULL),
  (3,  'Rundown',        'dev/2_rundown.png',      'RARE',     'Un sprint vers la victoire.',                  15.0, false, 1, NULL),
  (4,  'CardDropper',    'dev/3_CardDropper.png',  'LEGENDARY', 'La carte originelle.',                         2.0,  true,  1, NULL),
  (5,  'Dragon Aon',     'dev/dragon_aon.png',     'LEGENDARY', 'Le dragon primordial, flamme éternelle.',      0.5,  false, 1, NULL),
  (6,  'Corbac',         'dev/corbac.png',         'COMMON',    'Messager sombre entre les mondes.',            5.0,  false, 1, NULL),
  (7,  'Accusé',         'dev/accuse.png',         'RARE',      'Pointé du doigt, mais innocent... ou pas.',   15.0, false, 1, NULL),
  (8,  'Arm',            'dev/arm.png',            'EPIC',      'La force brute au service du style.',          2.0,  false, 1, NULL),
  (9,  'Piou',           'dev/piou.png',           'COMMON',    'Petit mais redoutable.',                      15.0, false, 1, NULL),
  (10, 'Storm',          'dev/storm.png',          'COMMON',    'La tempête ne prévient jamais.',               2.0,  false, 1, NULL),
  (11, 'Prism',          'dev/prism.png',          'EPIC',      'Réfracte la lumière et la réalité.',           5.0,  true, 1, NULL),
  (12, 'Pack à Gogo',    'dev/pack_a_gogo.png',    'EPIC',      'Je donne un pack gratuit',                    10.0, true, 2, 2);

ALTER SEQUENCE card_seq RESTART WITH 101;

-- ====== USER 1 OWNS 3 COMMONS + 1 EPIC ======
INSERT INTO user_cards (user_id, card_id) VALUES
  (1, 6),   -- Corbac (COMMON)
  (1, 9),   -- Piou (COMMON)
  (1, 10),  -- Storm (COMMON)
  (1, 8),   -- Arm (EPIC)
  (1, 12),  -- Admin pack (EPIC, unique to admin)
  (2, 2),
  (2, 6),
  (1, 1);

