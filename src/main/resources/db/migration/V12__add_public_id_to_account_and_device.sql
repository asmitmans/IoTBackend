-- 1. account.public_id
ALTER TABLE account ADD COLUMN public_id UUID;

UPDATE account SET public_id = gen_random_uuid() WHERE public_id IS NULL;

ALTER TABLE account ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE account ADD CONSTRAINT account_public_id_key UNIQUE (public_id);

-- 2. device.public_id
ALTER TABLE device ADD COLUMN public_id UUID;

UPDATE device SET public_id = gen_random_uuid() WHERE public_id IS NULL;

ALTER TABLE device ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE device ADD CONSTRAINT device_public_id_key UNIQUE (public_id);