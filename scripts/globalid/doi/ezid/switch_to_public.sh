#!/bin/sh

DOILIST=$1
SESSIONID=$2

if [ ! -f $DOILIST ] 
then
    echo "usage: ./script {DOILIST} {SESSIONID}"
    exit 1
fi

if [ $SESSIONID"x" = "x" ]
then
    echo "usage: ./script {DOILIST} {SESSIONID}"
    exit 1
fi

if [ ! -x "ezid.py" ]
then
    echo "Cannot find the python script ezid.py in the current directory!"
    echo "Please download the script from https://ezid.cdlib.org/doc/ezid.py,"
    echo "place it in the current directory and make sure it is executable"
    exit 1
fi

grep ^doi: $DOILIST | while read doi 
do 
    ./ezid.py sessionid=$SESSIONID update $doi _status public
    sleep 1
done    
