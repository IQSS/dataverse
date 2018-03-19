#!/bin/sh
# Download files based on database IDs (one per line) in files.txt.
usage() {
    echo "Usage: $0 -b http://localhost:8080 -s 0" 1>&2
    exit 1;
}

BASE_URL=http://localhost:8080
FILES=files.txt
SLEEP=0

while getopts ":s:b:" o; do
    case "${o}" in
        b)
            BASE_URL=${OPTARG}
            ;;
        s)
            SLEEP=${OPTARG}
            ;;
        *)
            usage
            ;;
    esac
done

if [ ! -f $FILES ]; then
  echo "$FILES not found! Create it in your current directory with a database ID for a file on each line."
  exit 1
fi

for i in `cat $FILES`; do
    curl --insecure --silent --show-error --output /dev/null $BASE_URL/api/access/datafile/$i
    sleep $SLEEP
done
