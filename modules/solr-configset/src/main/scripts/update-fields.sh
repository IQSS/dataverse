#!/usr/bin/env bash

set -euo pipefail

# [INFO]: Update a prepared Solr schema.xml for Dataverse with a given list of metadata fields

#### #### #### #### #### #### #### #### #### #### #### #### #### #### #### #### #### ####
# This script will
# 1. take a file (or read it from STDIN) with all <field> and <copyField> definitions
# 2. and replace the sections between the include guards with those in a given
#    schema.xml file
# The script validates the presence, uniqueness and order of the include guards.
#### #### #### #### #### #### #### #### #### #### #### #### #### #### #### #### #### ####


### Variables
# Internal use only (fork to change)
VERSION="0.1"
INPUT=""
FIELDS=""
COPY_FIELDS=""
TRIGGER_CHAIN=0
ED_DELETE_FIELDS="'a+,'b-d"
ED_DELETE_COPYFIELDS="'a+,'b-d"

SOLR_SCHEMA_FIELD_BEGIN_MARK="SCHEMA-FIELDS::BEGIN"
SOLR_SCHEMA_FIELD_END_MARK="SCHEMA-FIELDS::END"
SOLR_SCHEMA_COPYFIELD_BEGIN_MARK="SCHEMA-COPY-FIELDS::BEGIN"
SOLR_SCHEMA_COPYFIELD_END_MARK="SCHEMA-COPY-FIELDS::END"
MARKS_ORDERED="${SOLR_SCHEMA_FIELD_BEGIN_MARK} ${SOLR_SCHEMA_FIELD_END_MARK} ${SOLR_SCHEMA_COPYFIELD_BEGIN_MARK} ${SOLR_SCHEMA_COPYFIELD_END_MARK}"

### Common functions
function error {
    echo "ERROR:" "$@" >&2
    exit 2
}

function exists {
  type "$1" >/dev/null 2>&1 && return 0
  ( IFS=:; for p in $PATH; do [ -x "${p%/}/$1" ] && return 0; done; return 1 )
}

function usage {
    cat << EOF
$(basename "$0") ${VERSION}
Usage: $(basename "$0") [-hp] [ schema file ] [ source file ]

-h  Print usage (this text)
-p  Chained printing: write all metadata schema related <field>
    and <copyField> present in Solr XML to stdout

Provide target Solr Schema XML via argument or \$SCHEMA env var.

Provide source file via argument, \$SOURCE env var or piped input
(wget/curl, chained). Source file = "-" means read STDIN.
EOF
    exit 0
}

### Options
while getopts ":hp" opt; do
  case $opt in
    h) usage ;;
    p) TRIGGER_CHAIN=1 ;;
   \?) echo "Invalid option: -$OPTARG" >&2; exit 1 ;;
    :) echo "Option -$OPTARG requires an argument." >&2; exit 1 ;;
  esac
done

# Check for ed and bc being present
exists ed || error "Please ensure ed, bc, sed + awk are installed"
exists bc || error "Please ensure ed, bc, sed + awk are installed"
exists awk || error "Please ensure ed, bc, sed + awk are installed"
exists sed || error "Please ensure ed, bc, sed + awk are installed"

# remove all the parsed options
shift $((OPTIND-1))

# User overrideable locations
SCHEMA=${SCHEMA:-${1:-schema.xml}}
SOURCE=${SOURCE:-${2:-"-"}}


### VERIFY SCHEMA FILE EXISTS AND CONTAINS INCLUDE GUARDS ###
# Check for schema file & writeable
if [ ! -w "${SCHEMA}" ]; then
  error "Cannot find or write to a XML schema at ${SCHEMA}"
