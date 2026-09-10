-- V19__drop_unused_account_api_key.sql

ALTER TABLE account DROP COLUMN api_key_hash;
ALTER TABLE account DROP COLUMN api_key_prefix;