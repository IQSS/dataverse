#!/bin/sh
DCM_VERSION=0.5
RSAL_VERSION=0.1

if [ ! -e dcm-${DCM_VERSION}-0.noarch.rpm ]; then
	wget https://github.com/sbgrid/data-capture-module/releases/download/${DCM_VERSION}/dcm-${DCM_VERSION}-0.noarch.rpm
fi

if [ ! -e rsal-${RSAL_VERSION}-0.noarch.rpm ] ;then
	wget https://github.com/sbgrid/rsal/releases/download/${RSAL_VERSION}/rsal-${RSAL_VERSION}-0.noarch.rpm
fi
