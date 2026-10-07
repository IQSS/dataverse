#shellcheck shell=sh
#shellcheck disable=SC2154

# Run from tests/shell: shellspec spec/setup_datasetfields_spec.sh
# Add a response case in the curl boundary below, then assert the uploader's
# exit status and block/HTTP/body diagnostics in a corresponding example.
setup_datasetfields() {
  METADATA_HTTP_SCENARIO=$1
  METADATA_HTTP_STATUS=${2:-200}
  DATAVERSE_URL=http://metadata-upload.invalid
  export METADATA_HTTP_SCENARIO METADATA_HTTP_STATUS DATAVERSE_URL
  bash ../../scripts/api/setup-datasetfields.sh
}

Describe "Metadata block upload command"
  # Command-based mocks are inherited by the actual Bash entry point. Only the
  # HTTP boundary is replaced; response parsing and failure aggregation are real.
  Mock curl
    block=
    write_out=
    while [ "$#" -gt 0 ]; do
      case "$1" in
        */loadNAControlledVocabularyValue) block=NAControlledVocabularyValue ;;
        --data-binary)
          shift
          block=${1##*/}
          block=${block%.tsv}
          ;;
        -w|--write-out)
          shift
          write_out=$1
          ;;
      esac
      shift
    done

    body='{"status":"OK"}'
    http_status=200
    curl_status=0
    case "$METADATA_HTTP_SCENARIO:$block" in
      early_http_failure:citation)
        body='{"status":"ERROR","message":"citation rejected"}'
        http_status=500
        ;;
      final_http_failure:3d_objects)
        body='{"status":"ERROR","message":"object schema rejected"}'
        http_status=500
        ;;
      multiple_http_failures:NAControlledVocabularyValue)
        body='{"status":"ERROR","message":"controlled vocabulary rejected"}'
        http_status=500
        ;;
      multiple_http_failures:citation)
        body='{"status":"ERROR","message":"duplicate citation definition"}'
        http_status=409
        ;;
      multiple_http_failures:biomedical)
        body='{
  "status": "ERROR",
  "message": "biomedical schema rejected"
}'
        http_status=422
        ;;
      multiple_http_failures:3d_objects)
        body='{"status":"ERROR","message":"object schema unavailable"}'
        http_status=503
        ;;
      successful_http_status:citation)
        body='{
  "status": "OK",
  "data": "citation accepted"
}'
        http_status=$METADATA_HTTP_STATUS
        ;;
      controlled_vocabulary_failure:NAControlledVocabularyValue)
        body='{"status":"ERROR","message":"controlled vocabulary unavailable"}'
        http_status=500
        ;;
      connection_failure:citation)
        body=
        http_status=000
        curl_status=7
        printf '%s\n' 'curl: (7) Failed to connect to metadata-upload.invalid port 80' >&2
        ;;
      connection_failure:3d_objects)
        body='{"status":"ERROR","message":"object schema rejected after connection failure"}'
        http_status=500
        ;;
      redirected_upload:geospatial)
        body='<html>Sign in to load metadata</html>'
        http_status=302
        ;;
      truncated_response:citation)
        body='{"status":"OK","data":"truncated'
        http_status=200
        curl_status=18
        printf '%s\n' 'curl: (18) transfer closed with outstanding read data remaining' >&2
        ;;
    esac

    # Like curl, HTTP errors alone do not change the process exit status. The
    # status trailer is emitted only when the caller requests curl's write-out.
    printf '%s' "$body"
    case "$write_out" in
      '\n%{http_code}') printf '\n%s' "$http_status" ;;
      '') ;;
      *)
        printf 'Unsupported curl write-out format: %s\n' "$write_out" >&2
        exit 2
        ;;
    esac
    exit "$curl_status"
  End

  It "returns 1 for an early HTTP 500 even when curl and all later uploads succeed"
    When run setup_datasetfields early_http_failure
    The status should equal 1
    The error should match pattern '*citation*HTTP*500*{"status":"ERROR","message":"citation rejected"}*'
    The output should not include '{"status":"ERROR"'
  End

  It "returns 1 and reports the response body when the final block returns HTTP 500"
    When run setup_datasetfields final_http_failure
    The status should equal 1
    The error should match pattern '*3d_objects*HTTP*500*{"status":"ERROR","message":"object schema rejected"}*'
    The output should not include '{"status":"ERROR"'
  End

  It "rejects an unfollowed HTTP redirect instead of reporting a successful upload"
    When run setup_datasetfields redirected_upload
    The status should equal 1
    The error should match pattern '*geospatial*HTTP*302*<html>Sign in to load metadata</html>*'
    The output should not include '<html>Sign in to load metadata</html>'
  End

  It "continues after vocabulary and block failures and diagnoses every failed response"
    When run setup_datasetfields multiple_http_failures
    The status should equal 1
    The error should match pattern '*NAControlledVocabularyValue*HTTP*500*{"status":"ERROR","message":"controlled vocabulary rejected"}*'
    The error should match pattern '*citation*HTTP*409*{"status":"ERROR","message":"duplicate citation definition"}*'
    biomedical_error='{
  "status": "ERROR",
  "message": "biomedical schema rejected"
}'
    The error should match pattern "*biomedical*HTTP*422*$biomedical_error*"
    The error should match pattern '*3d_objects*HTTP*503*{"status":"ERROR","message":"object schema unavailable"}*'
    The output should not include '"ERROR"'
  End

  Describe "successful HTTP responses"
    Parameters
      200
      201
      299
    End

    It "accepts HTTP $1 and preserves a multiline response body without its status trailer"
      When run setup_datasetfields successful_http_status "$1"
      The status should equal 0
      expected_body='{
  "status": "OK",
  "data": "citation accepted"
}'
      The output should include "$expected_body"
      The output should not include "$1"
      The error should equal ""
    End
  End

  It "counts a controlled vocabulary HTTP 500 even when every built-in block succeeds"
    When run setup_datasetfields controlled_vocabulary_failure
    The status should equal 1
    The error should match pattern '*NAControlledVocabularyValue*HTTP*500*{"status":"ERROR","message":"controlled vocabulary unavailable"}*'
    The output should not include '{"status":"ERROR"'
  End

  It "returns 1 for curl exit 7 and retains the connection failure diagnostic"
    When run setup_datasetfields connection_failure
    The status should equal 1
    The error should include 'curl: (7) Failed to connect to metadata-upload.invalid port 80'
    The error should match pattern '*citation*HTTP*000*curl exit 7*'
    The error should match pattern '*3d_objects*HTTP*500*curl exit 0*{"status":"ERROR","message":"object schema rejected after connection failure"}*'
    The output should not include '000'
  End

  It "returns 1 for curl exit 18 despite HTTP 200 and retains the partial response body"
    When run setup_datasetfields truncated_response
    The status should equal 1
    The error should include 'curl: (18) transfer closed with outstanding read data remaining'
    The error should match pattern '*citation*HTTP*200*curl exit 18*{"status":"OK","data":"truncated*'
    The output should not include '{"status":"OK","data":"truncated'
  End
End
