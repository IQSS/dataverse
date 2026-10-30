#!/bin/sh
# Only the HTTP boundary is simulated. Uploader parsing, jq token extraction,
# setup-all sequencing, and bootstrap/persona error handling execute for real.
reject_request() {
  printf 'Invalid setup HTTP request: %s\n' "$1" >> "$SETUP_HTTP_TMPDIR/invalid-requests"
  exit 97
}

require_method() {
  [ "$method" = "$1" ] || reject_request "$path requires $1"
}

require_json_file() {
  case "$data" in
    @*.json) request_file=${data#@} ;;
    *) reject_request "$path requires a JSON file" ;;
  esac
  [ -f "$request_file" ] && [ -r "$request_file" ] || reject_request "JSON file must exist and be readable"
}

method=
content_type=
api_key=
data=
data_binary=
upload_file=
write_out=
url=
while [ "$#" -gt 0 ]; do
  case "$1" in
    -X|--request) shift; method=$1 ;;
    -H|--header)
      shift
      header=$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')
      case "$header" in
        'content-type: '*) content_type=${header#content-type: } ;;
        'content-type:'*) content_type=${header#content-type:} ;;
        'x-dataverse-key:'*) api_key=${header#x-dataverse-key:} ;;
      esac
      ;;
    -d|--data) shift; data=$1 ;;
    --data-binary) shift; data_binary=$1 ;;
    --upload-file) shift; upload_file=$1 ;;
    -w|--write-out) shift; write_out=$1 ;;
    -s|-sS|-sSf) ;;
    http://*) url=$1 ;;
    *) reject_request "unsupported curl argument" ;;
  esac
  shift
done

if [ -n "$data" ] || [ -n "$data_binary" ]; then
  method=${method:-POST}
elif [ -n "$upload_file" ]; then
  method=${method:-PUT}
else
  method=${method:-GET}
