#!/bin/bash

SOLR_VERSION='4.6.0'
OUTPUT_VERBOSITY=1

_usage() {
  echo "\nUsage: $0 \[dhilmsuv\]"
  echo "\nSupported options:"
  echo "  -h     Print this help message.
  echo "  -s     Select the solr version to install.\[${SOLR_VERSION}\]"
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "\n"
}

while getopts :h:s:v: FLAG; do
  case $FLAG in
    h)  #print help
      _usage
      exit 0
      ;;
    s)  #set option solr version "s"
      SOLR_VERSION=$OPTARG
      ;;
    v)  #set output verbosity level "v"
      OUTPUT_VERBOSITY=$OPTARG
      ;;
    :)  #valid option requires adjacent argument
      echo "Option $OPTARG requires an adjacent argument" >&2
      exit 1;
      ;;
    *)
      ;;
  esac
done

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
if [[ -e "/dataverse/scripts/api/bin/util-set-verbosity.sh" ]]; then
  . "/dataverse/scripts/api/bin/util-set-verbosity.sh"
elif [[ -e "../../api/bin/util-set-verbosity.sh" ]]; then
  . "../../api/bin/util-set-verbosity.sh"
elif [[ -e "./util-set-verbosity.sh" ]]; then
  . "./util-set-verbosity.sh"
else
  CURL_CMD='curl'
fi

$_IF_TERSE echo "Installing Solr ${SOLR_VERSION} using legacy install method with verbosity level ${OUTPUT_VERBOSITY}!"

#### Check for adequate java ####
$_IF_INFO echo "Checking java ..."
if ( type -p java >/dev/null 2>&1 ); then
  $_IF_VERBOSE echo "found java executable in PATH" 
  _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
  $_IF_VERBOSE echo "found java executable in JAVA_HOME"     
  _java="$JAVA_HOME/bin/java"
fi

JAVA_VERSION=0
if [[ "$_java" ]]; then
  JAVA_VERSION=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
  $_IF_VERBOSE echo "java version: ${JAVA_VERSION}"
fi

MIN_JAVA_VERSION=1.6
if [[ ( -z ${JAVA_VERSION+x} ) || ( $JAVA_VERSION < $MIN_JAVA_VERSION ) ]]; then
  $_IF_VERBOSE echo "A suitable version of java could not be found."
  $_IF_INFO echo "Preparing yum to install java ..."
  $_IF_VERBOSE 2>&1 rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6
  yummyPkg="java-1.8.0-openjdk-devel"
  $_IF_TERSE echo "Installing Java ${yummyPkg} ..."
  $YUM_CMD -y install $yummyPkg
  if ( type -p alternatives >/dev/null 2>&1 ); then
    $_IF_VERBOSE echo "Setting primary java/javac using alternatives..."
    alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java
    alternatives --set javac /usr/lib/jvm/java-1.8.0-openjdk.x86_64/bin/javac
  fi
  $_IF_TERSE echo "${yummyPkg} installed"
else
  $_IF_INFO echo "java version: ${JAVA_VERSION} is acceptable"
fi

#### Download solr package from apache archive ####
$_IF_TERSE echo "Installing solr version: ${SOLR_VERSION}"
if [[ ! -e /dataverse/downloads/solr-${SOLR_VERSION}.tgz ]]; then
  $_IF_VERBOSE 2>&1 pushd /dataverse/downloads
  $_IF_INFO echo "Downloading solr-${SOLR_VERSION}.tgz and solr-${SOLR_VERSION}.tgz.md5"
  for i in {1..5}; do
    $_IF_VERBOSE echo "Download attempt: $i"
    if [[ ! -e solr-${SOLR_VERSION}.tgz ]]; then
      $CURL_CMD -L -O "https://archive.apache.org/dist/lucene/solr/${SOLR_VERSION}/solr-${SOLR_VERSION}.tgz"
    fi
    if [[ ! -e solr-${SOLR_VERSION}.tgz.md5 ]]; then
      $CURL_CMD -L -O "https://archive.apache.org/dist/lucene/solr/${SOLR_VERSION}/solr-${SOLR_VERSION}.tgz.md5"
    fi
    if [[ -e solr-${SOLR_VERSION}.tgz.md5 ]]; then
      $_IF_VERBOSE echo "Checking md5sum attempt: ${i}"
      $_IF_VERBOSE 2>&1 md5sum -c solr-${SOLR_VERSION}.tgz.md5 
      if [[ $? == 0 ]]; then
        $_IF_VERBOSE echo "md5 verified. Download successful."
        break
      fi
      rm solr-${SOLR_VERSION}.tgz
      rm solr-${SOLR_VERSION}.tgz.md5
    fi
    if [[ $i == 5 ]]; then
      echo "Unable to download solr-${SOLR_VERSION}.tgz after 5 attempts!" >&2
      echo "Installation has failed!" >&2
      exit 1
    fi
  done
  $_IF_VERBOSE 2>&1 popd
fi

#### Extract and stage solr package ####
$_IF_VERBOSE 2>&1 tar -zxf /dataverse/downloads/solr-${SOLR_VERSION}
if [[ $? == 0 ]]; then
  $_IF_VERBOSE echo "Extraction successful"
else
  echo "solr-${SOLR_VERSION}.tgz could NOT be extracted!" >&2
  echo "Installation has failed!" >&2
  exit 1
fi

#### Insert custom dataverse schema.xml ####
$_IF_VERBOSE echo "Copying custom dataverse schema.xml to solr:example:collection1"
cp /dataverse/conf/solr/4.6.0/schema.xml ./solr-${SOLR_VERSION}/example/solr/collection1/conf/schema.xml

#### Start solr core ####
$_IF_VERBOSE echo "Starting solr core..."
cd ./solr-${SOLR_VERSION}/example && java -jar start.jar &


$_IF_TERSE echo "Solr ${SOLR_VERSION} legacy install complete!"