#!/usr/bin/env bash

function error {
    echo "ERROR:" "$@" >&2
    exit 2
}

function exists_on_path {
  type "$1" >/dev/null 2>&1 && return 0
  ( IFS=:; for p in $PATH; do [ -x "${p%/}/$1" ] && return 0 || echo "${p%/}/$1"; done; return 1 )
}

function require_on_path {
  if ! exists_on_path "$1"; then
    error "No $1 executable found on PATH."
  fi
}
