#!/bin/bash

# [INFO]: Watch Dataverse Metadata Fields and update Solr Schema on changes

set -euo pipefail

#### #### #### #### #### #### #### #### #### #### #### #### #### #### #### #### #### ####
# This script has two modes: watching and one-shot.
#
# In watching mode, it will:
# 1. Watch changes for changes to the Dataverse Metadata Fields by polling the REST API
# 2. Download the field definitions and apply them using update-fields.sh
# 3. Make sure there are actually changes between the current and the new schema.xml
# 4. Create a backup copy of the live schema.xml before replacing it
# 5. Call the Solr RELOAD API to update the index
# 6. In case something goes wrong, it will restore the known working configuration
#
# In one-shot mode, it (usually) only executes steps 2 to 4.
#
# Upgrade Mode (oneshot only):
# - Use --upgrade (-U) flag to apply downloaded metadata fields to a template schema
# - By default uses template from $SOLR_TEMPLATE/conf/schema.xml
# - Template location can be overridden with --schema-source-path or UPGRADE_SOURCE_PATH
#
# Health Checks (for Kubernetes):
# - Liveness: Check if /tmp/watcher-alive timestamp is recent (updated each cycle)
# - Readiness: Check if /tmp/watcher-ready file exists
# - These are opt-in via --enable-health-checks flag
#
# Example Kubernetes probes:
#   livenessProbe:
#     exec:
#       command:
#       - /bin/bash
#       - -c
#       - test $(( $(date +%s) - $(stat -c %Y /tmp/watcher-alive 2>/dev/null || echo 0) )) -lt 300
#     initialDelaySeconds: 30
#     periodSeconds: 60
#   readinessProbe:
#     exec:
#       command:
#       - test
#       - -f
#       - /tmp/watcher-ready
#     initialDelaySeconds: 5
#     periodSeconds: 10
#### #### #### #### #### #### #### #### #### #### #### #### #### #### #### #### #### ####

# Default configuration variables
DEFAULT_DATAVERSE_URL="http://localhost:8080"
DEFAULT_SOLR_URL="http://localhost:8983"
DEFAULT_SOLR_CORE="collection1"
DEFAULT_SCHEMA_PATH="/var/solr/data/collection1/conf/schema.xml"
DEFAULT_UPDATE_FIELDS_SCRIPT="$(dirname "$0")/update-fields.sh"
DEFAULT_POLL_INTERVAL="60"
DEFAULT_WORK_DIR="/tmp/dataverse-schema-watcher"
DEFAULT_MODE="oneshot"
DEFAULT_STARTUP_CHECK="fail"
DEFAULT_HEALTH_CHECKS_ENABLED="false"
DEFAULT_LIVENESS_FILE="/tmp/watcher-alive"
DEFAULT_READINESS_FILE="/tmp/watcher-ready"
DEFAULT_LOCK_TIMEOUT="300"
DEFAULT_WAIT_RETRY_PERIOD="5"
DEFAULT_WAIT_MAX_RETRIES="60"
DEFAULT_UPGRADE_MODE="false"
# Note: this is specific to the configbaker container use case. Override with -P!
DEFAULT_UPGRADE_SOURCE_PATH="${SOLR_TEMPLATE}/conf/schema.xml"

# Initialize from environment or defaults
DATAVERSE_URL="${DATAVERSE_URL:-${DEFAULT_DATAVERSE_URL}}"
SOLR_URL="${SOLR_URL:-${DEFAULT_SOLR_URL}}"
SOLR_CORE="${SOLR_CORE:-${DEFAULT_SOLR_CORE}}"
SCHEMA_TARGET_PATH="${SCHEMA_TARGET_PATH:-${DEFAULT_SCHEMA_PATH}}"
SCHEMA_SOURCE_PATH="${SCHEMA_SOURCE_PATH:-${DEFAULT_SCHEMA_PATH}}"
UPDATE_FIELDS_SCRIPT="${UPDATE_FIELDS_SCRIPT:-${DEFAULT_UPDATE_FIELDS_SCRIPT}}"
POLL_INTERVAL="${POLL_INTERVAL:-${DEFAULT_POLL_INTERVAL}}"
WORK_DIR="${WORK_DIR:-${DEFAULT_WORK_DIR}}"
MODE="${MODE:-${DEFAULT_MODE}}"
STARTUP_CHECK="${STARTUP_CHECK:-${DEFAULT_STARTUP_CHECK}}"
HEALTH_CHECKS_ENABLED="${HEALTH_CHECKS_ENABLED:-${DEFAULT_HEALTH_CHECKS_ENABLED}}"
LIVENESS_FILE="${LIVENESS_FILE:-${DEFAULT_LIVENESS_FILE}}"
READINESS_FILE="${READINESS_FILE:-${DEFAULT_READINESS_FILE}}"
LOCK_TIMEOUT="${LOCK_TIMEOUT:-${DEFAULT_LOCK_TIMEOUT}}"
WAIT_RETRY_PERIOD="${WAIT_RETRY_PERIOD:-${DEFAULT_WAIT_RETRY_PERIOD}}"
WAIT_MAX_RETRIES="${WAIT_MAX_RETRIES:-${DEFAULT_WAIT_MAX_RETRIES}}"
UPGRADE_MODE="${UPGRADE_MODE:-${DEFAULT_UPGRADE_MODE}}"
UPGRADE_SOURCE_PATH="${UPGRADE_SOURCE_PATH:-${DEFAULT_UPGRADE_SOURCE_PATH}}"

