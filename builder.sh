#!/usr/bin/env bash

# Fail on unexported vars
set -ue

# Include common library
source ./common.sh

# Define Highlander directory
HIGHLANDER_PATH=$PWD

# Define helper functions
function write() {
    if $DEBUG; then
        echo -e "[BUILDER]" $1
    fi
}

function contains() {
    local n=$#
    local value=${!n}
    for ((i=1;i < $#;i++)) {
        if [ "${!i}" == "${value}" ]; then
            echo "y"
            return 0
        fi
    }
    echo "n"
    return 1
}

# Define command-line arguments
DEBUG=false
if [[ $# -ge 1 ]] && [[ $1 == "-debug" ]]; then
    DEBUG=true
fi

DEPLOY=false
if [[ $# -ge 1 ]] && [[ $1 == "-deploy" ]]; then
    DEPLOY=true
fi

DOCKER=false
if [[ $# -ge 1 ]] && [[ $1 == "-docker" ]]; then
    DOCKER=true
fi

# Define base branch via GitHub API
if [ "$BUILDKITE_PULL_REQUEST" != "false" ] ; then
    write "Fetching base branch for PR#$BUILDKITE_PULL_REQUEST via Github API..."
    GITHUB_BASE_URL=https://api.github.com/repos/FoxComm/highlander/pulls
    GITHUB_REQUEST_URL=$GITHUB_BASE_URL/$BUILDKITE_PULL_REQUEST?access_token=$GITHUB_API_TOKEN
    BASE_BRANCH_VALUE=$(curl -sS -XGET $GITHUB_REQUEST_URL | jq '.base.ref' | tr -d '"')
    BASE_BRANCH="origin/$BASE_BRANCH_VALUE"
else
    write "No pull request created, setting base branch to master"
    BASE_BRANCH="master"
fi

# Fetch origin quietly
git fetch origin -q

# Define changed projects
if [ "$DEPLOY" = true ] ; then
    # Changed projects defined in merge commit
    ALL_CHANGED=$(git log -m -1 --name-only --pretty="format:" | xargs -I{} dirname {} | uniq)
else
    # Changed projects defined in branch comparison
    ALL_CHANGED=$(git diff --name-only $BASE_BRANCH...$BUILDKITE_COMMIT | xargs -I{} dirname {} | uniq)
fi

# Make newlines the only separator
IFS=$'\n'
ALL_CHANGED=($ALL_CHANGED)
unset IFS

# Detect changed projects
CHANGED=()
if [[ ${#ALL_CHANGED[@]} -gt 0 ]]; then
    for CHANGE in ${ALL_CHANGED[@]}; do
        if [ $(contains "${PROJECTS[@]}" "$CHANGE") == "y" ]; then
            CHANGED+=($CHANGE)
        fi
    done
fi

# Build everything if script goes wrong
if [[ ${#CHANGED[@]} == 0 ]]; then
    write "No projects changed, building all by default"
    for PROJECT in ${PROJECTS[@]}; do
        CHANGED+=($PROJECT)
    done
fi

# Debug output
write "Changed projects (${#CHANGED[@]}):"
for item in "${CHANGED[@]}"
do
    write "\t ${item}"
done

# Build, test, dockerize, push
if [ "$DEBUG" = false ] && [ "$DEPLOY" = false ] ; then
    write "Building subprojects..."
    for PROJECT_DIR in "${CHANGED[@]}"
    do
        cd $PROJECT_DIR
        make build test
        if [ "$DOCKER" = true ] ; then
            make docker docker-push
        fi
        cd $HIGHLANDER_PATH
    done
fi

# Deploy
if [ "$DEPLOY" = true ] ; then
    write "Deploying subprojects..."
    for ID in "${!CHANGED[@]}"
    do
        eval ${APPLICATIONS[$i]}=true
    done

    cd tabernacle
    ansible-playbook -v -i inventory/static/dev ansible/deploy_atomic.yml --extra-vars "debug_mode=true"
fi
