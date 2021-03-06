create table coupon_codes(
    id serial primary key,
    code generic_string not null,
    coupon_form_id integer not null references object_forms(id) on update restrict on delete restrict,
    created_at generic_timestamp
);

create index coupon_codes_coupon_form_idx on coupon_codes (coupon_form_id);
create unique index coupon_codex on coupon_codes (lower(code));