METADATA_ENDPOINT=""
LOCK_FD=""
SOLR_AUTH_HEADER=""
DATAVERSE_AUTH_HEADER=""
SCHEMA_SOURCE_PATH_SET_BY_USER="false"

# Logging functions
log_info() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] $*"
}

log_error() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [ERROR] $*" >&2
}

log_warn() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [WARN] $*"
}

# Update liveness indicator
update_liveness() {
    if [[ "${HEALTH_CHECKS_ENABLED}" == "true" ]]; then
        touch "${LIVENESS_FILE}" 2>/dev/null || log_warn "Failed to update liveness file"
    fi
}

# Mark as ready
mark_ready() {
    if [[ "${HEALTH_CHECKS_ENABLED}" == "true" ]]; then
        touch "${READINESS_FILE}" 2>/dev/null || log_warn "Failed to create readiness file"
        log_info "Marked as ready"
    fi
}

# Mark as not ready
mark_not_ready() {
    if [[ "${HEALTH_CHECKS_ENABLED}" == "true" ]]; then
        rm -f "${READINESS_FILE}" 2>/dev/null || true
        log_info "Marked as not ready"
    fi
}

# Cleanup function
cleanup() {
    log_info "Shutting down..."
    mark_not_ready
    release_schema_lock
    if [[ "${HEALTH_CHECKS_ENABLED}" == "true" ]]; then
        rm -f "${LIVENESS_FILE}" 2>/dev/null || true
    fi
    exit 0
}

# Set up signal handlers
trap cleanup SIGTERM SIGINT SIGQUIT

# Usage information
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
    -m, --mode MODE                Mode: 'watch' (default) or 'oneshot'
    -i, --interval SECONDS         Polling interval in seconds (watch mode)

    -d, --dataverse-url URL        Dataverse API base URL
    -s, --solr-url URL             Solr base URL
    -c, --core NAME                Solr core name

    -p, --schema-target-path PATH  Path to target schema.xml (where to write)
    -P, --schema-source-path PATH  Path to source schema.xml (base for updates)
    -t, --lock-timeout SECONDS     Schema file lock timeout in seconds
    -U, --upgrade                  Enable upgrade mode (oneshot only)
                                   Apply metadata to template schema instead of current and reload the core.

    -k, --startup-check MODE       Startup check mode: 'fail', 'warn', or 'wait'
                                   (fail: exit on error, warn: continue with warning, wait: block until ready)
    --wait-retry-period SECONDS    Retry period in seconds for 'wait' startup mode
    --wait-max-retries NUMBER      Maximum number of retries for 'wait' startup mode

    -e, --enable-health-checks     Enable Kubernetes liveness/readiness health checks
    -l, --liveness-file PATH       Path to liveness indicator file
    -r, --readiness-file PATH      Path to readiness indicator file

    -u, --update-script PATH       Path to update-fields.sh script
    -w, --work-dir PATH            Working directory path
    -h, --help                     Show this help message

Environment Variables (used as defaults if command-line options not provided):
    DATAVERSE_URL           Dataverse base URL (default: ${DEFAULT_DATAVERSE_URL})
    SOLR_URL                Solr base URL (default: ${DEFAULT_SOLR_URL})
    SOLR_CORE               Solr core name (default: ${DEFAULT_SOLR_CORE})
    SCHEMA_TARGET_PATH      Path to target schema.xml (default: ${DEFAULT_SCHEMA_PATH})
    SCHEMA_SOURCE_PATH      Path to source schema.xml (default: ${DEFAULT_SCHEMA_PATH})
    UPDATE_FIELDS_SCRIPT    Path to update-fields.sh script (default: ${DEFAULT_UPDATE_FIELDS_SCRIPT})
    POLL_INTERVAL           Polling interval in seconds (default: ${DEFAULT_POLL_INTERVAL})
    WORK_DIR                Working directory (default: ${DEFAULT_WORK_DIR})
    MODE                    Execution mode: 'watch' or 'oneshot' (default: ${DEFAULT_MODE})
    UPGRADE_MODE            Enable upgrade mode: 'true' or 'false' (default: ${DEFAULT_UPGRADE_MODE})
    UPGRADE_SOURCE_PATH     Template schema path for upgrade mode (default: ${DEFAULT_UPGRADE_SOURCE_PATH})
    STARTUP_CHECK           Startup check mode: 'fail', 'warn', or 'wait' (default: ${DEFAULT_STARTUP_CHECK})
    HEALTH_CHECKS_ENABLED   Enable health checks: 'true' or 'false' (default: ${DEFAULT_HEALTH_CHECKS_ENABLED})
    LIVENESS_FILE           Path to liveness indicator file (default: ${DEFAULT_LIVENESS_FILE})
    READINESS_FILE          Path to readiness indicator file (default: ${DEFAULT_READINESS_FILE})
    LOCK_TIMEOUT            File lock timeout in seconds (default: ${DEFAULT_LOCK_TIMEOUT})
    WAIT_RETRY_PERIOD       Retry period (in seconds) for 'wait' startup check mode (default: ${DEFAULT_WAIT_RETRY_PERIOD})
    WAIT_MAX_RETRIES        Max retries for 'wait' startup check mode (default: ${DEFAULT_WAIT_MAX_RETRIES})

