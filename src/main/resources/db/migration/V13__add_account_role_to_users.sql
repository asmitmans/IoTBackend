-- V13__add_account_role_to_users.sql
ALTER TABLE users ADD COLUMN account_role VARCHAR(20);

ALTER TABLE users ADD CONSTRAINT users_account_role_check
    CHECK (account_role IN ('OWNER', 'MEMBER'));