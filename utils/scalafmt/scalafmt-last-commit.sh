#!/usr/bin/env bash
set -e

SCALAFMT_DIR=$(dirname $0)

source "${SCALAFMT_DIR}"/shared-functions.sh

cd "${ROOT_DIR}"

eval "${SCALAFMT_DIR}/scalafmt.sh $(scala_files_in_last_commit)"

echo "     To update latest commit, run"
echo "           git commit --all --amend --no-edit"
echo