Secret Configuration (only via environment variable or file):
    SOLR_USERNAME                 Solr HTTP Basic Auth username (optional)
    SOLR_PASSWORD                 Solr HTTP Basic Auth password (optional)
    SOLR_USERNAME_FILE            File containing Solr username (alternative to SOLR_USERNAME)
    SOLR_PASSWORD_FILE            File containing Solr password (alternative to SOLR_PASSWORD)
    DATAVERSE_BEARER_TOKEN        Bearer token for Dataverse API (optional)
    DATAVERSE_BEARER_TOKEN_FILE   File containing bearer token (alternative)
    DATAVERSE_UNBLOCK_KEY         Unblock key for Dataverse API (optional)
    DATAVERSE_UNBLOCK_KEY_FILE    File containing unblock key (alternative)

Schema Path Behavior:
    By default, source and target paths are the same (${DEFAULT_SCHEMA_PATH}).
    In upgrade mode (-U), if source path is not explicitly set via -P:
      - Source automatically defaults to template: ${DEFAULT_UPGRADE_SOURCE_PATH}
      - Target remains as specified (or default)
    Use -P to explicitly override source path in any mode.

Health Checks (for Kubernetes):
    Liveness:  Check if ${DEFAULT_LIVENESS_FILE} timestamp is recent
    Readiness: Check if ${DEFAULT_READINESS_FILE} exists

Examples:
    # Watch mode with defaults
    $0

    # One-shot mode with custom paths
    $0 --mode oneshot --schema-target-path /opt/solr/schema.xml

    # Upgrade mode: apply metadata to template schema
    $0 --mode oneshot --upgrade

    # Upgrade mode with custom template location
    $0 --mode oneshot --upgrade --schema-source-path /custom/template/schema.xml

    # Upgrade mode with custom template via environment
    UPGRADE_SOURCE_PATH=/custom/template.xml $0 --mode oneshot --upgrade

    # Watch mode that waits for services to be ready with custom retry settings
    $0 --startup-check wait --wait-retry-period 10 --wait-max-retries 30

    # Enable health checks for Kubernetes
    $0 --enable-health-checks

    # Using environment variables
    MODE=oneshot SOLR_CORE=mycore $0

    # With Solr authentication from environment
    SOLR_USERNAME=admin SOLR_PASSWORD=secret $0

    # With secrets from files
    SOLR_USERNAME_FILE=/run/secrets/solr_user SOLR_PASSWORD_FILE=/run/secrets/solr_pass $0

    # With Dataverse bearer token
    DATAVERSE_BEARER_TOKEN=\$(cat /run/secrets/dv_token) $0

Kubernetes Probe Examples:
    livenessProbe:
      exec:
        command:
        - /bin/bash
        - -c
        - test \$(( \$(date +%s) - \$(stat -c %Y ${DEFAULT_LIVENESS_FILE} 2>/dev/null || echo 0) )) -lt 300
      initialDelaySeconds: 30
      periodSeconds: 60

    readinessProbe:
      exec:
        command:
        - test
        - -f
        - ${DEFAULT_READINESS_FILE}
      initialDelaySeconds: 5
      periodSeconds: 10

EOF
    exit 0
}

# Check for required commands
check_cli_utils() {
    local missing=()

    if ! command -v sha256sum >/dev/null 2>&1; then
        missing+=("sha256sum")
    fi

    if ! command -v curl >/dev/null 2>&1; then
        missing+=("curl")
    fi

    if ! command -v diff >/dev/null 2>&1; then
        missing+=("diff")
    fi

    if ! command -v flock >/dev/null 2>&1; then
        missing+=("flock")
    fi

    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing required commands: ${missing[*]}"
        log_error "Please install the missing CLI utilities"
        return 1
    fi

    log_info "All required CLI utilities are available"
    return 0
}

# Check if update-fields.sh script exists and is executable
check_update_script() {
    if [[ ! -f "${UPDATE_FIELDS_SCRIPT}" ]]; then
        log_error "Update fields script not found: ${UPDATE_FIELDS_SCRIPT}"
        return 1
    fi

    if [[ ! -x "${UPDATE_FIELDS_SCRIPT}" ]]; then
        log_error "Update fields script is not executable: ${UPDATE_FIELDS_SCRIPT}"
        log_error "Run: chmod +x ${UPDATE_FIELDS_SCRIPT}"
        return 1
    fi

    log_info "Update fields script found and executable: ${UPDATE_FIELDS_SCRIPT}"
    return 0
}

