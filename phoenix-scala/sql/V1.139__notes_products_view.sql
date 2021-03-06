create materialized view notes_products_view as
select
    n.id,
    -- Product
    case when count(p) = 0
    then
        null
    else
        to_json((
            f.id,
            f.attributes,
            to_char(p.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
        )::export_products_raw)
    end as product
from notes as n
left join products as p on (p.form_id = n.reference_id and n.reference_type = 'product')
left join object_forms as f on (f.id = p.form_id and f.kind = 'product')
group by n.id, p.id, f.id
order by id;

create unique index notes_products_view_idx on notes_products_view (id);
