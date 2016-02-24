create materialized view order_line_items_view as
select
    o.id as order_id,
    count(sku.id) as count,
    case when count(sku) = 0
    then
        '[]'
    else
        json_agg((oli.state, 
                sku.sku, 
                product.attributes->'name'->>(product_shadow.attributes->>'name'), 
                sku.attributes->'price'->(sku_shadow.attributes->>'price')->>'value')::export_line_items)
    end as items
from orders as o
left join order_line_items as oli on (o.id = oli.order_id)
left join order_line_item_origins as oli_origins on (oli.origin_id = oli_origins.id)
left join order_line_item_skus as oli_skus on (oli_origins.id = oli_skus.id)
left join skus as sku on (oli_skus.sku_id = sku.id)
left join sku_shadows as sku_shadow on (oli_skus.sku_shadow_id = sku_shadow.id)
left join products as product on (sku.product_id = product.id)
left join product_shadows as product_shadow on (product.id = product_shadow.product_id and sku_shadow.product_context_id = product_shadow.product_context_id)
group by o.id;

create unique index order_line_items_view_idx on order_line_items_view (order_id);
