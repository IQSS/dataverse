#!/bin/bash

# This script is used to retrieve the next revision number for a given image reference.
# The image reference must be of the form "[<namespace>/]<repo>:<tag>", where the namespace will default to "library" if omitted.

set -eu

IMAGE=${1}
IMAGE_NS_REPO="$( echo "$IMAGE" | cut -d: -f1 )"
IMAGE_TAG="$( echo "$IMAGE" | cut -d: -f2 )"

if [[ "$IMAGE_TAG" == "$IMAGE_NS_REPO" ]]; then
  >&2 echo "You must provide an image reference in the format [<namespace>/]<repo>:<tag>"
  exit 1
fi

case "$IMAGE_NS_REPO" in
    */*) :;; # namespace/repository syntax, leave as is
    *) IMAGE_NS_REPO="library/$IMAGE_NS_REPO";; # bare repository name (docker official image); must convert to namespace/repository syntax
esac

# Without such a token we run into rate limits
token=$( curl -s "https://auth.docker.io/token?service=registry.docker.io&scope=repository:$IMAGE_NS_REPO:pull" )

ALL_TAGS="$(
  i=0
  while [ $? == 0 ]; do
      i=$((i+1))
      RESULT=$( curl -s -H "Authorization: Bearer $token" "https://registry.hub.docker.com/v2/repositories/$IMAGE_NS_REPO/tags/?page=$i&page_size=100" )
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

# If a former tag could not be found, it just might not exist already. Setting to -1, will be incremented to 0 to start a new series.
CURRENT=$( echo "$ALL_TAGS" | grep "${IMAGE_TAG}-r" | sed -e "s#${IMAGE_TAG}-r##" | sort -h | tail -n1 )
if [[ "$CURRENT" ]]; then
  echo "$((CURRENT+1))"
else
  echo "0"
fi
