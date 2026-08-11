-- V18__drop_legacy_user_account_columns.sql

ALTER TABLE users DROP CONSTRAINT users_account_id_fkey;
ALTER TABLE users DROP CONSTRAINT users_account_role_check;
ALTER TABLE users DROP COLUMN account_id;
ALTER TABLE users DROP COLUMN account_role;