# Check read/write permissions
check_permissions() {
    local schema_dir
    schema_dir="$(dirname "${SCHEMA_TARGET_PATH}")"

    # Check schema directory is writable (for creating backups and updating schema)
    if [[ ! -d "${schema_dir}" ]]; then
        log_error "Schema directory does not exist: ${schema_dir}"
        return 1
    fi

    if [[ ! -w "${schema_dir}" ]]; then
        log_error "Schema directory is not writable: ${schema_dir}"
        return 1
    fi

    # If schema file exists, check if it's readable and writable
    if [[ -f "${SCHEMA_TARGET_PATH}" ]]; then
        if [[ ! -r "${SCHEMA_TARGET_PATH}" ]]; then
            log_error "Schema file is not readable: ${SCHEMA_TARGET_PATH}"
            return 1
        fi

        if [[ ! -w "${SCHEMA_TARGET_PATH}" ]]; then
            log_error "Schema file is not writable: ${SCHEMA_TARGET_PATH}"
            return 1
        fi

        log_info "Schema file is readable and writable: ${SCHEMA_TARGET_PATH}"
    else
        log_warn "Schema file does not exist yet: ${SCHEMA_TARGET_PATH}"
        log_info "Will be created on first update"
    fi

    log_info "Schema directory is writable: ${schema_dir}"
    return 0
}

# Acquire exclusive lock on schema file operations
acquire_schema_lock() {
    local lock_file="${WORK_DIR}/schema.lock"

    log_info "Acquiring lock on schema operations (timeout: ${LOCK_TIMEOUT}s)"

    # Open file descriptor for lock file
    exec {LOCK_FD}>"${lock_file}" || {
        log_error "Failed to open lock file: ${lock_file}"
        return 1
    }

    # Try to acquire exclusive lock with timeout
    if ! flock -x -w "${LOCK_TIMEOUT}" "${LOCK_FD}"; then
        log_error "Failed to acquire lock within ${LOCK_TIMEOUT} seconds"
        exec {LOCK_FD}>&- 2>/dev/null || true
        unset LOCK_FD
        return 1
    fi

    log_info "Lock acquired successfully"
    return 0
}

# Release schema lock
release_schema_lock() {
    if [[ -n "${LOCK_FD}" ]]; then
        log_info "Releasing schema lock"
        exec {LOCK_FD}>&- 2>/dev/null || true
        unset LOCK_FD
    fi
}

# Initialize working directory
init_work_dir() {
    if ! mkdir -p "${WORK_DIR}"; then
        log_error "Failed to create working directory: ${WORK_DIR}"
        return 1
    fi

    if [[ ! -w "${WORK_DIR}" ]]; then
        log_error "Working directory is not writable: ${WORK_DIR}"
        return 1
    fi

    log_info "Working directory ready: ${WORK_DIR}"
    return 0
}

# Check if an endpoint is reachable
check_endpoint() {
    local url="$1"
    local name="$2"
    local auth_header="${3:-}"

    local curl_opts=(-sf --max-time 5)

    if [[ -n "${auth_header}" ]]; then
        curl_opts+=(-H "${auth_header}")
    fi

    if curl "${curl_opts[@]}" "${url}" >/dev/null 2>&1; then
        log_info "${name} is reachable: ${url}"
        return 0
    else
        log_error "${name} is not reachable: ${url}"
        return 1
    fi
}

# Check Solr status endpoint
check_solr_status() {
    local status_url="${SOLR_URL}/solr/${SOLR_CORE}/admin/ping"
    check_endpoint "${status_url}" "Solr core (${SOLR_CORE})" "${SOLR_AUTH_HEADER}"
}

# Check Dataverse API status
check_dataverse_status() {
    local status_url="${DATAVERSE_URL}/api/admin/settings"
    check_endpoint "${status_url}" "Dataverse API" "${DATAVERSE_AUTH_HEADER}"
}

# Perform startup checks with configured behavior
perform_startup_checks() {
    local all_ok=true

    log_info "Performing startup checks (mode: ${STARTUP_CHECK})"

    case "${STARTUP_CHECK}" in
        wait)
            log_info "Waiting for services to be ready..."

            # Check once with output to show URLs (always check both)
            check_solr_status
            local solr_ok=$?
            check_dataverse_status
            local dataverse_ok=$?

            if [[ ${solr_ok} -eq 0 && ${dataverse_ok} -eq 0 ]]; then
                log_info "All services are ready"
                return 0
            fi

            # Services not ready, enter retry loop
            local retry_count=1
            while [[ ${retry_count} -lt ${WAIT_MAX_RETRIES} ]]; do
                all_ok=true

                # Update liveness during wait
                update_liveness

                local status_msg=""
                if ! check_solr_status >/dev/null 2>&1; then
                    all_ok=false
                    status_msg="Solr: not ready"
                else
                    status_msg="Solr: ready"
                fi

                if ! check_dataverse_status >/dev/null 2>&1; then
                    all_ok=false
                    status_msg="${status_msg}, Dataverse: not ready"
                else
                    status_msg="${status_msg}, Dataverse: ready"
                fi

                if [[ "${all_ok}" == "true" ]]; then
                    log_info "All services are ready"
                    return 0
                fi

                retry_count=$((retry_count + 1))
                log_info "${status_msg} (attempt ${retry_count}/${WAIT_MAX_RETRIES})"
                sleep "${WAIT_RETRY_PERIOD}"
            done

            log_error "Services did not become ready after ${WAIT_MAX_RETRIES} attempts"
            return 1
            ;;

        warn)
            if ! check_solr_status; then
                log_warn "Solr status check failed, but continuing due to startup-check=warn"
                all_ok=false
            fi

            if ! check_dataverse_status; then
                log_warn "Dataverse status check failed, but continuing due to startup-check=warn"
                all_ok=false
            fi

            if [[ "${all_ok}" == "false" ]]; then
                log_warn "Some startup checks failed, continuing anyway"
            fi
            return 0
            ;;

        fail)
            if ! check_solr_status; then
                return 1
            fi

            if ! check_dataverse_status; then
                return 1
            fi

            log_info "All startup checks passed"
            return 0
            ;;

        *)
            log_error "Invalid startup check mode: ${STARTUP_CHECK}"
            return 1
            ;;
    esac
}