else
  # Check schema file for include guards
  CHECKS=$(
    for MARK in ${MARKS_ORDERED}
    do
      grep -c "${MARK}" "${SCHEMA}" || error "Missing ${MARK} from ${SCHEMA}"
    done
  )

  # Check guards are unique (count occurrences and sum calc via bc)
  # Note: fancy workaround to re-add closing \n on Linux & MacOS or no calculation
  [ "$( (echo -n "${CHECKS}" | tr '\n' '+' ; echo ) | bc)" -eq 4 ] || \
    error "Some include guards are not unique in ${SCHEMA}"

  # Check guards are in order (line number comparison via bc tricks)
  CHECKS=$(
    for MARK in ${MARKS_ORDERED}
    do
        grep -n "${MARK}" "${SCHEMA}" | cut -f 1 -d ":"
    done
  )
  # Actual comparison of line numbers
  echo "${CHECKS}" | tr '\n' '<' | awk -F'<' '{ if ($1 < $2 && $2 < $3 && $3 < $4) {exit 0} else {exit 1} }' || \
    error "Include guards are not in correct order in ${SCHEMA}"

  # Check guards are exclusively in their lines
  # (no field or copyField on same line)
  for MARK in ${MARKS_ORDERED}
  do
    grep "${MARK}" "${SCHEMA}" | grep -q -v -e '\(<field \|<copyField \)' \
      || error "Mark ${MARK} is not on an exclusive line"
  done

  # Check if there are no lines between the field marks (then skip delete in ed)
  # Note: fancy workaround to re-add closing \n on Linux & MacOS or no calculation
  DISTANCE_FIELDS_MARKS=$( \
    (grep -n -e "\(${SOLR_SCHEMA_FIELD_BEGIN_MARK}\|${SOLR_SCHEMA_FIELD_END_MARK}\)" "${SCHEMA}" \
      | cut -f 1 -d ":" | tr '\n' '<' | sed -e 's#<$#-1#' ; echo) \
      | bc
  )
  if [ "${DISTANCE_FIELDS_MARKS}" -eq 0 ]; then
    ED_DELETE_FIELDS="#"
  fi
  # Check if there are no lines between the copyfield marks (then skip delete in ed)
  DISTANCE_COPYFIELDS_MARKS=$( \
    (grep -n -e "\(${SOLR_SCHEMA_COPYFIELD_BEGIN_MARK}\|${SOLR_SCHEMA_COPYFIELD_END_MARK}\)" "${SCHEMA}" \
      | cut -f 1 -d ":" | tr '\n' '<' | sed -e 's#<$#-1#' ; echo ) \
      | bc
  )
  if [ "${DISTANCE_COPYFIELDS_MARKS}" -eq 0 ]; then
    ED_DELETE_COPYFIELDS="#"
  fi
fi


### READ DATA ###
# Switch to standard input if no file present or "-"
if [ -z "${SOURCE}" ] || [ "${SOURCE}" = "-" ]; then
  # But ONLY if stdin for this script has not been attached to a terminal, but a pipe
  if [ ! -t 0 ]; then
    SOURCE="/dev/stdin"
  else
    error "No data - either provide source file or piped input"
  fi
else
  # Check the given file for readability and non-zero length
  if [ ! -r "${SOURCE}" ] || [ ! -s "${SOURCE}" ]; then
    error "Cannot read from or empty file ${SOURCE}"
  fi
fi
# Read relevant parts only, filter nonsense and avoid huge memory usage
INPUT=$(grep -e "<\(field\|copyField\) .*/>" "${SOURCE}" | sed -e 's#^\s\+##' -e 's#\s\+$##' || true)


### DATA HANDLING ###
# Split input into different types
if [ -z "${INPUT}" ]; then
  error "No <field> or <copyField> in input"
else
  # Check for <field> definitions (if nomatch, avoid failing pipe)
  FIELDS=$(mktemp)
  echo "${INPUT}" | grep -e "<field .*/>" | sed -e 's#^#    #' > "${FIELDS}" || true
  # If file actually contains output, write to schema
  if [ -s "${FIELDS}" ]; then
    # Use an ed script to replace all <field>
    cat << EOF | grep -v -e "^#" | ed -s "${SCHEMA}"
H
# Mark field begin as 'a'
/${SOLR_SCHEMA_FIELD_BEGIN_MARK}/ka
# Mark field end as 'b'
/${SOLR_SCHEMA_FIELD_END_MARK}/kb
# Delete all between lines a and b
${ED_DELETE_FIELDS}
# Read fields file and paste after line a
'ar ${FIELDS}
# Write fields to schema
w
q
EOF
  fi
  rm "${FIELDS}"

  # Check for <copyField> definitions (if nomatch, avoid failing pipe)
  COPY_FIELDS=$(mktemp)
  echo "${INPUT}" | grep -e "<copyField .*/>" | sed -e 's#^#    #' > "${COPY_FIELDS}" || true
  # If file actually contains output, write to schema
  if [ -s "${COPY_FIELDS}" ]; then
      # Use an ed script to replace all <copyField>, filter comments (BSD ed does not support comments)
      cat << EOF | grep -v -e "^#" | ed -s "${SCHEMA}"
H
# Mark copyField begin as 'a'
/${SOLR_SCHEMA_COPYFIELD_BEGIN_MARK}/ka
# Mark copyField end as 'b'
/${SOLR_SCHEMA_COPYFIELD_END_MARK}/kb
# Delete all between lines a and b
${ED_DELETE_COPYFIELDS}
# Read fields file and paste after line a
'ar ${COPY_FIELDS}
# Write copyFields to schema
w
q
EOF
    fi
    rm "${COPY_FIELDS}"
fi


### CHAINING OUTPUT
# Scripts following this one might want to use the field definitions now present
if [ "${TRIGGER_CHAIN}" -eq 1 ]; then
  grep -A1000 "${SOLR_SCHEMA_FIELD_BEGIN_MARK}" "${SCHEMA}" | grep -B1000 "${SOLR_SCHEMA_FIELD_END_MARK}"
  grep -A1000 "${SOLR_SCHEMA_COPYFIELD_BEGIN_MARK}" "${SCHEMA}" | grep -B1000 "${SOLR_SCHEMA_COPYFIELD_END_MARK}"
fi
