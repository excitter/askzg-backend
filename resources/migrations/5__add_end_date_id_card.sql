ALTER TABLE events
ADD COLUMN IF NOT EXISTS end_date TIMESTAMP WITHOUT TIME ZONE;

UPDATE events SET end_date = date;

ALTER TABLE events ALTER COLUMN end_date SET NOT NULL;
ALTER TABLE events ADD CONSTRAINT event_start_before_end CHECK(end_date >= date);


ALTER TABLE members
ADD id_card_number CHARACTER VARYING(30) NULL;
