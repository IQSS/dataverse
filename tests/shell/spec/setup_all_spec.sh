#shellcheck shell=sh
#shellcheck disable=SC2154

# Run from tests/shell: shellspec spec/setup_all_spec.sh
# Add HTTP cases in support/setup_http_mock.sh, then assert resulting request
# journals/server state here. The real setup-all and uploader always execute.
. ./support/setup_http_fixture.sh

Describe "Complete setup command"
  BeforeEach 'setup_http_fixture'
  AfterEach 'setup_http_cleanup'

  Mock curl
    sh "$SETUP_HTTP_MOCK" "$@"
  End

  It "revokes the builtin-user key and blocks admin APIs after citation HTTP 500, then returns 1"
    When run run_setup_all metadata_failure
    The status should equal 1
    The error should match pattern '*citation*HTTP*500*curl exit 0*{"status":"ERROR","message":"citation rejected"}*'
    The path "$SETUP_HTTP_TMPDIR/stdout" should not satisfy setup_http_contains 'Setup done.'
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
PUT :BlockedApiEndpoints admin,builtin-users'
    The path "$SETUP_HTTP_STATE/builtin-key-active" should not be exist
    The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should satisfy setup_http_content 'admin,builtin-users'
    The path "$SETUP_HTTP_STATE/blocked-api-policy" should satisfy setup_http_content 'localhost-only'
    The path "$SETUP_HTTP_STATE/admin-superuser" should be file
    The path "$SETUP_HTTP_STATE/root-created" should be file
    The path "$SETUP_HTTP_STATE/license-created" should be file
    The path "$SETUP_HTTP_TMPDIR/requests" should satisfy setup_http_contains 'POST /api/admin/roles/'
    The path "$SETUP_HTTP_TMPDIR/requests" should satisfy setup_http_contains 'POST /api/admin/authenticationProviders/'
    The path "$SETUP_HTTP_TMPDIR/requests" should satisfy setup_http_contains 'POST /api/dataverses/:root/facets/'
  End

  It "returns 0 and completes secure setup when all metadata requests return HTTP 200"
    When run run_setup_all success
    The status should equal 0
    The error should equal ''
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
PUT :BlockedApiEndpoints admin,builtin-users'
    The path "$SETUP_HTTP_STATE/builtin-key-active" should not be exist
    The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should satisfy setup_http_content 'admin,builtin-users'
    The path "$SETUP_HTTP_STATE/root-created" should be file
    The path "$SETUP_HTTP_STATE/license-created" should be file
  End

  Describe "deliberately insecure setup"
    Parameters
      '--insecure'
      '-insecure'
    End

    It "keeps builtin-user creation enabled without blocking APIs for $1, but still returns 1 after HTTP 500"
      When run run_setup_all metadata_failure "$1"
      The status should equal 1
      The error should match pattern '*citation*HTTP*500*curl exit 0*'
      The path "$SETUP_HTTP_TMPDIR/stdout" should not satisfy setup_http_contains 'Setup done.'
      The path "$SETUP_HTTP_TMPDIR/metadata-requests" should satisfy setup_http_content 'NAControlledVocabularyValue
citation
geospatial
social_science
astrophysics
biomedical
journals
3d_objects'
      The path "$SETUP_HTTP_TMPDIR/security-requests" should satisfy setup_http_content 'PUT :BuiltinUsersKey
PUT :BlockedApiPolicy localhost-only'
      The path "$SETUP_HTTP_STATE/builtin-key-active" should be file
      The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should not be exist
      The path "$SETUP_HTTP_STATE/root-created" should be file
      The path "$SETUP_HTTP_STATE/license-created" should be file
    End

    It "returns 0 and leaves APIs deliberately unblocked for $1 when metadata succeeds"
      When run run_setup_all success "$1"
      The status should equal 0
      The error should equal ''
      The path "$SETUP_HTTP_TMPDIR/security-requests" should satisfy setup_http_content 'PUT :BuiltinUsersKey
PUT :BlockedApiPolicy localhost-only'
      The path "$SETUP_HTTP_STATE/builtin-key-active" should be file
      The path "$SETUP_HTTP_STATE/blocked-api-endpoints" should not be exist
      The path "$SETUP_HTTP_STATE/root-created" should be file
      The path "$SETUP_HTTP_STATE/license-created" should be file
    End
  End

End
