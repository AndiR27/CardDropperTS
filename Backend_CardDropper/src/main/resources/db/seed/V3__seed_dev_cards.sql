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
  (12, 'Pack à Gogo',    'dev/pack_a_gogo.png',    'EPIC',      'Je donne un pack gratuit',                    10.0, true, 2, 2),
  (13, 'Ombre',          'dev/corbac.png',         'COMMON',    'Une silhouette fugace dans la nuit.',          15.0, false, 1, NULL),
  (14, 'Éclair',         'dev/storm.png',          'RARE',      'Frappe vite, frappe fort.',                     5.0, false, 1, NULL),
  (15, 'Titan',          'dev/arm.png',            'LEGENDARY', 'Colosse parmi les mortels.',                    0.5, false, 2, NULL),
  (16, 'Mirage',         'dev/prism.png',          'RARE',      'Ce que tu vois n est pas réel.',                5.0, false, 1, NULL),
  (17, 'Faucon',         'dev/corbac.png',         'COMMON',    'Rapide et silencieux.',                        15.0, false, 1, NULL),
  (18, 'Volcan',         'dev/dragon_aon.png',     'EPIC',      'La lave coule dans ses veines.',                2.0, false, 2, NULL),
  (19, 'Spectre',        'dev/accuse.png',         'COMMON',    'Invisible mais toujours présent.',             15.0, false, 1, NULL),
  (20, 'Nexus',          'dev/3_CardDropper.png',  'RARE',      'Le point de convergence de tout.',              5.0, false, 1, NULL),
  (21, 'Crépuscule',     'dev/storm.png',          'EPIC',      'Entre la lumière et les ténèbres.',             2.0, false, 1, NULL),
  (22, 'Sentinelle',     'dev/arm.png',            'COMMON',    'Garde éternelle aux portes du monde.',         15.0, false, 2, NULL),
  (23, 'Vortex',         'dev/prism.png',          'LEGENDARY', 'Aspire tout sur son passage.',                  0.5, true,  1, NULL),
  (24, 'Fantôme',        'dev/piou.png',           'RARE',      'Un écho du passé.',                             5.0, false, 1, NULL),
  (25, 'Brasier',        'dev/dragon_aon.png',     'COMMON',    'Flamme modeste mais tenace.',                  15.0, false, 1, NULL),
  (26, 'Cyclone',        'dev/storm.png',          'EPIC',      'Rien ne résiste à sa rotation.',                2.0, false, 2, NULL),
  (27, 'Lueur',          'dev/Stofonde_card.png',  'RARE',      'Petite flamme dans l obscurité.',               5.0, false, 1, NULL),
  (28, 'Golem',          'dev/arm.png',            'COMMON',    'Forgé dans la pierre ancienne.',               15.0, false, 1, NULL),
  (29, 'Abysse',         'dev/Stofonde_card.png',  'EPIC',      'Les profondeurs appellent.',                    2.0, false, 1, NULL),
  (30, 'Duel',           'dev/duoQ.png',           'COMMON',    'Face à face, pas de fuite.',                   15.0, false, 2, NULL),
  (31, 'Étoile',         'dev/prism.png',          'RARE',      'Brille même dans le chaos.',                    5.0, false, 1, NULL),
  (32, 'Rugissement',    'dev/dragon_aon.png',     'LEGENDARY', 'Le cri qui fait trembler la terre.',            0.5, false, 1, NULL),
  (33, 'Chasseur',       'dev/2_rundown.png',      'COMMON',    'Traque sa proie sans relâche.',                15.0, false, 1, NULL),
  (34, 'Gardien',        'dev/accuse.png',         'EPIC',      'Protège ce qui doit l être.',                   2.0, false, 2, NULL),
  (35, 'Relique',        'dev/pack_a_gogo.png',    'RARE',      'Un artefact d un autre âge.',                   5.0, false, 1, NULL);

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

