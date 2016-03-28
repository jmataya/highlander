create materialized view sku_search_view as
select
    sku.id, 
    sku.code as code,
    context.name as context,
    sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref') as title,
    sku_form.attributes->(sku_shadow.attributes->'price'->>'ref')->>'value' as price
from skus as sku, object_forms as sku_form, object_shadows as sku_shadow, object_contexts as context 
where sku_shadow.id = sku.shadow_id and sku_form.id = sku.form_id and sku.context_id = context.id;

create unique index sku_search_view_idx on sku_search_view (id, context);
