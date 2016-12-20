--- Rename tables
alter table skus rename to product_variants;
alter table variants rename to product_options;
alter table variant_variant_value_links rename to product_option__value_links;
alter table variant_value_sku_links rename to product_value__variant_links;
alter table product_variant_links rename to product__option_links;
alter table product_sku_links rename to product__variant_links;
alter table sku_album_links rename to variant_album_links;

-- Update kinds in object forms
update object_forms set kind = 'product-option' where kind = 'variant';
update object_forms set kind = 'product-value' where kind = 'variant-value';
update object_forms set kind = 'product-variant' where kind = 'sku';


-- Views
alter table product_sku_links_view rename to product__variant_links_view;

-- alter table product__variant_links_view rename column skus to variants;

-- fields
alter table order_line_items rename column sku_shadow_id to variant_shadow_id;
alter table order_line_items rename column sku_id to variant_id;
alter table cart_line_items rename column sku_id to variant_id;
alter table save_for_later rename column sku_id to variant_id;

update object_shadows set json_schema = 'variant' where json_schema = 'sku';

create or replace function update_object_schemas_insert_fn() returns trigger as $$
declare
  dep text;
  did int;
begin
  -- check deps are already exists in object_schemas
    foreach dep in array new.dependencies
      loop
        select id into did from object_schemas where "name" = dep;
         if did is null then
            raise exception 'ObjectSchema with name % doesn''t exist', dep;
         end if;
      end loop;
  --
  insert into object_full_schemas(id, kind, "name", "schema", created_at)
    values (new.id, new.kind, new.name, new.schema || get_definitions_for_object_schema(new.name), new.created_at);

  return null;
end;
$$ language plpgsql;

create or replace function update_object_schemas_update_fn() returns trigger as $$
declare
  dep text;
  did int;
begin
  -- check deps are already exists in object_schemas
    foreach dep in array new.dependencies
      loop
        select id into did from object_schemas where "name" = dep;
         if did is null then
            raise exception 'ObjectSchema with name % doesn''t exist', dep;
         end if;
      end loop;
  --
  update object_full_schemas
    set schema = new.schema ||get_definitions_for_object_schema(new.name),
      kind = new.kind,
      "name" = new.name;

  return null;
end;
$$ language plpgsql;

create trigger update_object_schemas_update
    after update on object_schemas
    for each row
    execute procedure update_object_schemas_update_fn();

drop trigger if exists update_object_schemas_insert on object_schemas;

create trigger update_object_schemas_insert
    after insert on object_schemas
    for each row
    execute procedure update_object_schemas_insert_fn();

update object_schemas set name = 'variant' where name = 'sku';
update object_schemas set kind = 'variant' where kind = 'sku';

-- notes

update notes set reference_type = 'variant' where reference_type = 'sku';