fi
case "$url" in
  "$DATAVERSE_URL"/api/*) path=${url#"$DATAVERSE_URL"} ;;
  *) reject_request "unexpected server or endpoint" ;;
esac
# Do not record password/key query parameters or authentication headers.
path=${path%%\?*}
printf '%s %s\n' "$method" "$path" >> "$SETUP_HTTP_TMPDIR/requests"
body='{"status":"OK"}'
http_status=200

case "$path" in
  /api/metadatablocks)
    require_method GET
    body='{"status":"OK","data":[]}'
    ;;
  /api/admin/datasetfield/loadNAControlledVocabularyValue)
    require_method GET
    printf '%s\n' NAControlledVocabularyValue >> "$SETUP_HTTP_TMPDIR/metadata-requests"
    ;;
  /api/admin/datasetfield/load)
    require_method POST
    [ "$content_type" = text/tab-separated-values ] || reject_request "TSV content type required"
    case "$data_binary" in
      @*.tsv) tsv_file=${data_binary#@} ;;
      *) reject_request "TSV load requires --data-binary @file.tsv" ;;
    esac
    [ -f "$tsv_file" ] && [ -r "$tsv_file" ] || reject_request "TSV file must exist and be readable"
    block=${tsv_file##*/}
    block=${block%.tsv}
    case "$block" in
      citation|geospatial|social_science|astrophysics|biomedical|journals|3d_objects) ;;
      *) reject_request "unexpected metadata block" ;;
    esac
    printf '%s\n' "$block" >> "$SETUP_HTTP_TMPDIR/metadata-requests"
    if [ "$SETUP_HTTP_SCENARIO" = metadata_failure ] && [ "$block" = citation ]; then
      http_status=500
      body='{"status":"ERROR","message":"citation rejected"}'
    fi
    ;;
  /api/admin/roles/|/api/admin/authenticationProviders/)
    require_method POST
    require_json_file
    ;;
  /api/admin/settings/:BuiltinUsersKey)
    case "$method" in
      PUT)
        [ -n "$data" ] || reject_request "builtin-user creation key is empty"
        : > "$SETUP_HTTP_STATE/builtin-key-active"
        printf '%s\n' 'PUT :BuiltinUsersKey' >> "$SETUP_HTTP_TMPDIR/security-requests"
        ;;
      DELETE)
        rm -f "$SETUP_HTTP_STATE/builtin-key-active"
        printf '%s\n' 'DELETE :BuiltinUsersKey' >> "$SETUP_HTTP_TMPDIR/security-requests"
        ;;
      *) reject_request "builtin-user key requires PUT or DELETE" ;;
    esac
    ;;
  /api/admin/settings/:BlockedApiPolicy)
    require_method PUT
    case "$data" in
      localhost-only|unblock-key) ;;
      *) reject_request "unexpected blocked-API policy" ;;
    esac
    printf '%s\n' "$data" > "$SETUP_HTTP_STATE/blocked-api-policy"
    printf 'PUT :BlockedApiPolicy %s\n' "$data" >> "$SETUP_HTTP_TMPDIR/security-requests"
    ;;
  /api/admin/settings/:BlockedApiKey)
    require_method PUT
    [ -n "$data" ] || reject_request "blocked-API key is empty"
    : > "$SETUP_HTTP_STATE/blocked-key-active"
    printf '%s\n' 'PUT :BlockedApiKey' >> "$SETUP_HTTP_TMPDIR/security-requests"
    ;;
  /api/admin/settings/:BlockedApiEndpoints)
    require_method PUT
    [ "$data" = admin,builtin-users ] || reject_request "sensitive endpoints must both be blocked"
    printf '%s\n' "$data" > "$SETUP_HTTP_STATE/blocked-api-endpoints"
    printf 'PUT :BlockedApiEndpoints %s\n' "$data" >> "$SETUP_HTTP_TMPDIR/security-requests"
    ;;
  /api/admin/settings/:AllowSignUp|/api/admin/settings/:SignUpUrl|/api/admin/settings/:UploadMethods)
    require_method PUT
    ;;
  /api/builtin-users)
    require_method POST
    require_json_file
    [ -f "$SETUP_HTTP_STATE/builtin-key-active" ] || reject_request "admin created without builtin-user key"
    : > "$SETUP_HTTP_STATE/admin-created"
    # Clearly fake UUID-shaped API token, not copied from any legacy secret.
    body='{"status":"OK","data":{"apiToken":"00000000-0000-4000-8000-000000000001"}}'
    ;;
  /api/admin/superuser/dataverseAdmin)
    require_method PUT
    [ -f "$SETUP_HTTP_STATE/admin-created" ] || reject_request "superuser assigned before admin creation"
    : > "$SETUP_HTTP_STATE/admin-superuser"
    ;;
  /api/dataverses/)
    require_method POST
    require_json_file
    [ "$url" = "$DATAVERSE_URL/api/dataverses/?key=00000000-0000-4000-8000-000000000001" ] || reject_request "root creation requires returned admin token"
    [ -f "$SETUP_HTTP_STATE/admin-superuser" ] || reject_request "root created before admin superuser assignment"
    : > "$SETUP_HTTP_STATE/root-created"
    ;;
  /api/dataverses/:root)
    # Demo may have a mounted root-collection JSON file. Simulate that optional
    # update without modifying or copying any mounted/user-owned data.
    require_method PUT
    [ "$api_key" = 00000000-0000-4000-8000-000000000001 ] || reject_request "root update requires returned admin token"
    [ -f "$SETUP_HTTP_STATE/root-created" ] || reject_request "root updated before creation"
    [ -f "$upload_file" ] && [ -r "$upload_file" ] || reject_request "root update file must exist and be readable"
    : > "$SETUP_HTTP_STATE/root-updated"
    ;;
  /api/dataverses/:root/metadatablocks/|/api/dataverses/:root/facets/)
    require_method POST
    [ -f "$SETUP_HTTP_STATE/root-created" ] || reject_request "root configured before creation"
    [ "$url" = "$DATAVERSE_URL$path?key=00000000-0000-4000-8000-000000000001" ] || reject_request "root configuration requires returned admin token"
    ;;
  /api/licenses)
    require_method POST
    [ "$api_key" = 00000000-0000-4000-8000-000000000001 ] || reject_request "license creation requires returned admin token"
    [ -f "$upload_file" ] && [ -r "$upload_file" ] || reject_request "license file must exist and be readable"
    : > "$SETUP_HTTP_STATE/license-created"
    ;;
  /api/admin/settings/:DoiProvider)
    require_method PUT
    if [ "$SETUP_HTTP_SCENARIO" = persona_curl_failure ]; then
      printf '%s\n' 'curl: (7) Failed to connect to the DOI settings endpoint' >&2
      exit 7
    fi
    [ "$data" = FAKE ] || reject_request "test personas must use FAKE DOI provider"
    printf '%s\n' "$data" > "$SETUP_HTTP_STATE/doi-provider"
    ;;
  /api/dataverses/:root/actions/:publish|/api/dataverses/:root/assignments)
    require_method POST
    [ "$api_key" = 00000000-0000-4000-8000-000000000001 ] || reject_request "persona configuration requires returned admin token"
    [ -f "$SETUP_HTTP_STATE/root-created" ] || reject_request "persona configured before root creation"
    case "$path" in
      */:publish) : > "$SETUP_HTTP_STATE/root-published" ;;
      */assignments)
        [ "$(printf '%s' "$data" | jq -r '.assignee + " " + .role')" = ':authenticated-users fullContributor' ] || reject_request "unexpected root access assignment"
        : > "$SETUP_HTTP_STATE/root-open-to-authenticated-users"
        ;;
    esac
    ;;
  /api/info/version)
    require_method GET
    body='{"status":"OK","data":{"version":"test-version"}}'
    ;;
  *) reject_request "unexpected endpoint $path" ;;
esac

printf '%s' "$body"
case "$write_out" in
  '\n%{http_code}') printf '\n%s' "$http_status" ;;
  '') ;;
  *) reject_request "unsupported curl write-out format" ;;
esac
# curl returns zero for an HTTP 500 unless asked to fail on HTTP errors.
exit 0
