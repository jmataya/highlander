#!/bin/bash

export HOME=/root
export PHOENIX_URL=http://{{ phoenix_server }}
export NODE_ENV={{ashes_env}}
export DEMO_USER={{demo_user}}
export DEMO_PASS={{demo_pass}}
export PHOENIX_PUBLIC_KEY={{ public_keys_dest_dir }}/public_key.pem
export GA_TRACKING_ID={{ashes_ga_tracking_id}}
export FIREBIRD_LANGUAGE={{firebird_default_language}}
export STRIPE_PUBLISHABLE_KEY={{stripe_publishable_key}}

cd {{ ashes_install_dir }}
gulp server