# Fetch metadata fields from Dataverse API
fetch_metadata_fields() {
    local output_file="$1"
    local url="${METADATA_ENDPOINT}"

    log_info "Fetching metadata fields from ${METADATA_ENDPOINT}"

    local curl_opts=(-sf -o "${output_file}")

    # Add authentication header if configured
    if [[ -n "${DATAVERSE_AUTH_HEADER}" ]]; then
        curl_opts+=(-H "${DATAVERSE_AUTH_HEADER}")
    fi

    if ! curl "${curl_opts[@]}" "${url}"; then
        log_error "Failed to fetch metadata fields from API"
        return 1
    fi

    # Verify we got XML content
    if ! grep -q "<?xml" "${output_file}" 2>/dev/null && ! grep -q "<field" "${output_file}" 2>/dev/null; then
        log_error "Response does not appear to be valid XML"
        return 1
    fi

    log_info "Metadata fields saved to ${output_file}"
    return 0
}

# Calculate checksum of metadata
calculate_metadata_checksum() {
    local file="$1"
    sha256sum "${file}" | awk '{print $1}'
}

# Apply field definitions using update-fields.sh
apply_field_definitions() {
    local metadata_file="$1"
    local target_schema="$2"

    log_info "Applying field definitions using ${UPDATE_FIELDS_SCRIPT}"

    # The update-fields.sh script takes: schema_file source_file
    # It modifies the schema file in place, so we need to work with a copy
    local temp_schema="${WORK_DIR}/schema.xml.temp"

    # Use source schema as base for updates
    if [[ -f "${SCHEMA_SOURCE_PATH}" ]]; then
        log_info "Using source schema as base: ${SCHEMA_SOURCE_PATH}"
        cp "${SCHEMA_SOURCE_PATH}" "${temp_schema}"
    elif [[ -f "${target_schema}" ]]; then
        log_info "Source schema not found, using target schema as base: ${target_schema}"
        cp "${target_schema}" "${temp_schema}"
    else
        log_error "No base schema file found (neither source nor target exist)"
        return 1
    fi

    if ! "${UPDATE_FIELDS_SCRIPT}" "${temp_schema}" "${metadata_file}"; then
        log_error "Failed to apply field definitions"
        return 1
    fi

    # Move the updated temp schema to the target location
    mv "${temp_schema}" "${target_schema}"

    log_info "Field definitions applied successfully"
    return 0
}

# Check if schema has changes
schema_has_changes() {
    local current_schema="$1"
    local new_schema="$2"

    if [[ ! -f "${current_schema}" ]]; then
        log_warn "Current schema not found, treating as changed"
        return 0
    fi

    if diff -q "${current_schema}" "${new_schema}" > /dev/null 2>&1; then
        log_info "No changes detected in schema"
        return 1
    fi

    log_info "Schema changes detected"
    return 0
}

# Backup current schema
backup_schema() {
    local schema_file="$1"
    # shellcheck disable=2155
    local timestamp="$(date +'%Y%m%d_%H%M%S')"
    local backup_file="${schema_file}.backup.${timestamp}"

    if [[ ! -f "${schema_file}" ]]; then
        log_warn "No existing schema to backup"
        return 0
    fi

    log_info "Backing up schema to ${backup_file}"
    if ! cp "${schema_file}" "${backup_file}"; then
        log_error "Failed to backup schema"
        return 1
    fi

    echo "${backup_file}"
    return 0
}

# Replace schema file (must be called with lock held)
replace_schema() {
    local new_schema="$1"
    local target_schema="$2"

    log_info "Replacing schema file"
    if ! cp "${new_schema}" "${target_schema}"; then
        log_error "Failed to replace schema file"
        return 1
    fi

    log_info "Schema file replaced successfully"
    return 0
}

