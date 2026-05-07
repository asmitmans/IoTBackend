CREATE TABLE company (
    id              SERIAL          PRIMARY KEY,
    company_name    VARCHAR(100)    NOT NULL,
    api_key_hash    VARCHAR(255)    NOT NULL,
    api_key_prefix  VARCHAR(10)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id          SERIAL          PRIMARY KEY,
    username    VARCHAR(50)     UNIQUE NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    alias       VARCHAR(20),
    names       VARCHAR(100),
    surnames    VARCHAR(100),
    company_id  INTEGER         REFERENCES company(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id          SERIAL      PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id     INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     INTEGER     NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);
