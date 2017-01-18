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
      "name" = new.name
    where name = old.name;

  return null;
end;
$$ language plpgsql;