# Reload Solr core using v2 API
reload_solr_core() {
    # Using Solr API v2 style here!
    local reload_url="${SOLR_URL}/api/cores/${SOLR_CORE}/reload"
    local response_file="${WORK_DIR}/solr_reload_response.json"
    local http_code

    log_info "Reloading Solr core: ${SOLR_CORE}"
    log_info "Using Solr v2 API: ${reload_url}"

    local curl_opts=(-sf -w "%{http_code}" -o "${response_file}" -X POST -H 'Content-type: application/json')

    # Add authentication if configured
    if [[ -n "${SOLR_AUTH_HEADER}" ]]; then
        curl_opts+=(-H "${SOLR_AUTH_HEADER}")
    fi

    http_code=$(curl "${curl_opts[@]}" "${reload_url}" 2>/dev/null || echo "000")

    if [[ "${http_code}" != "200" ]]; then
        log_error "Failed to reload Solr core (HTTP ${http_code})"

        # Try to extract error details from response
        if [[ -f "${response_file}" && -s "${response_file}" ]]; then
            log_error "Solr response:"

            # Try to pretty-print JSON if possible, otherwise dump raw
            if command -v jq >/dev/null 2>&1; then
                jq '.' "${response_file}" 2>/dev/null | while IFS= read -r line; do
                    log_error "  ${line}"
                done
            else
                while IFS= read -r line; do
                    log_error "  ${line}"
                done < "${response_file}"
            fi

            # Try to extract specific error message
            if command -v grep >/dev/null 2>&1; then
                local error_msg
                error_msg=$(grep -o '"msg":"[^"]*"' "${response_file}" 2>/dev/null | sed 's/"msg":"\(.*\)"/\1/' || true)
                if [[ -n "${error_msg}" ]]; then
                    log_error "Error message: ${error_msg}"
                fi
            fi
        else
            log_error "No response received from Solr"
        fi

        return 1
    fi

    # Check response status
    if [[ -f "${response_file}" ]]; then
        local status
        status=$(grep -o '"status":[0-9]*' "${response_file}" 2>/dev/null | cut -d':' -f2 || echo "")

        if [[ -n "${status}" && "${status}" != "0" ]]; then
            log_error "Solr returned non-zero status: ${status}"
            log_error "Full response:"
            while IFS= read -r line; do
                log_error "  ${line}"
            done < "${response_file}"
            return 1
        fi
    fi

    log_info "Solr core reloaded successfully"
    return 0
}

# Restore schema from backup (must be called with lock held)
restore_schema() {
    local backup_file="$1"
    local target_schema="$2"

    log_warn "Restoring schema from backup: ${backup_file}"

    if [[ ! -f "${backup_file}" ]]; then
        log_error "Backup file not found: ${backup_file}"
        return 1
    fi

    if ! cp "${backup_file}" "${target_schema}"; then
        log_error "Failed to restore schema from backup"
        return 1
    fi

    log_info "Schema restored successfully"
    reload_solr_core || log_error "Failed to reload Solr after restoration"
    return 0
}

# Process schema update (steps 2-4, optionally 5)
process_schema_update() {
    local reload_solr="${1:-false}"
    local metadata_file="${WORK_DIR}/metadata_fields.xml"
    local new_schema="${WORK_DIR}/schema.xml.new"
    local backup_file=""
    local update_success=false

    # Step 2: Download and apply field definitions
    if ! fetch_metadata_fields "${metadata_file}"; then
        return 1
    fi

    if ! apply_field_definitions "${metadata_file}" "${new_schema}"; then
        return 1
    fi

    # Step 3: Check for changes
    if ! schema_has_changes "${SCHEMA_TARGET_PATH}" "${new_schema}"; then
        log_info "No update needed"
        return 0
    fi

    # Acquire lock for critical section (backup, replace, reload)
    if ! acquire_schema_lock; then
        log_error "Failed to acquire schema lock"
        return 1
    fi

    # Critical section begins here
    {
        # Step 4: Backup current schema
        if ! backup_file=$(backup_schema "${SCHEMA_TARGET_PATH}"); then
            release_schema_lock
            return 1
        fi

        # Replace schema
        if ! replace_schema "${new_schema}" "${SCHEMA_TARGET_PATH}"; then
            release_schema_lock
            return 1
        fi

        # Step 5: Reload Solr (only in watch or upgrade mode)
        if [[ "${reload_solr}" == "true" ]]; then
            if ! reload_solr_core; then
                log_error "Solr reload failed, attempting to restore backup"
                if [[ -n "${backup_file}" ]]; then
                    restore_schema "${backup_file}" "${SCHEMA_TARGET_PATH}"
                fi
                release_schema_lock
                return 1
            fi
        fi

        update_success=true
    }
    # Critical section ends here

    release_schema_lock

    if [[ "${update_success}" == "true" ]]; then
        log_info "Schema update completed successfully"
        return 0
    else
        return 1
    fi
}

# One-shot mode
run_oneshot() {
    log_info "Running in oneshot mode"

    # Initial liveness update
    update_liveness

    # In oneshot, default to not reload Solr. But if upgrading, we want to reload.
    local reload_solr="false"
    if [[ "${UPGRADE_MODE}" == "true" ]]; then
      log_info "Will attempt to RELOAD Solr after upgrading the schema."
      reload_solr="true"
    fi

    if process_schema_update "$reload_solr"; then
        mark_ready
        log_info "Oneshot execution completed successfully"
        return 0
    else
        log_error "Oneshot execution failed"
        return 1
    fi
}

