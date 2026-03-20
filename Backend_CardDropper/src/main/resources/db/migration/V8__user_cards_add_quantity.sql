-- ===========================================
--   V8 : Add quantity column to user_cards
--   Collapse duplicate rows into one with quantity = count
--   Add UNIQUE constraint on (user_id, card_id)
-- ===========================================

-- Step 1: Add quantity column (all existing rows default to 1)
ALTER TABLE user_cards ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1;

-- Step 2: For each (user_id, card_id) group with duplicates,
--         update the surviving row (MIN id) with the total count
UPDATE user_cards uc
SET quantity = subq.cnt
FROM (
    SELECT MIN(id) AS keep_id, COUNT(*) AS cnt
    FROM user_cards
    GROUP BY user_id, card_id
    HAVING COUNT(*) > 1
) subq
WHERE uc.id = subq.keep_id;

-- Step 3: Delete all duplicate rows (keep only the one with MIN id per group)
DELETE FROM user_cards
WHERE id NOT IN (
    SELECT MIN(id)
    FROM user_cards
    GROUP BY user_id, card_id
);

-- Step 4: Add unique constraint to enforce one row per (user, card) pair
ALTER TABLE user_cards ADD CONSTRAINT uq_user_cards_user_card UNIQUE (user_id, card_id);
