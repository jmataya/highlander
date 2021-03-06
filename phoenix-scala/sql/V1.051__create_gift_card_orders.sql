create table gift_card_orders (
    id integer primary key,
    cord_ref text not null,
    created_at generic_timestamp,
    foreign key (id) references gift_card_origins(id) on update restrict on delete restrict,
    foreign key (cord_ref) references cords(reference_number) on update restrict on delete restrict
);

create index gift_card_orders_idx on gift_card_orders (cord_ref);

create trigger set_gift_card_orders_id
    before insert
    on gift_card_orders
    for each row
    execute procedure set_gift_card_origin_id();

