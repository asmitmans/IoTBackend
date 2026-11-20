ALTER TABLE location ADD COLUMN public_id UUID;

UPDATE location SET public_id = gen_random_uuid() WHERE public_id IS NULL;

ALTER TABLE location ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE location ADD CONSTRAINT location_public_id_key UNIQUE (public_id);