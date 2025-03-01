ALTER TABLE payments
ADD COLUMN refraction_id INT NULL;

ALTER TABLE payments
ADD CONSTRAINT refraction_fk FOREIGN KEY (refraction_id) REFERENCES refractions(id) ON DELETE SET NULL;
