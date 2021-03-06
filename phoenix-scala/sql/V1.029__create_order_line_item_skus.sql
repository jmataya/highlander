-- child table inheriting PK from concrete supertable order_line_item_origins
-- this provides a mapping to orders <--> skus as polymorphic line items
create table order_line_item_skus (
    id integer primary key,
    sku_id integer not null unique references skus(id) on update restrict on delete restrict,
    sku_shadow_id integer not null unique references object_shadows(id) on update restrict on delete restrict,
    foreign key (id) references order_line_item_origins(id) on update restrict on delete restrict
);

create trigger set_order_line_item_sku_id
    before insert
    on order_line_item_skus
    for each row
    execute procedure set_order_line_item_origin_id();

