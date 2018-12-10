#!/usr/bin/env bash

JACOCO_HOME=${HOME}/local/jacoco-0.8.2/lib/
PID=$$
WAR_IN=$1
WAR_OUT=$2

if [ -z "${WAR_IN}" ]; then
	echo "no input war specified; bailing out"
	exit 1
fi

if [ -z "${WAR_OUT}" ]; then
	echo "no output war specified; bailing out"
	exit 1
fi

if [ ! -e "${JACOCO_HOME}/jacococli.jar" ]; then
	echo "jacococli.jar not found in ${JACOCO_HOME}; bailing out"
	exit 1
fi

TMPDIR=/tmp/war-wksp-${PID}
CWD=`pwd`

mkdir -p ${TMPDIR}/extract/
cp ${WAR_IN} ${TMPDIR}/
cd ${TMPDIR}/extract/
wb=`basename ${WAR_IN}`
jar xf ../${wb}
mv WEB-INF/classes WEB-INF/orig-classes
java -jar ${JACOCO_HOME}/jacococli.jar instrument WEB-INF/orig-classes/ --dest WEB-INF/classes
rm -r WEB-INF/orig-classes/
jar cf ${CWD}/${WAR_OUT} *
cd ${CWD}

# assuming that /tmp gets auto-cleaned, don't bother removing ${TMPDIR}

