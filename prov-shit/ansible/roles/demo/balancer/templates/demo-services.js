upstream storefront {
  {{ '{{' }} range service "storefront" {{ '}}'}} server {{ '{{'}} .Address {{ '}}' }}:{{ '{{' }} .Port {{ '}}' }} max_fails=10 fail_timeout=30s weight=1;
  {{ '{{' }} else {{ '}}' }} server {{storefront_server}} fail_timeout=30s max_fails=10; {{ '{{' }} end {{ '}}' }}
}
