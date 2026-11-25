#shellcheck shell=sh
#shellcheck disable=SC2154

# Run from tests/shell: shellspec spec/bootstrap_spec.sh
# These execute bootstrap plus the actual dev/demo personas and setup uploader.
# The extra file-boundary mocks only isolate the personas' fixed /tmp log path.
. ./support/setup_http_fixture.sh

Describe "Configbaker bootstrap command"
  BeforeEach 'setup_bootstrap_fixture'
  AfterEach 'setup_http_cleanup'

  Mock curl
    sh "$SETUP_HTTP_MOCK" "$@"
  End

  Mock wait4x
    # The controlled HTTP instance is immediately available; never poll a host.
    exit 0
  End

  Mock tee
    /usr/bin/tee "$SETUP_HTTP_TMPDIR/setup-all.sh.out"
  End

  Mock grep
    if [ "$#" -eq 2 ] && [ "$2" = /tmp/setup-all.sh.out ]; then
      /usr/bin/grep "$1" "$SETUP_HTTP_TMPDIR/setup-all.sh.out"
    else
      /usr/bin/grep "$@"
    fi
  End

  Mock mktemp
    if [ "$#" -eq 0 ]; then
      /usr/bin/mktemp "$SETUP_HTTP_TMPDIR/env.XXXXXX"
    else
      /usr/bin/mktemp "$@"
    fi
  End

  It "finishes demo lockdown after citation HTTP 500 and returns bootstrap status 1"
    When run run_bootstrap metadata_failure demo
    The status should equal 1
    The error should match pattern '*citation*HTTP*500*curl exit 0*'
    The path "$SETUP_HTTP_TMPDIR/stdout" should not satisfy setup_http_contains 'Done, your instance has been configured for demo or eval.'
    The path "$SETUP_HTTP_TMPDIR/metadata-requests" should satisfy setup_http_content 'NAControlledVocabularyValue
citation
geospatial
social_science
astrophysics
biomedical
journals
3d_objects'
    The path "$SETUP_HTTP_TMPDIR/security-requests" should satisfy setup_http_content 'PUT :BuiltinUsersKey
PUT :BlockedApiPolicy localhost-only
DELETE :BuiltinUsersKey
PUT :BlockedApiKey
PUT :BlockedApiPolicy unblock-key
PUT :BlockedApiEndpoints admin,builtin-users'
    The path "$SETUP_HTTP_STATE/builtin-key-active" should not be exist
    The path "$SETUP_HTTP_STATE/blocked-key-active" should be file
    The path "$SETUP_HTTP_STATE/blocked-api-policy" should satisfy setup_http_content 'unblock-key'
    The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should satisfy setup_http_content 'admin,builtin-users'
    The path "$SETUP_HTTP_STATE/doi-provider" should satisfy setup_http_content 'FAKE'
    The path "$SETUP_HTTP_STATE/root-created" should be file
    The path "$SETUP_HTTP_STATE/license-created" should be file
  End

  It "returns 0 after successful demo setup and leaves both sensitive APIs blocked"
    When run run_bootstrap success demo
    The status should equal 0
    The error should equal ''
    The path "$SETUP_HTTP_STATE/builtin-key-active" should not be exist
    The path "$SETUP_HTTP_STATE/blocked-key-active" should be file
    The path "$SETUP_HTTP_STATE/blocked-api-policy" should satisfy setup_http_content 'unblock-key'
    The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should satisfy setup_http_content 'admin,builtin-users'
    The path "$SETUP_HTTP_STATE/doi-provider" should satisfy setup_http_content 'FAKE'
  End

  It "exports the returned admin token and completes intentional development access after HTTP 500, then returns 1"
    When run run_bootstrap metadata_failure dev
    The status should equal 1
    The error should match pattern '*citation*HTTP*500*curl exit 0*'
    The path "$SETUP_HTTP_TMPDIR/stdout" should not satisfy setup_http_contains 'Done, your instance has been configured for development.'
    The path "$SETUP_HTTP_TMPDIR/target.env" should satisfy setup_http_content 'API_TOKEN=00000000-0000-4000-8000-000000000001'
    The path "$SETUP_HTTP_TMPDIR/security-requests" should satisfy setup_http_content 'PUT :BuiltinUsersKey
PUT :BlockedApiPolicy localhost-only'
    The path "$SETUP_HTTP_STATE/builtin-key-active" should be file
    The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should not be exist
    The path "$SETUP_HTTP_STATE/doi-provider" should satisfy setup_http_content 'FAKE'
    The path "$SETUP_HTTP_STATE/root-published" should be file
    The path "$SETUP_HTTP_STATE/root-open-to-authenticated-users" should be file
    The path "$SETUP_HTTP_TMPDIR/requests" should satisfy setup_http_contains 'GET /api/info/version'
  End

  It "returns 0 and exports the admin token after successful development setup"
    When run run_bootstrap success dev
    The status should equal 0
    The error should equal ''
    The path "$SETUP_HTTP_TMPDIR/target.env" should satisfy setup_http_content 'API_TOKEN=00000000-0000-4000-8000-000000000001'
    The path "$SETUP_HTTP_STATE/root-published" should be file
    The path "$SETUP_HTTP_STATE/root-open-to-authenticated-users" should be file
    The path "$SETUP_HTTP_STATE/builtin-key-active" should be file
    The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should not be exist
  End

  Describe "unrelated persona failures still obey errexit"
    Parameters
      dev
      demo
    End

    It "returns curl status 7 and stops $1 when its DOI configuration cannot connect"
      When run run_bootstrap persona_curl_failure "$1"
      The status should equal 7
      The error should include 'curl: (7)'
      The path "$SETUP_HTTP_TMPDIR/stdout" should not satisfy setup_http_contains 'Done, your instance has been configured'
      The path "$SETUP_HTTP_STATE/root-created" should be file
      The path "$SETUP_HTTP_STATE/license-created" should be file
      The path "$SETUP_HTTP_STATE/doi-provider" should not be exist
      The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should not be exist
      The path "$SETUP_HTTP_STATE/root-published" should not be exist
      The path "$SETUP_HTTP_STATE/root-open-to-authenticated-users" should not be exist
      The path "$SETUP_HTTP_TMPDIR/target.env" should satisfy setup_http_content ''
    End
  End
End
