alter table products
    drop column date;
alter table products
    drop column paid;
alter table products
    drop column paid_amount;

CREATE TABLE product_participations
(
    id         SERIAL PRIMARY KEY,
    member_id  INT     NOT NULL,
    product_id INT     NOT NULL,
    paid       BOOLEAN NOT NULL DEFAULT FALSE
) WITHOUT OIDS;

ALTER TABLE product_participations
    ADD CONSTRAINT product_participations_member_id_fk FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE;
ALTER TABLE product_participations
    ADD CONSTRAINT product_participations_product_id_fk FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE;

delete
from products
where id > 0;

alter table payments
    drop column product_id;
ALTER TABLE payments
    add column product_participation_id int null;
ALTER TABLE payments
    ADD CONSTRAINT payments_product_participation_id_fk FOREIGN KEY (product_participation_id) REFERENCES product_participations (id) ON DELETE SET NULL;