#!/bin/bash

set -euo pipefail

function check_newer_parent() {
  PARENT_IMAGE="$1"
  # Get namespace, default to "library" if not found
  PARENT_IMAGE_NS="${PARENT_IMAGE%/*}"
  if [[ "$PARENT_IMAGE_NS" = "${PARENT_IMAGE}" ]]; then
    PARENT_IMAGE_NS="library"
  fi
  PARENT_IMAGE_REPO_CUT_NS="${PARENT_IMAGE#*/}"
  PARENT_IMAGE_REPO="${PARENT_IMAGE_REPO_CUT_NS%:*}"
  PARENT_IMAGE_TAG="${PARENT_IMAGE#*:}"

  PARENT_IMAGE_LAST_UPDATE="$( curl -sS "https://hub.docker.com/v2/namespaces/${PARENT_IMAGE_NS}/repositories/${PARENT_IMAGE_REPO}/tags/${PARENT_IMAGE_TAG}" | jq -r .last_updated )"
  if [[ "$PARENT_IMAGE_LAST_UPDATE" = "null" ]]; then
    echo "::error title='Invalid PARENT Image'::Could not find ${PARENT_IMAGE} in the registry"
    exit 1
  fi

  DERIVED_IMAGE="$2"
  # Get namespace, default to "library" if not found
  DERIVED_IMAGE_NS="${DERIVED_IMAGE%/*}"
  if [[ "${DERIVED_IMAGE_NS}" = "${DERIVED_IMAGE}" ]]; then
    DERIVED_IMAGE_NS="library"
  fi
  DERIVED_IMAGE_REPO="$( echo "${DERIVED_IMAGE%:*}" | cut -f2 -d/ )"
  DERIVED_IMAGE_TAG="${DERIVED_IMAGE#*:}"

  DERIVED_IMAGE_LAST_UPDATE="$( curl -sS "https://hub.docker.com/v2/namespaces/${DERIVED_IMAGE_NS}/repositories/${DERIVED_IMAGE_REPO}/tags/${DERIVED_IMAGE_TAG}" | jq -r .last_updated )"
  if [[ "$DERIVED_IMAGE_LAST_UPDATE" = "null" || "$DERIVED_IMAGE_LAST_UPDATE" < "$PARENT_IMAGE_LAST_UPDATE" ]]; then
    echo "Parent image $PARENT_IMAGE has a newer release ($PARENT_IMAGE_LAST_UPDATE), which is more recent than $DERIVED_IMAGE ($DERIVED_IMAGE_LAST_UPDATE)"
    return 0
  else
    echo "Parent image $PARENT_IMAGE ($PARENT_IMAGE_LAST_UPDATE) is older than $DERIVED_IMAGE ($DERIVED_IMAGE_LAST_UPDATE)"
    return 1
  fi
}

function check_newer_pkgs() {
  IMAGE="$1"
  PKGS="$2"

  docker run --rm -u 0 "${IMAGE}" sh -c "apt update >/dev/null 2>&1 && apt install -s ${PKGS}" | tee /proc/self/fd/2 | grep -q "0 upgraded"
  STATUS=$?

  if [[ $STATUS -eq 0 ]]; then
    echo "Base image $IMAGE has no updates for our custom installed packages"
    return 1
  else
    echo "Base image $IMAGE needs updates for our custom installed packages"
    return 0
  fi

  # TODO: In a future version of this script, we might want to include checking for other security updates,
  #       not just updates to the packages we installed.
  # grep security /etc/apt/sources.list > /tmp/security.list
  # apt-get update -oDir::Etc::Sourcelist=/tmp/security.list
  # apt-get dist-upgrade -y -oDir::Etc::Sourcelist=/tmp/security.list -oDir::Etc::SourceParts=/bin/false -s

}

function current_revision() {
  IMAGE="$1"
  IMAGE_NS_REPO="${IMAGE%:*}"
  IMAGE_TAG="${IMAGE#*:}"

  if [[ "$IMAGE_TAG" = "$IMAGE_NS_REPO" ]]; then
    >&2 echo "You must provide an image reference in the format [<namespace>/]<repo>:<tag>"
    exit 1
  fi

  case "$IMAGE_NS_REPO" in
      */*) :;; # namespace/repository syntax, leave as is
      *) IMAGE_NS_REPO="library/$IMAGE_NS_REPO";; # bare repository name (docker official image); must convert to namespace/repository syntax
  esac

  # Without such a token we may run into rate limits
  # OB 2024-09-16: for some reason using this token stopped working. Let's go without and see if we really fall into rate limits.
  # token=$( curl -s "https://auth.docker.io/token?service=registry.docker.io&scope=repository:$IMAGE_NS_REPO:pull" )

  ALL_TAGS="$(
    i=0
    while [ $? == 0 ]; do
        i=$((i+1))
        # OB 2024-09-16: for some reason using this token stopped working. Let's go without and see if we really fall into rate limits.
        # RESULT=$( curl -s -H "Authorization: Bearer $token" "https://registry.hub.docker.com/v2/repositories/$IMAGE_NS_REPO/tags/?page=$i&page_size=100" )
        RESULT=$( curl -s "https://registry.hub.docker.com/v2/repositories/$IMAGE_NS_REPO/tags/?page=$i&page_size=100" )
        if [[ $( echo "$RESULT" | jq '.message' ) != "null" ]]; then
          # If we run into an error on the first attempt, that means we have a problem.
          if [[ "$i" == "1" ]]; then
            >&2 echo "Error when retrieving tag data: $( echo "$RESULT" | jq '.message' )"
            exit 2
          # Otherwise it will just mean we reached the last page already
          else
            break
          fi
        else
          echo "$RESULT" | jq -r '."results"[]["name"]'
          # DEBUG:
          #echo "$RESULT" | >&2 jq -r '."results"[]["name"]'
        fi
    done
  )"

  # Note: if a former tag could not be found, it just might not exist already. Start new series with rev 0
  echo "$ALL_TAGS" | grep "${IMAGE_TAG}-r" | sed -e "s#${IMAGE_TAG}-r##" | sort -h | tail -n1 || echo "-1"
}
