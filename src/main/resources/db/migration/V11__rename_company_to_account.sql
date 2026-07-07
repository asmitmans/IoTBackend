-- V11__rename_company_to_account.sql

ALTER TABLE company RENAME TO account;
ALTER TABLE account RENAME COLUMN company_name TO account_name;

ALTER TABLE account RENAME CONSTRAINT company_pkey TO account_pkey;
ALTER SEQUENCE company_id_seq RENAME TO account_id_seq;

-- 2. users.company_id -> users.account_id
ALTER TABLE users RENAME COLUMN company_id TO account_id;
ALTER TABLE users RENAME CONSTRAINT users_company_id_fkey TO users_account_id_fkey;

-- 3. device.company_id -> device.account_id
ALTER TABLE device RENAME COLUMN company_id TO account_id;
ALTER TABLE device RENAME CONSTRAINT device_company_id_fkey TO device_account_id_fkey;
ALTER INDEX idx_device_company_id RENAME TO idx_device_account_id;

-- 4. location.company_id -> location.account_id
ALTER TABLE location RENAME COLUMN company_id TO account_id;
ALTER TABLE location RENAME CONSTRAINT location_company_id_fkey TO location_account_id_fkey;
ALTER TABLE location RENAME CONSTRAINT location_company_id_parent_id_name_key TO location_account_id_parent_id_name_key;
ALTER INDEX idx_location_company_id RENAME TO idx_location_account_id;

-- 5. measurement.company_id -> measurement.account_id
ALTER TABLE measurement RENAME COLUMN company_id TO account_id;
ALTER INDEX idx_measurement_company_recorded RENAME TO idx_measurement_account_recorded;

-- 6. device_command.company_id -> device_command.account_id
ALTER TABLE device_command RENAME COLUMN company_id TO account_id;