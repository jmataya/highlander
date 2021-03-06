-- General domains
create domain generic_string text check (length(value) <= 255);
create domain generic_timestamp timestamp without time zone default (now() at time zone 'utc');

-- ISO4217 declares currency as alphanumeric-3
create domain currency character(3) not null;

-- Region abbreviation
create domain region_abbr text check (length(value) <= 10);

-- RFC2821 + Errata 1690 limits max email size to 254 chars
create domain email text check (length(value) <= 254);

-- Using text instead of character varying is more efficient
create domain reference_number text check (length(value) <= 20);

-- Generic phone number
create domain phone_number text check (length(value) <= 15);

-- Zip code
create domain zip_code text check (
    length(value) > 0 and
    length(value) <= 12 and
    value ~ '(?i)^[a-z0-9][a-z0-9\- ]{0,10}[a-z0-9]$'
);

-- SKU Code
create domain sku_code text check (length(value) > 0);

-- Object Links
create domain object_link_type text check (value in ('productVariant', 'promotionDiscount', 'skuVariantValue',
                                                     'variantValue'));

-- Gift Cards
create domain gift_card_state text check (value in ('onHold', 'active', 'canceled', 'cart', 'fullyRedeemed'));
create domain gift_card_origin_type text check (value in ('customerPurchase', 'csrAppeasement', 'fromStoreCredit',
                                                          'rmaProcess'));

-- Store Credits
create domain store_credit_state text check (value in ('onHold', 'active', 'canceled', 'fullyRedeemed'));
create domain store_credit_origin_type text check (value in ('giftCardTransfer', 'csrAppeasement', 'rmaProcess',
                                                             'custom'));

-- Assignments
create domain assignment_type text check (value in ('assignee', 'watcher'));
create domain assignment_ref_type text check (value in ('order', 'giftCard', 'customer', 'return', 'product', 'sku',
                                                        'promotion', 'coupon'));

-- Notes
create domain note_body text check (length(value) > 0 and length(value) <= 1000);
create domain note_reference_type text check (value in ('order', 'giftCard', 'customer', 'return', 'product', 'sku',
                                                        'promotion', 'coupon', 'storeAdmin'));

-- Shared Searches
create domain shared_search_scope text check (value in ('customersScope', 'ordersScope', 'storeAdminsScope',
                                                        'giftCardsScope', 'productsScope', 'inventoryScope',
                                                        'promotionsScope', 'couponsScope', 'couponCodesScope',
                                                        'skusScope'));

-- Returns
create domain reason_type text check (value in('general', 'giftCardCreation', 'storeCreditCreation', 'cancellation'));
create domain return_reason_type text check (value in ('baseReason', 'productReturnCode'));
create domain return_type text check (value in ('standard', 'creditOnly', 'restockOnly'));
create domain return_state text check (value in ('pending', 'processing', 'review', 'complete', 'canceled'));
create domain return_inventory_disposition text check (value in ('putaway', 'damage', 'recovery', 'discontinued'));
create domain return_line_item_origin_type text check (value in ('skuItem', 'giftCardItem', 'shippingCost'));
