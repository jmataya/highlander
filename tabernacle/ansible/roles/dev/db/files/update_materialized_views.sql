-- store_credit_transactions
refresh materialized view concurrently store_credit_transactions_admins_view;

-- failed authorizations
refresh materialized view concurrently failed_authorizations_search_view;
