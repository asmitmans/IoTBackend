-- V14__add_join_code_to_account.sql
ALTER TABLE account ADD COLUMN join_code_hash VARCHAR(255);
ALTER TABLE account ADD COLUMN join_code_expires_at TIMESTAMPTZ;