#!/bin/bash

SOLR_INSTALL_DIR='/opt'
SOLR_DOMAIN_DIR='/opt/solr'
SOLR_OWNER='solr'

while getopts :d:i:s:u:v: FLAG; do
  case $FLAG in
    d)  #set option solr domain directory "d"
      SOLR_DOMAIN_DIR==$OPTARG
      ;;
    i)  #set option solr install directory "i"
      SOLR_INSTALL_DIR=$OPTARG
      ;;
    s)  #set option solr version "s"
      SOLR_VERSION=$OPTARG
      ;;
    u)  #install solr as SOLR_OWNER "u"
      ## user will be created if not exists
      SOLR_OWNER=$OPTARG
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

case $SOLR_VERSION in
  4.6.0*)
    SOLR_VERSION='4.6.0'
    ;;
  4.6.1*)
    SOLR_VERSION='4.6.1'
    ;;
  4.6*)
    SOLR_VERSION='4.6.1'
    ;;
  4.7.0*)
    SOLR_VERSION='4.7.0'
    ;;
  4.7.1*)
    SOLR_VERSION='4.7.1'
    ;;
  4.7.2*)
    SOLR_VERSION='4.7.2'
    ;;
  4.7*)
    SOLR_VERSION='4.7.2'
    ;;
  4.8.0*)
    SOLR_VERSION='4.8.0'
    ;;
  4.8.1*)
    SOLR_VERSION='4.8.1'
    ;;
  4.8*)
    SOLR_VERSION='4.8.1'
    ;;
  4.9.0*)
    SOLR_VERSION='4.9.0'
    ;;
  4.9.1*)
    SOLR_VERSION='4.9.1'
    ;;
  4.9*)
    SOLR_VERSION='4.9.1'
    ;;
  5.0*)
    SOLR_VERSION='5.0.0'
    ;;
  5.1*)
    SOLR_VERSION='5.1.0'
    ;;
  5.2.0*)
    SOLR_VERSION='5.2.0'
    ;;
  5.2.1*)
    SOLR_VERSION='5.2.1'
    ;;
  5.2*)
    SOLR_VERSION='5.2.1'
    ;;
  5.3.0*)
    SOLR_VERSION='5.3.0'
    ;;
  5.3.1*)
    SOLR_VERSION='5.3.1'
    ;;
  5.3.2*)
    SOLR_VERSION='5.3.2'
    ;;
  5.3*)
    SOLR_VERSION='5.3.2'
    ;;
  5.4.0*)
    SOLR_VERSION='5.4.0'
    ;;
  5.4.1*)
    SOLR_VERSION='5.4.1'
    ;;
  5.4*)
    SOLR_VERSION='5.4.1'
    ;;
  *)
    SOLR_VERSION='4.6.0'
    ;;
esac

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

$_IF_TERSE echo "Installing solr version: ${SOLR_VERSION}"

#### Configure solr owner/user ####
$_IF_TERSE echo "Configuring solr service account/user: $SOLR_OWNER"
$_IF_VERBOSE echo "Checking to see if user: $SOLR_OWNER exists"
$_IF_VERBOSE 2>&1 id -u $SOLR_OWNER
if [[ $? == 0 ]]; then
  $_IF_VERBOSE echo "user: $SOLR_OWNER exists."
else
  $_IF_VERBOSE echo "user: $SOLR_OWNER does NOT exists."
  $_IF_INFO echo "Creating solr service account/user: $SOLR_OWNER"
  $_IF_VERBOSE 2>&1 useradd -r -d /var/solr -s /sbin/nologin $SOLR_OWNER
  if [[ $? == 0 ]]; then
    $_IF_VERBOSE echo "user $SOLR_OWNER successfully created"
  else
    echo "Could not create solr service user $SOLR_OWNER" >&2
    echo "Installation has failed!" >&2
  exit 1
fi

#### Test for writable install directory ####
$_IF_VERBOSE echo "Testing for writable installation directory: $SOLR_INSTALL_DIR"
if [[ ! -w $SOLR_INSTALL_DIR ]]; then
  echo "Installation directory: $SOLR_INSTALL_DIR is not writable by user: $USER" >&2
  echo "Installation has failed!" >&2
  exit 1
fi
$_IF_VERBOSE echo "$SOLR_INSTALL_DIR is writable"

#### Create solr domain directory ####
$_IF_VERBOSE echo "Checking to see if solr domain directory: $SOLR_DOMAIN_DIR exists"
if [[ ! -e $SOLR_DOMAIN_DIR ]]; then
  $_IF_VERBOSE echo "$SOLR_DOMAIN_DIR does not exist. Creating it now..."
  $_IF_VERBOSE 2>&1 mkdir -p $SOLR_DOMAIN_DIR
  if [[ $? == 0 ]]; then
    $_IF_VERBOSE echo "$SOLR_DOMAIN_DIR successfully created"
  else
    echo "Could not create solr domain directory: $SOLR_DOMAIN_DIR" >&2
    echo "Installation has failed!" >&2
    exit 1
  fi
else
  $_IF_VERBOSE echo "solr domain directory: $SOLR_DOMAIN_DIR does exist."
fi

#### Check for adequate java ####
$_IF_INFO echo "Checking java ..."
if ( type -p java >/dev/null ); then
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

## Should determine minimum java version from solr requirements
MIN_JAVA_VERSION=1.8
if [[ ( -z ${JAVA_VERSION+x} ) || ( $JAVA_VERSION < $MIN_JAVA_VERSION ) ]]; then
  $_IF_TERSE echo "A suitable version of java could not be found."
  $_IF_TERSE echo "Installing Java..."
  $yummyPkg = "java-1.8.0-openjdk-devel"
  $YUM_CMD install -y $yummyPkg
  if ( type -p alternatives ); then
    alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java
    alternatives --set javac /usr/lib/jvm/java-1.8.0-openjdk.x86_64/bin/javac
  fi
  $_IF_INFO echo "${yummyPkg} installed"
else
  $_IF_INFO echo "java version: ${JAVA_VERSION} is acceptable"
fi

#### Download solr from apache archive ####
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
    $_IF_VERBOSE echo "Checking md5sum (attempt: ${i})"
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

#### Extract and stage in the solr installation directory ####
$_IF_VERBOSE echo "Extracting solr-${SOLR_VERSION}.tgz to $SOLR_INSTALL_DIR"
tar -C $SOLR_INSTALL_DIR -zxf solr-${SOLR_VERSION}.tgz
if [[ $? == 0 ]]; then
  $_IF_VERBOSE echo "Extraction successful."
else
  echo "solr-${SOLR_VERSION}.tgz could NOT be extracted to $SOLR_INSTALL_DIR!" >&2
  echo "Installation has failed!" >&2
  exit 1
fi
$_IF_VERBOSE echo "Creating link ${SOLR_INSTALL_DIR}/solr -> ${SOLR_INSTALL_DIR}/solr-${SOLR_VERSION}"
ln -s "${SOLR_INSTALL_DIR}/solr-${SOLR_VERSION}" "${SOLR_INSTALL_DIR}/solr"

#### Transfer ownership to SOLR_OWNER ####
chown -R ${SOLR_OWNER}:${SOLR_OWNER} ${SOLR_INSTALL_DIR}/solr-${SOLR_VERSION}
chown -R ${SOLR_OWNER}:${SOLR_OWNER} ${SOLR_INSTALL_DIR}/solr
chown -R ${SOLR_OWNER}:${SOLR_OWNER} $SOLR_DOMAIN_DIR

$_IF_TERSE echo "solr-$SOLR_VERSION installed successfully!"