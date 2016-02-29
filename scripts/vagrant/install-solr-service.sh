#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

SOLR_INSTALL_DIR='/opt'
SOLR_HOME_DIR='/var/solr'
SOLR_VERSION='5.0.0'
SOLR_OWNER='solr'

if [[ -z ${OUTPUT_VERBOSITY} ]];then OUTPUT_VERBOSITY='1'; fi

_usage() {
  echo "\nUsage: $0 \[dhilmsuv\]"
  echo "\nSupported options:"
  echo "  -h     Print this help message."
  echo "  -i     Directory in which to extract th solr installation. \[${SOLR_INSTALL_DIR}\]"
  echo "  -m     Directory for solr site files. \[${SOLR_HOME_DIR}\]"
  echo "  -s     Select the solr version to install.\[${SOLR_VERSION}\]"
  echo "  -u     User the solr process will run as, this user also will own the solr files. \[${SOLR_OWNER}\]"
  echo "          This user account will be created as a system account if it doesn't already exist!"
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "  -x     Network accessible Hostname/IP address for solr server. \[\]"
  echo "\n"
}

while getopts :h:i:m:s:u:v:x: FLAG; do
  case $FLAG in
    h)  #print help
      _usage
      exit 0
      ;;
    i)  #set option solr install directory "i"
      SOLR_INSTALL_DIR=$OPTARG
      ;;
    m)  #set option solr home directory "h"
      SOLR_HOME_DIR=$OPTARG
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
    x)  #solr server hostname/IP "x"
      SOLR_HOSTNAME=$OPTARG
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
    echo "Installing solr as a service requires solr version 5+" >&2
    echo "  This installation script currently supports versions 5.0.0 - 5.4.1" >&2
    echo "  Please select an appropriate solr version and try again!" >&2
    echo "Installation failed!" >&2
    exit 1
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

$_IF_TERSE echo "Installing solr-${SOLR_VERSION} as a platform service using verbosity level: ${OUTPUT_VERBOSITY}"

#### Test for writable installation directory ####
$_IF_VERBOSE echo "Testing for writable solr installation directory: $SOLR_INSTALL_DIR"
if [[ ! -e $SOLR_INSTALL_DIR ]]; then
  $_IF_VERBOSE echo "$SOLR_INSTALL_DIR does not exist. Creating it now..."
  $_IF_VERBOSE 2>&1 mkdir -p $SOLR_INSTALL_DIR
  if [[ $? == 0 ]]; then
    $_IF_VERBOSE echo "$SOLR_INSTALL_DIR successfully created"
  else
    echo "Could not create solr installation directory: $SOLR_INSTALL_DIR" >&2
    echo "Installation has failed!" >&2
    exit 1
  fi
else
  if [[ ! -w $SOLR_INSTALL_DIR ]]; then
    echo "Installation directory: $SOLR_INSTALL_DIR is not writable by user: $USER" >&2
    echo "Installation has failed!" >&2
    exit 1
  fi
fi
$_IF_VERBOSE echo "$SOLR_INSTALL_DIR is writable"


#### Check for adequate java ####
$_IF_INFO echo "Checking java ..."
if [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
  $_IF_VERBOSE echo "found java executable in JAVA_HOME"     
  _java="$JAVA_HOME/bin/java"
elif ( type -p java >/dev/null 2>&1 ); then
  $_IF_VERBOSE echo "found java executable in PATH" 
  _java=java
fi

JAVA_VERSION=0
if [[ "$_java" ]]; then
  JAVA_VERSION=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
  $_IF_VERBOSE echo "java version: ${JAVA_VERSION}"
fi

## Should determine minimum and possibly maximum java version from solr requirements
MIN_JAVA_VERSION=1.8
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

$_IF_TERSE echo "Installing solr version: ${SOLR_VERSION}"

#### Test for /dataverse/downloads directory and existing .tgz/.tgz.md5 pair ####
$_IF_VERBOSE echo "Checking /dataverse/downloads for solr-${SOLR_VERSION}.tgz and solr-${SOLR_VERSION}.tgz.md5"
if [[ ( -e "/dataverse/downloads/solr-${SOLR_VERSION}.tgz" ) && ( -e  "/dataverse/downloads/solr-${SOLR_VERSION}.tgz.md5" ) ]]; then
  $_IF_VERBOSE pushd /dataverse/downloads
  $_IF_VERBOSE echo "Found! Checking md5sum ..."
  $_IF_VERBOSE 2>&1 md5sum -c solr-${SOLR_VERSION}.tgz.md5 
  if [[ $? == 0 ]]; then
    $_IF_VERBOSE echo "md5 verified."
    _download_solr=0
  else
    $_IF_VERBOSE echo "md5 check failed!"
    $_IF_VERBOSE rm -f "/dataverse/downloads/solr-${SOLR_VERSION}.tgz"
    $_IF_VERBOSE rm -f "/dataverse/downloads/solr-${SOLR_VERSION}.tgz.md5"
  fi
  $_IF_VERBOSE popd
fi

#### Configure download location ####
if [[ -e /dataverse/downloads ]]; then 
  $_IF_VERBOSE pushd /dataverse/downloads
fi

if [[ ( $_download_solr != 0 ) ]]; then
  #### Download solr package from apache archive ####
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
fi

#### Extract/Run the package's install_solr_service.sh script ####
tar -zxf solr-${SOLR_VERSION}.tgz solr-${SOLR_VERSION}/bin/install_solr_service.sh

$_IF_VERBOSE echo "running solr-${SOLR_VERSION}/bin/install_solr_service.sh packaged install script"
SOLR_INSTALL_OPTS="-d ${SOLR_HOME_DIR} -i ${SOLR_INSTALL_DIR} -u ${SOLR_OWNER}"
solr-${SOLR_VERSION}/bin/install_solr_service.sh ./solr-${SOLR_VERSION}.tgz $SOLR_INSTALL_OPTS

if [[ -z ${SOLR_HOSTNAME} ]]; then 
  SOLR_HOSTNAME=`hostname -f`
else
  $_IF_INFO echo "Adding specified host ${SOLR_HOSTNAME} to /etc/default/solr.in.sh"
  $_IF_VERBOSE echo "SOLR_HOST=${SOLR_HOSTNAME}" >> /etc/default/solr.in.sh
fi

if [[ -e /dataverse/downloads ]]; then 
  $_IF_VERBOSE popd
fi

$_IF_TERSE echo "solr ${SOLR_VERSION} has been installed."
$_IF_TERSE echo "Please restart the solr service to fully enable custom configurations"
