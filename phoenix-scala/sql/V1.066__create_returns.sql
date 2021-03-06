create table returns (
    id serial primary key,
    reference_number reference_number not null unique,
    order_id integer not null references orders(id) on update restrict on delete restrict,
    order_ref text not null references orders(reference_number) on update restrict on delete restrict,
    return_type return_type not null,
    state return_state not null,
    is_locked boolean default false,
    message_to_customer text null,
    customer_id integer not null references customers(id) on update restrict on delete restrict,
    store_admin_id integer null references store_admins(id) on update restrict on delete restrict,
    canceled_reason integer null references reasons(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);

create index returns_order_ref_and_state on returns (order_ref, state);

create function next_return_id(order_id integer) returns bigint as $$
    select count(*)+1 as return_count from "returns" where order_id=$1;
$$ language 'sql';

create function set_returns_reference_number() returns trigger as $$
begin
    if length(new.reference_number) = 0 then
        new.reference_number = concat(new.order_ref, '.', next_return_id(new.order_id)::text);
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_returns_reference_number_trg
    before insert
    on returns
    for each row
    execute procedure set_returns_reference_number();