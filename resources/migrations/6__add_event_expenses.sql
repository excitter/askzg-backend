CREATE TABLE event_expenses
(
    id              SERIAL  PRIMARY KEY,
    event_id        INT                  NOT NULL,
    payment_id      INT                  NOT NULL,
    auto_calculated BOOLEAN              NOT NULL  DEFAULT TRUE
) WITHOUT OIDS;

ALTER TABLE event_expenses
    ADD CONSTRAINT event_expenses_event_id_fk FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE;
ALTER TABLE event_expenses
    ADD CONSTRAINT event_expenses_payment_id_fk FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE;
