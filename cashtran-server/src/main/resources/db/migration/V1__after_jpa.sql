BEGIN TRANSACTION;

DROP SEQUENCE IF EXISTS seq_transfer_id;


CREATE TABLE transfer_type (
                               transfer_type_id BIGSERIAL NOT NULL,
                               transfer_type_desc varchar(10) NOT NULL,
                               CONSTRAINT PK_transfer_type PRIMARY KEY (transfer_type_id)
);

CREATE TABLE transfer_status (
                                 transfer_status_id BIGSERIAL NOT NULL,
                                 transfer_status_desc varchar(10) NOT NULL,
                                 CONSTRAINT PK_transfer_status PRIMARY KEY (transfer_status_id)
);

CREATE TABLE cashtran_user (
                               user_id BIGSERIAL PRIMARY KEY,
                               username VARCHAR(50) NOT NULL UNIQUE,
                               password VARCHAR(200) NOT NULL,
                               email VARCHAR(100) NOT NULL UNIQUE,
                               activated BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE account (
                         account_id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL UNIQUE,
                         balance DECIMAL(13,2) NOT NULL,

                         CONSTRAINT FK_account_user
                             FOREIGN KEY(user_id)
                                 REFERENCES cashtran_user(user_id)
);

CREATE TABLE authority (
                           authority_id BIGSERIAL PRIMARY KEY,
                           authority_name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE cashtran_user_authority (
                                         user_id BIGINT NOT NULL,
                                         authority_id BIGINT NOT NULL,

                                         PRIMARY KEY(user_id, authority_id),

                                         FOREIGN KEY(user_id)
                                             REFERENCES cashtran_user(user_id),

                                         FOREIGN KEY(authority_id)
                                             REFERENCES authority(authority_id)
);

CREATE TABLE transfer (
                          transfer_id BIGSERIAL,
                          transfer_type_id BIGINT NOT NULL,
                          transfer_status_id BIGINT NOT NULL,
                          account_from BIGINT NOT NULL,
                          account_to BIGINT NOT NULL,
                          amount decimal(13, 2) NOT NULL,
                          CONSTRAINT PK_transfer PRIMARY KEY (transfer_id),
                          CONSTRAINT FK_transfer_account_from FOREIGN KEY (account_from) REFERENCES account (account_id),
                          CONSTRAINT FK_transfer_account_to FOREIGN KEY (account_to) REFERENCES account (account_id),
                          CONSTRAINT FK_transfer_transfer_status FOREIGN KEY (transfer_status_id) REFERENCES transfer_status (transfer_status_id),
                          CONSTRAINT FK_transfer_transfer_type FOREIGN KEY (transfer_type_id) REFERENCES transfer_type (transfer_type_id),
                          CONSTRAINT CK_transfer_not_same_account CHECK (account_from <> account_to),
                          CONSTRAINT CK_transfer_amount_gt_0 CHECK (amount > 0)
);


INSERT INTO transfer_status (transfer_status_desc) VALUES ('Pending');
INSERT INTO transfer_status (transfer_status_desc) VALUES ('Approved');
INSERT INTO transfer_status (transfer_status_desc) VALUES ('Rejected');

INSERT INTO transfer_type (transfer_type_desc) VALUES ('Request');
INSERT INTO transfer_type (transfer_type_desc) VALUES ('Send');

INSERT INTO authority (authority_name)
VALUES
    ('ROLE_USER'),
    ('ROLE_ADMIN') ON CONFLICT DO NOTHING;

COMMIT;


