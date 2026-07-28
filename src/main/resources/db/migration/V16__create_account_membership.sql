-- V16__create_account_membership.sql
CREATE TABLE account_membership (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      INTEGER     NOT NULL REFERENCES users(id),
    account_id   BIGINT      NOT NULL REFERENCES account(id),
    account_role VARCHAR(20) NOT NULL
                     CHECK (account_role IN ('OWNER', 'MEMBER')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, account_id)
);

CREATE INDEX idx_account_membership_user_id    ON account_membership(user_id);
CREATE INDEX idx_account_membership_account_id ON account_membership(account_id);

-- Backfill: cada usuario con cuenta hoy pasa a tener su membresía equivalente
INSERT INTO account_membership (user_id, account_id, account_role)
SELECT id, account_id, account_role
FROM users
WHERE account_id IS NOT NULL;