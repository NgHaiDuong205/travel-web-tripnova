-- 2026-09-30: cho phép review (posts) cho tour, xe và chuyến bay.
ALTER TABLE posts DROP CONSTRAINT IF EXISTS posts_entity_type_check;
ALTER TABLE posts ADD CONSTRAINT posts_entity_type_check
    CHECK (entity_type IN ('hotel', 'landmark', 'destination', 'tour', 'car', 'flight'));
