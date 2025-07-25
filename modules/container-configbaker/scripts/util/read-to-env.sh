#!/usr/bin/env bash

set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

# Read from a target into environment variables.
# Parameters: $target
#             Case A) If $target is a file, simply source it.
#             Case B) If $target is a directory, parse dirs and files in it as variable names and file content as value
function read_to_env() {
  local target="$1"

  if [ -f "$target" ] && [ -r "$target" ]; then
    set -o allexport
    # shellcheck disable=SC1090
    source "$target"
    set +o allexport
  elif [ -d "$target" ] && [ -r "$target" ] && [ -x "$target" ]; then
    # Find all files
    FILES=$( find "$target" -type f -printf '%P\n' )
    for FILE in $FILES; do
      # Same as MPCONFIG does!
      VARNAME=$( echo "$FILE" | tr '[:lower:]' '[:upper:]' | tr '/' '_' )
      VARVAL=$( cat "$target/$FILE")

      # Use printf to create the variable in global scope
      printf -v "$VARNAME" '%s' "$VARVAL"
      export "${VARNAME?}"
    done
  else
    error "'$target' not a (readable) environment file or directory"
  fi
}
