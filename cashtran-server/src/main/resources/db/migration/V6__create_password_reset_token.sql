CREATE TABLE password_reset_token
(
    id         BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id    BIGINT      NOT NULL,
    expires_at TIMESTAMP   NOT NULL,

    CONSTRAINT fk_password_reset_token_user
        FOREIGN KEY (user_id)
            REFERENCES cashtran_user (user_id)
            ON DELETE CASCADE
);