# Watch mode
run_watch() {
    log_info "Running in watch mode with ${POLL_INTERVAL}s polling interval"

    local last_checksum=""
    local initial_sync_done=false

    while true; do
        # Update liveness indicator at the start of each cycle
        update_liveness

        local metadata_file="${WORK_DIR}/metadata_fields_check.xml"

        if fetch_metadata_fields "${metadata_file}"; then
            local current_checksum
            current_checksum=$(calculate_metadata_checksum "${metadata_file}")

            if [[ -n "${last_checksum}" && "${current_checksum}" != "${last_checksum}" ]]; then
                log_info "Metadata change detected, processing schema update"

                # Mark as not ready during update
                mark_not_ready

                if process_schema_update "true"; then
                    last_checksum="${current_checksum}"
                    mark_ready
                else
                    log_error "Schema update failed"
                    # Stay not ready until successful update
                fi
            elif [[ -z "${last_checksum}" ]]; then
                log_info "Initial metadata fetch, setting baseline"
                last_checksum="${current_checksum}"

                # Mark ready after first successful fetch
                if [[ "${initial_sync_done}" == "false" ]]; then
                    mark_ready
                    initial_sync_done=true
                fi
            else
                log_info "No metadata changes detected"

                # Ensure we're marked ready if we haven't done initial sync
                if [[ "${initial_sync_done}" == "false" ]]; then
                    mark_ready
                    initial_sync_done=true
                fi
            fi
        else
            log_error "Failed to fetch metadata fields, will retry"
            # Mark as not ready on fetch failure
            mark_not_ready
        fi

        # Update liveness before sleep
        update_liveness

        sleep "${POLL_INTERVAL}"
    done
}

