create table inventory_synchronizations (
    id bigint primary key,
    source generic_string,
    reference_number generic_string,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict
);

create trigger set_inventory_id_trigger
    before insert
    on inventory_synchronizations
    for each row
    execute procedure set_inventory_event_id();

