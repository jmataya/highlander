create table orders (
    inventory_event_id integer not null,
    customer_id integer,
    status character varying(255) not null,
    locked int,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

create sequence orders_id_seq
    start with 1
    increment by 1
    no minvalue
    no maxvalue
    cache 1;

alter table only orders
    add constraint orders_pkey primary key (id);

alter table only orders
    alter column id set default nextval('orders_id_seq'::regclass);

alter table only purchase_orders_receipts
      add constraint purchase_order_receipts_pkey primary key (inventory_adjustment_id);

alter table only purchase_order_receipts
    add constraint purchase_order_receipts_inventory_adjustment_id_fk foreign key (inventory_adjustment_id) references inventory_adjustments (id) on update restrict on delete cascade;