# Main
main() {
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -m|--mode)
                MODE="$2"
                shift 2
                ;;
            -d|--dataverse-url)
                DATAVERSE_URL="$2"
                shift 2
                ;;
            -s|--solr-url)
                SOLR_URL="$2"
                shift 2
                ;;
            -c|--core)
                SOLR_CORE="$2"
                shift 2
                ;;
            -p|--schema-target-path)
                SCHEMA_TARGET_PATH="$2"
                shift 2
                ;;
            -P|--schema-source-path)
                SCHEMA_SOURCE_PATH="$2"
                SCHEMA_SOURCE_PATH_SET_BY_USER="true"
                shift 2
                ;;
            -u|--update-script)
                UPDATE_FIELDS_SCRIPT="$2"
                shift 2
                ;;
            -i|--interval)
                POLL_INTERVAL="$2"
                shift 2
                ;;
            -w|--work-dir)
                WORK_DIR="$2"
                shift 2
                ;;
            -U|--upgrade)
                UPGRADE_MODE="true"
                shift
                ;;
            -k|--startup-check)
                STARTUP_CHECK="$2"
                shift 2
                ;;
            -e|--enable-health-checks)
                HEALTH_CHECKS_ENABLED="true"
                shift
                ;;
            -l|--liveness-file)
                LIVENESS_FILE="$2"
                shift 2
                ;;
            -r|--readiness-file)
                READINESS_FILE="$2"
                shift 2
                ;;
            -t|--lock-timeout)
                LOCK_TIMEOUT="$2"
                shift 2
                ;;
            --wait-retry-period)
                WAIT_RETRY_PERIOD="$2"
                shift 2
                ;;
            --wait-max-retries)
                WAIT_MAX_RETRIES="$2"
                shift 2
                ;;
            -h|--help)
                usage
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                ;;
        esac
    done

    # Load secrets from files or environment variables
    # Solr authentication
    if [[ -n "${SOLR_USERNAME_FILE:-}" && -f "${SOLR_USERNAME_FILE}" ]]; then
        SOLR_USERNAME=$(cat "${SOLR_USERNAME_FILE}")
    fi
    if [[ -n "${SOLR_PASSWORD_FILE:-}" && -f "${SOLR_PASSWORD_FILE}" ]]; then
        SOLR_PASSWORD=$(cat "${SOLR_PASSWORD_FILE}")
    fi

    if [[ -n "${SOLR_USERNAME:-}" && -n "${SOLR_PASSWORD:-}" ]]; then
        SOLR_AUTH_HEADER="Authorization: Basic $(echo -n "${SOLR_USERNAME}:${SOLR_PASSWORD}" | base64 -w 0)"
        log_info "Solr authentication configured (HTTP Basic)"
    fi

    # Dataverse authentication
    # Priority 1: Bearer token (env var or file)
    if [[ -n "${DATAVERSE_BEARER_TOKEN:-}" ]]; then
        # Bearer token already set, use it
        DATAVERSE_AUTH_HEADER="Authorization: Bearer ${DATAVERSE_BEARER_TOKEN}"
        log_info "Dataverse authentication configured (Bearer Token)"
    elif [[ -n "${DATAVERSE_BEARER_TOKEN_FILE:-}" ]]; then
        # Bearer token file specified, try to read it
        if [[ -f "${DATAVERSE_BEARER_TOKEN_FILE}" ]]; then
            DATAVERSE_BEARER_TOKEN=$(cat "${DATAVERSE_BEARER_TOKEN_FILE}")
            DATAVERSE_AUTH_HEADER="Authorization: Bearer ${DATAVERSE_BEARER_TOKEN}"
            log_info "Dataverse authentication configured (Bearer Token from file)"
        else
            log_error "DATAVERSE_BEARER_TOKEN_FILE specified but file not found: ${DATAVERSE_BEARER_TOKEN_FILE}"
            exit 1
        fi
    # Priority 2: Unblock key (only if no bearer token)
    elif [[ -n "${DATAVERSE_UNBLOCK_KEY:-}" ]]; then
        # Unblock key already set, use it
        DATAVERSE_AUTH_HEADER="X-Dataverse-unblock-key: ${DATAVERSE_UNBLOCK_KEY}"
        log_info "Dataverse authentication configured (Unblock Key)"
    elif [[ -n "${DATAVERSE_UNBLOCK_KEY_FILE:-}" ]]; then
        # Unblock key file specified, try to read it
        if [[ -f "${DATAVERSE_UNBLOCK_KEY_FILE}" ]]; then
            DATAVERSE_UNBLOCK_KEY=$(cat "${DATAVERSE_UNBLOCK_KEY_FILE}")
            DATAVERSE_AUTH_HEADER="X-Dataverse-unblock-key: ${DATAVERSE_UNBLOCK_KEY}"
            log_info "Dataverse authentication configured (Unblock Key from file)"
        else
            log_error "DATAVERSE_UNBLOCK_KEY_FILE specified but file not found: ${DATAVERSE_UNBLOCK_KEY_FILE}"
            exit 1
        fi
    fi

    # Set metadata endpoint based on Dataverse URL
    METADATA_ENDPOINT="${DATAVERSE_URL}/api/admin/index/solr/schema"

    # Validate upgrade mode restrictions
    if [[ "${UPGRADE_MODE}" == "true" && "${MODE}" == "watch" ]]; then
        log_error "Upgrade mode (-U|--upgrade) is only allowed in oneshot mode"
        log_error "Please use: --mode oneshot --upgrade"
        exit 1
    fi

    # Handle upgrade mode: override source path if not explicitly set by user
    if [[ "${UPGRADE_MODE}" == "true" && "${SCHEMA_SOURCE_PATH_SET_BY_USER}" == "false" ]]; then
        log_info "Upgrade mode enabled: using template schema as source"
        SCHEMA_SOURCE_PATH="${UPGRADE_SOURCE_PATH}"
    fi

    # Validate that source schema exists in upgrade mode
    if [[ "${UPGRADE_MODE}" == "true" && ! -f "${SCHEMA_SOURCE_PATH}" ]]; then
        log_error "Upgrade mode enabled but source schema not found: ${SCHEMA_SOURCE_PATH}"
        log_error "Please ensure the template schema exists or use -P to specify a different location"
        exit 1
    fi

    # Validate startup check mode
    case "${STARTUP_CHECK}" in
        fail|warn|wait)
            ;;
        *)
            log_error "Invalid startup check mode: ${STARTUP_CHECK}. Must be 'fail', 'warn', or 'wait'"
            exit 1
            ;;
    esac

    # Validate mode
    case "${MODE}" in
        watch|oneshot)
            ;;
        *)
            log_error "Invalid mode: ${MODE}. Must be 'watch' or 'oneshot'"
            exit 1
            ;;
    esac

    log_info "Starting Solr Driver for Dataverse Metadata Schemas"
    log_info "Mode: ${MODE}"
    if [[ "${UPGRADE_MODE}" == "true" ]]; then
        log_info "Upgrade Mode: ENABLED"
        log_info "Schema Source Path: ${SCHEMA_SOURCE_PATH}"
    fi
    log_info "Dataverse API: ${DATAVERSE_URL}"
    log_info "Solr URL: ${SOLR_URL}"
    log_info "Solr Core: ${SOLR_CORE}"
    log_info "Schema Target Path: ${SCHEMA_TARGET_PATH}"
    if [[ "${UPGRADE_MODE}" != "true" && "${SCHEMA_SOURCE_PATH}" != "${SCHEMA_TARGET_PATH}" ]]; then
        log_info "Schema Source Path: ${SCHEMA_SOURCE_PATH}"
    fi
    log_info "Update Script: ${UPDATE_FIELDS_SCRIPT}"
    log_info "Work Directory: ${WORK_DIR}"
    log_info "Startup Check Mode: ${STARTUP_CHECK}"
    log_info "Health Checks Enabled: ${HEALTH_CHECKS_ENABLED}"
    if [[ "${HEALTH_CHECKS_ENABLED}" == "true" ]]; then
        log_info "Liveness File: ${LIVENESS_FILE}"
        log_info "Readiness File: ${READINESS_FILE}"
    fi
    log_info "Lock Timeout: ${LOCK_TIMEOUT}s"
    if [[ "${STARTUP_CHECK}" == "wait" ]]; then
        log_info "Wait Retry Period: ${WAIT_RETRY_PERIOD}s"
        log_info "Wait Max Retries: ${WAIT_MAX_RETRIES}"
    fi

    # Initialize liveness indicator early
    update_liveness

    # Pre-flight checks
    log_info "Running pre-flight checks..."

    if ! check_cli_utils; then
        exit 1
    fi

    if ! check_update_script; then
        exit 1
    fi

    if ! check_permissions; then
        exit 1
    fi

    if ! init_work_dir; then
        exit 1
    fi

    if ! perform_startup_checks; then
        exit 1
    fi

    log_info "All pre-flight checks passed"

    # Run appropriate mode
    case "${MODE}" in
        watch)
            run_watch
            ;;
        oneshot)
            run_oneshot
            ;;
    esac
}

main "$@"
