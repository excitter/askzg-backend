ALTER TABLE payments
ADD COLUMN event_participation_id INT NULL;

ALTER TABLE payments
    ADD CONSTRAINT event_participation_fk FOREIGN KEY (event_participation_id) REFERENCES event_participation(id) ON DELETE SET NULL;
