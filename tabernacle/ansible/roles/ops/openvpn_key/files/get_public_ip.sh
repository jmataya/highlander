#!/bin/sh

set -euo pipefail

curl -H"Metadata-Flavor: Google" http://metadata/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip
