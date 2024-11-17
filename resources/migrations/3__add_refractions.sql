CREATE TABLE refractions
(
    id         SERIAL PRIMARY KEY,
    member_id  INT                         NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    type       VARCHAR(30)                 NOT NULL,
    comment    VARCHAR(255)                NOT NULL,
    paid       BOOLEAN                     NOT NULL DEFAULT FALSE
) WITHOUT OIDS;

ALTER TABLE refractions
    ADD CONSTRAINT refractions_member_id_fk FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE;
