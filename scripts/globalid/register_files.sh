#!/bin/sh

fileidsfile=$1

if [ ! -f $fileidsfile ] 
then
    echo "usage: ./register_files.sh {FILE CONTAINING DATAFILE IDs}"
    echo "see the README.txt for more info"
    exit 1
fi

# Sort the provided list of datafile ids for uniqueness: 

sort -nr $fileidsfile > $fileidsfile.sorted

if [ ! -f $fileidsfile.sorted ] 
then
    echo "ERROR! Failed to sort the provided list of datafile ids"
    exit 1
fi


cat $fileidsfile.sorted | while read id
do
    curl "http://localhost:8080/api/admin/$id/registerDataFile"
    echo 
    echo "registered $id"
    sleep 1
done