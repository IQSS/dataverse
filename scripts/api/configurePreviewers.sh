#!/bin/bash

curl -X POST -H 'Content-type: application/json' --upload-file ../../conf/external-tools/audioTool.json http://localhost:8080/api/admin/externalTools
curl -X POST -H 'Content-type: application/json' --upload-file ../../conf/external-tools/videoTool.json http://localhost:8080/api/admin/externalTools
curl -X POST -H 'Content-type: application/json' --upload-file ../../conf/external-tools/imageTool.json http://localhost:8080/api/admin/externalTools
curl -X POST -H 'Content-type: application/json' --upload-file ../../conf/external-tools/pdfTool.json http://localhost:8080/api/admin/externalTools


