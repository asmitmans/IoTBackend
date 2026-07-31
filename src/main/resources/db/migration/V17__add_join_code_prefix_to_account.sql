-- V17__add_join_code_prefix_to_account.sql
ALTER TABLE account ADD COLUMN join_code_prefix VARCHAR(10);
CREATE INDEX idx_account_join_code_prefix ON account(join_code_prefix);