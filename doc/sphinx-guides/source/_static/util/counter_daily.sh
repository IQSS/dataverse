#! /bin/bash
#counter_daily.sh

COUNTER_PROCESSOR_DIRECTORY="/usr/local/counter-processor-1.06"
MDC_LOG_DIRECTORY="/usr/local/payara6/glassfish/domains/domain1/logs/mdc"
COUNTER_PROCESSOR_TMP_DIRECTORY="/tmp"
# If you wish to keep the logs, use a directory that is not periodically cleaned, e.g.
#COUNTER_PROCESSOR_TMP_DIRECTORY="/usr/local/counter-processor-1.06/tmp"

cd $COUNTER_PROCESSOR_DIRECTORY

echo >>$COUNTER_PROCESSOR_TMP_DIRECTORY/counter_daily.log
date >>$COUNTER_PROCESSOR_TMP_DIRECTORY/counter_daily.log
echo >>$COUNTER_PROCESSOR_TMP_DIRECTORY/counter_daily.log

# "You should run Counter Processor once a day to create reports in SUSHI (JSON) format that are saved to disk for Dataverse to process and that are sent to the DataCite hub."

LAST=$(date -d "yesterday 13:00" '+%Y-%m-%d')
# echo $LAST
YEAR_MONTH=$(date -d "yesterday 13:00" '+%Y-%m')
# echo $YEAR_MONTH
d=$(date -I -d "$YEAR_MONTH-01")
#echo $d
while [ "$(date -d "$d" +%Y%m%d)" -le "$(date -d "$LAST" +%Y%m%d)" ];
do
  if [ -f "$MDC_LOG_DIRECTORY/counter_$d.log" ]; then
      echo "Found counter_$d.log"
  else
      touch "$MDC_LOG_DIRECTORY/counter_$d.log"
  fi
  d=$(date -I -d "$d + 1 day")
done

#run counter-processor as counter user

sudo -u counter YEAR_MONTH=$YEAR_MONTH python3 main.py >>$COUNTER_PROCESSOR_TMP_DIRECTORY/counter_daily.log

# Process all make-data-count-report.json.* files
for report_file in $COUNTER_PROCESSOR_TMP_DIRECTORY/make-data-count-report.json.*; do
    if [ -f "$report_file" ]; then
        echo "Processing $report_file" >>$COUNTER_PROCESSOR_TMP_DIRECTORY/counter_daily.log
        curl -X POST "http://localhost:8080/api/admin/makeDataCount/addUsageMetricsFromSushiReport?reportOnDisk=$report_file"
        echo "Finished processing $report_file" >>$COUNTER_PROCESSOR_TMP_DIRECTORY/counter_daily.log
        
        # Extract the base filename and the extension
        file_base=$(basename "$report_file" | sed 's/\.json\..*//')
        file_ext=$(echo "$report_file" | sed -n 's/.*\.json\.\(.*\)/\1/p')
        echo $file_base
        echo $file_ext
        # Remove the old file if it exists
        rm -f $COUNTER_PROCESSOR_TMP_DIRECTORY/${file_base}.${YEAR_MONTH}.json.${file_ext}

        # Move the processed file
        mv $report_file $COUNTER_PROCESSOR_TMP_DIRECTORY/${file_base}.${YEAR_MONTH}.json.${file_ext}
    fi
done
