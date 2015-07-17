create table orders (
    id bigint primary key,
    reference_number character varying(255) not null,
    customer_id integer,
    status character varying(255) not null,
    locked boolean default false,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    constraint valid_status check (status in ('cart','ordered','fraudHold','remorseHold','manualHold','canceled',
                                              'fulfillmentStarted','partiallyShipped','shipped'))
);

create index orders_customer_and_status_idx on orders (customer_id, status);

create trigger set_inventory_id_trigger
    before insert
    on orders
    for each row
    execute procedure set_inventory_event_id();

-- partial index ensures we never have more than 1 cart per customer
create unique index orders_has_only_one_cart on orders (customer_id, status)
    where status = 'cart';

