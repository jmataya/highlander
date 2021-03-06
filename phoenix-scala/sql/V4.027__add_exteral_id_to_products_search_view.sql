alter table products_search_view add column external_id text;

create or replace function insert_products_search_view_from_products_fn() returns trigger as $$
begin

  insert into products_search_view select
    new.id as id,
    f.id as product_id,
    context.name as context,
    f.attributes->>(s.attributes->'title'->>'ref') as title,
    f.attributes->>(s.attributes->'description'->>'ref') as description,
    f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
    f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
    f.attributes->>(s.attributes->'tags'->>'ref') as tags,
    link.skus as skus,
    albumLink.albums as albums,
    to_char(new.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
    f.attributes->>(s.attributes->'externalId'->>'ref') as external_id
    from products as p
    inner join object_contexts as context on (p.context_id = context.id)
    inner join object_forms as f on (f.id = p.form_id)
    inner join object_shadows as s on (s.id = p.shadow_id)
    left join product_sku_links_view as link on (link.product_id = p.id)
    left join product_album_links_view as albumLink on (albumLink.product_id = p.id)
    where p.id = new.id;

    return null;
end;
$$ language plpgsql;

-- object forms
create or replace function update_products_search_view_from_attrs_fn() returns trigger as $$
begin

  update products_search_view set
    product_id = subquery.product_id,
    title = subquery.title,
    description = subquery.description,
    active_from = subquery.active_from,
    active_to = subquery.active_to,
    tags = subquery.tags,
    external_id = subquery.external_id,
    archived_at = subquery.archived_at
    from (select
            p.id,
            f.id as product_id,
            f.attributes->>(s.attributes->'title'->>'ref') as title,
            f.attributes->>(s.attributes->'description'->>'ref') as description,
            f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
            f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
            f.attributes->>(s.attributes->'tags'->>'ref') as tags,
            to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
            f.attributes->>(s.attributes->'externalId'->>'ref') as external_id
        from products as p
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        where p.id = new.id) as subquery
    where subquery.id = products_search_view.id;

    return null;
end;
$$ language plpgsql;
