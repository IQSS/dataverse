#! /bin/bash
set -x

# This script will process each file from s3 bucket where archive log files are stored
# 1. Loop through each file not already processed (by date).
# 2. Call counter-processor to convert the log files to SUSHI formatted files
# 3. counter-processor will call Dataverse API: /api/admin/makeDataCount/addUsageMetricsFromSushiReport?reportOnDisk=... to store dataset metrics in dataverse DB.
# 4. counter-processor will upload the data to DataCite if upload_to_hub is set to True.
# 5. The state of each file is inserted in Dataverse DB. This allows failed files to be re-tried as well as limiting the number of files being processed with each run.

# MDC logs. There is one log per node per day, .../domain1/logs/counter_YYYY-MM-DD.log
# To enable MDC logging set the following settings:
# curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:DisplayMDCMetrics
# curl -X PUT -d '/opt/dvn/app/payara6/glassfish/domains/domain1/logs' http://localhost:8080/api/admin/settings/:MDCLogPath
declare -a NODE=("app-1" "app-2")
COUNTERPROCESSORDIR=/usr/local/counter-processor-1.05
LOGDIR=/opt/dvn/app/payara6/glassfish/domains/domain1/logs
ARCHIVEDIR=s3://dvn-cloud/Admin/logs/payara/counter
REPORTONDISKDIR=$LOGDIR
CopyFromArchiveCmd="aws s3 cp ${ARCHIVEDIR}"
RunAsCounterProcessorUser="sudo -u counter"
upload_to_hub=False
platform_name="Harvard Dataverse"
hub_base_url="https://api.datacite.org"
# If uploading to DataCite make sure the hub_api_token is defined in COUNTERPROCESSORDIR/config/secrets.yaml and not hard coded in this script

# Testing with dataverse running in docker
if [ -d docker-dev-volumes/ ]; then
  echo "Docker Directory exists."
  RunAsCounterProcessorUser="sudo"
  DATAVERSESOURCEDIR=$PWD
  #COUNTERPROCESSORDIR=$DATAVERSESOURCEDIR/../counter-processor
  LOGDIR=$DATAVERSESOURCEDIR/docker-dev-volumes/app/data/temp
  ARCHIVEDIR=$DATAVERSESOURCEDIR/tests/data
  REPORTONDISKDIR=/dv/temp
  CopyFromArchiveCmd="cp -v ${ARCHIVEDIR}"
  platform_name="Harvard Dataverse Test Account"
  hub_base_url="https://api.test.datacite.org"
  upload_to_hub=False
fi

log_name_pattern="${COUNTERPROCESSORDIR}/log/counter_(yyyy-mm-dd).log"
output_report_file=$COUNTERPROCESSORDIR/tmp/make-data-count-report

# This config file contains the settings that can not be overwritten here.
# path_types:
#    investigations:
#    requests:
export CONFIG_FILE="${COUNTERPROCESSORDIR}/config/counter-processor-config.yaml"
# See: https://guides.dataverse.org/en/latest/admin/make-data-count.html#configure-counter-processor
# and download https://guides.dataverse.org/en/latest/_downloads/f99910a3cc45e4f68cc047f7c033c7f0/counter-processor-config.yaml

function process_json_file () {
  # Process the logs by calling counter-processor
  year_month="${1}"
  cd $COUNTERPROCESSORDIR

  l=$(ls log/counter_${year_month}-*.log | sort -r)
  log_date=${l:12:10}
  sim_date=$(date -j -v +1d -f "%Y-%m-%d" "${log_date}" +%F)
  response=$(curl -sS -X GET "http://localhost:8080/api/admin/makeDataCount/$log_date/processingState") 2>/dev/null
  state=$(echo "$response" | jq -j '.data.state')
  rerun=True
  if [[ "${state}" == "FAILED" ]]; then
    rerun=True
  fi
  curl -sS -X POST "http://localhost:8080/api/admin/makeDataCount/$log_date/processingState?state=processing"
  : > $COUNTERPROCESSORDIR/tmp/datacite_response_body.txt
  eval "$RunAsCounterProcessorUser YEAR_MONTH=${year_month} SIMULATE_DATE=${sim_date} PLATFORM='${platform_name}' LOG_NAME_PATTERN='${log_name_pattern}' OUTPUT_FILE='${output_report_file}' UPLOAD_TO_HUB='${upload_to_hub}' HUB_BASE_URL='${hub_base_url}' CLEAN_FOR_RERUN='${rerun}' python3 main.py &> $COUNTERPROCESSORDIR/tmp/counter.log"
  cat $COUNTERPROCESSORDIR/tmp/counter.log
  cat $COUNTERPROCESSORDIR/tmp/datacite_response_body.txt
  report=counter_${log_date}.json
  cp -v ${output_report_file}.json ${LOGDIR}/${report}
  response=$(curl -sS -X POST "http://localhost:8080/api/admin/makeDataCount/addUsageMetricsFromSushiReport?reportOnDisk=${REPORTONDISKDIR}/${report}") 2>/dev/null
  if [[ "$(echo "$response" | jq -j '.status')" != "OK" ]]; then
    state="failed"
  else
    state="done"
    # ok to delete the report now. The original is still in counter-processor if needed
    rm -rf ${LOGDIR}/${report}
  fi
  curl -sS -X POST "http://localhost:8080/api/admin/makeDataCount/$log_date/processingState?state="$state
  # If the month is complete update the year_month
  if [[ "${sim_date:8:2}" == "01" ]]; then
    curl -sS -X POST "http://localhost:8080/api/admin/makeDataCount/$year_month/processingState?state="$state
  else
    # TODO: will we ever encounter a tar file with an incomplete month? If so then we need to figure out how to skip it until it's complete
    curl -sS -X POST "http://localhost:8080/api/admin/makeDataCount/$year_month/processingState?state=skip"
  fi
}

