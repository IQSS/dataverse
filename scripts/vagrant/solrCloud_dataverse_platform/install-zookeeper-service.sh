#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]];then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${ZOOKEEPER_CFG} ]]; then ZOOKEEPER_CFG='server.1=localhost:2888:3888'; fi

ZOOKEEPER_SERVER_ID='1'

_usage() {
  echo "\nUsage: $0 \[ehiv\]"
  echo "\nSupported options:"
  echo "  -e     Zookeeper ensemble server(s) configuration to add to zoo.cfg."
  echo "  -h     Print this help message."
  echo "  -i     Numeric zookeeper server id (1..255). \[${ZOOKEEPER_SERVER_ID}\]"
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "\n"
}

while getopts :e:h:i:v: FLAG; do
  case $FLAG in
    e)
      ZOOKEEPER_CFG=$OPTARG
      ;;
    h)  #print help
      _usage
      exit 0
      ;;
    i)  #set option solr install directory "i"
      ZOOKEEPER_SERVER_ID=$OPTARG
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

$_IF_TERSE echo "Installing zookeeper-server as a system service using verbosity level: ${OUTPUT_VERBOSITY}"

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

MIN_JAVA_VERSION=1.8
if [[ ( -z ${JAVA_VERSION+x} ) || ( $JAVA_VERSION < $MIN_JAVA_VERSION ) ]]; then
  $_IF_VERBOSE echo "A suitable version of java could not be found."
  $_IF_INFO echo "Preparing yum to install java ..."
  $_IF_VERBOSE 2>&1 rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6
  yummyPkg="java-1.8.0-openjdk-devel"
  $_IF_TERSE echo "Installing ${yummyPkg} ..."
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


#### Install zookeeper-server using yum ####
$_IF_INFO echo "Adding cloudera yum repo for zookeeper-server"
$_IF_VERBOSE pushd /etc/yum.repos.d
$_IF_VERBOSE curl -L -O https://archive.cloudera.com/cdh5/redhat/6/x86_64/cdh/cloudera-cdh5.repo

$_IF_TERSE echo "Installing zookeeper-server ..."
$YUM_CMD -y install zookeeper-server
$_IF_TERSE echo "zookeeper-server installed"


#### Modify zoo.cfg configuration file #### 
$_IF_INFO echo "Adding ensemble servers to the zoo.cfg configuration file"
$_IF_VERBOSE echo "${ZOOKEEPER_CFG}" >> /etc/zookeeper/conf/zoo.cfg


#### init zookeeper-server ####
$_IF_INFO echo "Initialize the zookeeper-server service for server ${ZOOKEEPER_SERVER_ID}"
$_IF_VERBOSE service zookeeper-server init --myid=${ZOOKEEPER_SERVER_ID}


#### start zookeeper-server ####
$_IF_INFO echo "Starting the zookeeper-server service"
$_IF_VERBOSE service zookeeper-server start

$_IF_TERSE echo "zookeeper-server service installed and running"