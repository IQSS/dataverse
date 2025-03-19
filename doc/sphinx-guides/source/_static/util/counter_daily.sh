#! /bin/bash

COUNTER_PROCESSOR_DIRECTORY="/usr/local/counter-processor-1.06"
MDC_LOG_DIRECTORY="/usr/local/payara6/glassfish/domains/domain1/logs/mdc"

# counter_daily.sh

cd $COUNTER_PROCESSOR_DIRECTORY

echo >>/tmp/counter_daily.log
date >>/tmp/counter_daily.log
echo >>/tmp/counter_daily.log

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
#       echo "Found counter_$d.log"
  else
        touch "$MDC_LOG_DIRECTORY/counter_$d.log"
  fi
  d=$(date -I -d "$d + 1 day")
done

#run counter-processor as counter user

sudo -u counter YEAR_MONTH=$YEAR_MONTH python3 main.py >>/tmp/counter_daily.log

curl -X POST "http://localhost:8080/api/admin/makeDataCount/addUsageMetricsFromSushiReport?reportOnDisk=/tmp/make-data-count-report.json"
