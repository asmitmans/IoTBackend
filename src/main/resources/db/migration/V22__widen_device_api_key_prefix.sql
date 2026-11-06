-- V22__widen_device_api_key_prefix.sql
ALTER TABLE device ALTER COLUMN api_key_prefix TYPE VARCHAR(16);