function process_archived_files () {
  # Check each node for the newest file. If multiple nodes have the same date file we need to merge the files
  nodeArraylength=${#NODE[@]}
  for (( i=0; i<${nodeArraylength}; i++ ));
  do
    echo "index: $i, value: ${NODE[$i]}"
    ls ${ARCHIVEDIR}/${NODE[$i]}/counter_*.tar | sort -r | while read l
    do
        year_month=${l:(-11):7}
        echo "Found archive file for "$year_month
        response=$(curl -sS -X GET "http://localhost:8080/api/admin/makeDataCount/$year_month/processingState") 2>/dev/null
        state=$(echo "$response" | jq -j '.data.state')
        if [[ "${state}" == "DONE" ]] || [[ "${state}" == "SKIP" ]]; then
          echo "Skipping due to state:${state}"
        else
          NEW_LOGDIR=${LOGDIR}/${NODE[$i]}_${year_month}
          mkdir -p ${NEW_LOGDIR}
          # Copy the tar file from archive back to local, un-tar it and clean up intermediate files.
          eval "$CopyFromArchiveCmd/${NODE[$i]}/counter_${year_month}.tar ${NEW_LOGDIR}/counter_${year_month}.tar"
          tar -xvzf ${NEW_LOGDIR}/counter_${year_month}.tar --directory ${NEW_LOGDIR}
          ls ${NEW_LOGDIR}/counter_${year_month}-* | while read l
          do
            gzip -d $l
          done
          rm -r ${NEW_LOGDIR}/counter_${year_month}.tar
          break
        fi
    done
  done

  # Determine which node/nodes have the newest files. Unless a node was down for the month they should all have files
  # for the same dates so merging is a must.
  # Get a list of directories under LOGDIR that are in format NODE_yyyy-mm and strip to get yyyy_mm
  # Sort so newest yyyy-mm is first in the list
  ls -1d $LOGDIR/*/ | rev | cut -d'_' -f1 | rev | sort -r | uniq > /tmp/archived_files
  # Read first line and strip off trailing '/' to get the newest year_month to process
  read -r line < /tmp/archived_files
  year_month=${line:(-8):7}
  echo $year_month
  # year_month will be empty if no more files to process
  if [ ! -z "$year_month" ]; then
    # Get the list of directories to merge for this year_month
    ls -1d $LOGDIR/*_$year_month/ > /tmp/archived_files

    # Merge subsequent directories into firstDirectory. Note: firstDirectory may or may not be NODE 1. It shouldn't matter
    read -r firstDirectory < /tmp/archived_files
    tail -n +2 /tmp/archived_files| while read l
    do
       ls ${l}counter_*.log | while read l
         do
           # It should never happen but if 1 of the files is missing create it so the merge will not fail
           if [ ! -e "$l" ]; then
             touch $l
           fi
           # Strip off just the file name ie. counter_2024-02-01.log
           log_file=${l:(-22)}
           sort -um -o ${firstDirectory}${log_file} ${firstDirectory}${log_file} ${l}
        done
    done < /tmp/archived_files

    # Now firstDirectory has all the merged data so we can move it to the counter_processor log directory and clean up the NODE directories
    eval "$RunAsCounterProcessorUser cp ${firstDirectory}*.log $COUNTERPROCESSORDIR/log"
    for (( i=0; i<${nodeArraylength}; i++ ));
    do
      rm -rf $LOGDIR/${NODE[$i]}*
    done

    process_json_file "$year_month"

    # After processing is done delete the log files from counter_processor log directory
    eval "$RunAsCounterProcessorUser rm -rf $COUNTERPROCESSORDIR/log/counter_*.log"
  fi
}

# Main
process_archived_files
