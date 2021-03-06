create materialized view notes_gift_cards_view as
select
    n.id,
    -- Gift Card
    case when count(gc) = 0
    then
        null
    else
        to_json((gc.code, gc.origin_type, gc.currency, to_char(gc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_gift_cards)
    end as gift_card
from notes as n
left join gift_cards as gc on (n.reference_id = gc.id AND n.reference_type = 'giftCard')
group by n.id, gc.id
order by id;

create unique index notes_gift_cards_view_idx on notes_gift_cards_view (id);
