#!/bin/bash
count=0; while true; echo "hitting homepage attempt $((++count))"; do (curl -s -i http://127.0.0.1:8888 | head -9); sleep 3; done
