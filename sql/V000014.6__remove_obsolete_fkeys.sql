-- remove obsolete foreign keys
alter table stock_item_summaries drop constraint stock_item_summaries_stock_item_id_fkey1;
alter table stock_item_units drop constraint stock_item_units_stock_item_id_fkey1;
alter table shipments drop constraint shipments_address_id_fkey1;
alter table shipment_line_items drop constraint shipment_line_items_shipment_id_fkey1;
alter table addresses drop constraint addresses_region_id_fkey1;
