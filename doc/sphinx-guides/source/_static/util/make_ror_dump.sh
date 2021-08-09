#!/bin/sh

ROR_FILTER=country.country_code:GB,types:Education
DUMP_FILE=ror.json

ROR_PER_PAGE=20
RESULTS_COUNT=$(curl "https://api.ror.org/organizations?filter=$ROR_FILTER" | jq .number_of_results)

echo "ROR identifiers matching filter [$ROR_FILTER] count: $RESULTS_COUNT"

total_pages=$(( ($RESULTS_COUNT+$ROR_PER_PAGE-1)/$ROR_PER_PAGE ))
echo "Total pages to download: $total_pages"

page=1
while [ $page -le $total_pages ]
do
  echo "Downloading page $page"

  curl "https://api.ror.org/organizations?page=$page&filter=$ROR_FILTER" | jq .items >> $DUMP_FILE.$page

  echo "Created temporary file $DUMP_FILE.$page with single page content"

  page=$(( $page + 1 ))
done

echo "Joining pages into single file"
jq -s 'flatten' $DUMP_FILE.* >> $DUMP_FILE

echo "Removing temporary files"
rm $DUMP_FILE.*

ROR_IN_DUMP=$(jq length $DUMP_FILE)
echo "ROR identifiers count in produced dump: $ROR_IN_DUMP"
