# shellcheck shell=sh
# Shared fixture for real setup-all/bootstrap entry points. Each example has its
# own HTTP request journal and simulated server state; nothing reaches a server.
setup_http_fixture() {
  SETUP_HTTP_TMPDIR=$(mktemp -d "${TMPDIR:-/tmp}/dataverse-setup-spec.XXXXXX") || return 98
  SETUP_HTTP_STATE=$SETUP_HTTP_TMPDIR/state
  SETUP_HTTP_MOCK=$(pwd)/support/setup_http_mock.sh
  DATAVERSE_URL=http://setup-regression.invalid
  mkdir "$SETUP_HTTP_STATE"
  : > "$SETUP_HTTP_TMPDIR/requests"
  : > "$SETUP_HTTP_TMPDIR/security-requests"
  : > "$SETUP_HTTP_TMPDIR/metadata-requests"
  : > "$SETUP_HTTP_TMPDIR/invalid-requests"
  : > "$SETUP_HTTP_TMPDIR/target.env"
  export SETUP_HTTP_TMPDIR SETUP_HTTP_STATE SETUP_HTTP_MOCK DATAVERSE_URL
}

setup_http_cleanup() {
  rm -rf "$SETUP_HTTP_TMPDIR"
}

setup_http_result() {
  # Never let a deliberately failing HTTP scenario mask a bad request.
  if [ -s "$SETUP_HTTP_TMPDIR/invalid-requests" ]; then
    cat "$SETUP_HTTP_TMPDIR/invalid-requests" >&2
    return 97
  fi
  return "$1"
}

run_setup_all() {
  SETUP_HTTP_SCENARIO=$1
  shift
  export SETUP_HTTP_SCENARIO
  setup_status=0
  # Clearly fake five-character substitute for the legacy default password.
  bash ../../scripts/api/setup-all.sh -p=fake0 "$@" > "$SETUP_HTTP_TMPDIR/stdout" || setup_status=$?
  setup_http_result "$setup_status"
}

setup_bootstrap_fixture() {
  setup_http_fixture || return $?
  BOOTSTRAP_DIR=$SETUP_HTTP_TMPDIR/bootstrap
  mkdir "$BOOTSTRAP_DIR"
  # Symlink the actual script directories, not copied or rewritten personas.
  ln -s "$(pwd)/../../scripts/api" "$BOOTSTRAP_DIR/base"
  ln -s "$(pwd)/../../modules/container-configbaker/scripts/bootstrap/dev" "$BOOTSTRAP_DIR/dev"
  ln -s "$(pwd)/../../modules/container-configbaker/scripts/bootstrap/demo" "$BOOTSTRAP_DIR/demo"
  # Clearly fake nine-character substitute for the demo's blocked-API key.
  BLOCKED_API_KEY=fakekey00
  export BOOTSTRAP_DIR BLOCKED_API_KEY
}

run_bootstrap() {
  SETUP_HTTP_SCENARIO=$1
  export SETUP_HTTP_SCENARIO
  bootstrap_status=0
  bash ../../modules/container-configbaker/scripts/bootstrap.sh \
    -e "$SETUP_HTTP_TMPDIR/target.env" "$2" > "$SETUP_HTTP_TMPDIR/stdout" || bootstrap_status=$?
  setup_http_result "$bootstrap_status"
}

# Follow the existing specs' path/satisfy convention. ShellSpec supplies the
# subject path in the variable with the same name as each predicate.
# shellcheck disable=SC2154
setup_http_content() {
  [ "$(cat "$setup_http_content")" = "$1" ]
}

# shellcheck disable=SC2154
setup_http_contains() {
  case "$(cat "$setup_http_contains")" in
    *"$1"*) return 0 ;;
    *) return 1 ;;
  esac
}
