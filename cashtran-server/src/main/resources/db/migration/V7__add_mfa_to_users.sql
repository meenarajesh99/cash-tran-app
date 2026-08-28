ALTER TABLE cashtran_user
    ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE cashtran_user
    ADD COLUMN mfa_secret VARCHAR(255);