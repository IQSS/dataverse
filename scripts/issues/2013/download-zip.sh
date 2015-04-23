#!/bin/bash
APACHE_PORT=8888
GLASSFISH_PORT=8088
PORT=$APACHE_PORT
count=0; while true; do echo "downloading 4 GB file as zip attempt $((++count))"; curl -s http://127.0.0.1:$PORT/api/access/datafiles/3 > /tmp/3; done
