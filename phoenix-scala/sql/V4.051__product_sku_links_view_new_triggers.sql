create trigger update_products_catalog_view_on_variants_skus
after update or insert or delete on variant_value_sku_links
for each row
execute procedure update_product_sku_links_view_from_products_and_deps_fn();
