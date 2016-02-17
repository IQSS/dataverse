#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

SOLR_INSTALL_DIR='/opt'
SOLR_HOME_DIR='/var/solr'
SOLR_DOMAIN_DIR='/var/solr/data'
SOLR_LOG_DIR='/var/solr/logs'
SOLR_VERSION='4.6.1'
SOLR_OWNER='solr'
OUTPUT_VERBOSITY=1

_usage() {
  echo "\nUsage: $0 \[dhilmsuv\]"
  echo "\nSupported options:"
  echo "  -d     Directory for solr core/collection files, such as index data. \[${SOLR_DOMAIN_DIR}\]"
  echo "  -h     Print this help message.
  echo "  -i     Directory in which to extract th solr installation. \[${SOLR_INSTALL_DIR}\]"
  echo "  -l     Directory for writing solr log files. \[${SOLR_LOG_DIR}\]"
  echo "  -m     Directory for solr site files. \[${SOLR_HOME_DIR}\]"
  echo "  -s     Select the solr version to install.\[${SOLR_VERSION}\]"
  echo "  -u     User the solr process will run as, this user also will own the solr files. \[${SOLR_OWNER}\]"
  echo "          This user account will be created as a system account if it doesn't already exist!"
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "\n"
}

while getopts :d:h:i:l:m:s:u:v: FLAG; do
  case $FLAG in
    d)  #set option solr domain directory "d"
      SOLR_DOMAIN_DIR=$OPTARG
      ;;
    h)  #print help
      _usage
      exit 0
      ;;
    i)  #set option solr install directory "i"
      SOLR_INSTALL_DIR=$OPTARG
      ;;
    l)  #set option solr logs directory "l"
      SOLR_LOG_DIR=$OPTARG
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
  4.10.0*)
    SOLR_VERSION='4.10.0'
    ;;
  4.10.1*)
    SOLR_VERSION='4.10.1'
    ;;
  4.10.2*)
    SOLR_VERSION='4.10.2'
    ;;
  4.10.3*)
    SOLR_VERSION='4.10.3'
    ;;
  4.10.4*)
    SOLR_VERSION='4.10.4'
    ;;
  4.10*
    SOLR_VERSION='4.10.4'
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
    SOLR_VERSION='4.6.1'
    ;;
esac


#### Determine startup style based on solr version ####
## 0: 'java -jar start.jar &'
## 1: '/bin/solr ...'
case $SOLR_VERSION in
  4.[0-9].*)
    SOLR_STARTUP_STYLE=0
    ;;
  4.10.*)
    SOLR_STARTUP_STYLE=1
    ;;
  5.*)
    SOLR_STARTUP_STYLE=1
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


#### Create solr home/data directory ####
$_IF_VERBOSE echo "Configuring solr home directory: ${SOLR_HOME_DIR}"
if [[ ! -e $SOLR_HOME_DIR ]]; then
  $_IF_VERBOSE echo "$SOLR_HOME_DIR does not exist. Creating it now..."
  $_IF_VERBOSE 2>&1 mkdir -p $SOLR_HOME_DIR
  if [[ $? == 0 ]]; then
    $_IF_VERBOSE echo "$SOLR_HOME_DIR successfully created"
  else
    echo "Could not create solr home directory: $SOLR_HOME_DIR" >&2
    echo "Installation has failed!" >&2
    exit 1
  fi
fi
$_IF_VERBOSE echo "solr home directory: $SOLR_HOME_DIR configured."


#### Configure solr owner/user ####
$_IF_TERSE echo "Configuring solr service account/user: $SOLR_OWNER"
$_IF_VERBOSE echo "Checking to see if user ${SOLR_OWNER} exists"
_solr_uid="`id -u ${SOLR_OWNER} 2>&1`"
if [[ $? == 0 ]]; then
  $_IF_VERBOSE echo "user $SOLR_OWNER exists with uid $_solr_uid"
else
  $_IF_VERBOSE echo "user $SOLR_OWNER does NOT exists"
  $_IF_INFO echo "Creating solr service account/user: $SOLR_OWNER"
  $_IF_VERBOSE 2>&1 useradd -r -d $SOLR_HOME -s /sbin/nologin $SOLR_OWNER
  if [[ $? == 0 ]]; then
    $_IF_VERBOSE echo "user $SOLR_OWNER successfully created"
  else
    echo "Could not create solr service user $SOLR_OWNER" >&2
    echo "Installation has failed!" >&2
    exit 1
  fi
fi


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


#### Create solr domain directory ####
$_IF_VERBOSE echo "Configuring solr domain directory: ${SOLR_DOMAIN_DIR}"
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
fi
$_IF_VERBOSE echo "solr domain directory: $SOLR_DOMAIN_DIR configured."


#### Create solr logs directory ####
$_IF_VERBOSE echo "Configuring solr logs directory: ${SOLR_LOG_DIR}"
if [[ ! -e $SOLR_LOG_DIR ]]; then
  $_IF_VERBOSE echo "$SOLR_LOG_DIR does not exist. Creating it now..."
  $_IF_VERBOSE 2>&1 mkdir -p $SOLR_LOG_DIR
  if [[ $? == 0 ]]; then
    $_IF_VERBOSE echo "$SOLR_LOG_DIR successfully created"
  else
    echo "Could not create solr logs directory: $SOLR_LOG_DIR" >&2
    echo "Installation has failed!" >&2
    exit 1
  fi
fi
$_IF_VERBOSE echo "solr logs directory: $SOLR_LOG_DIR configured."


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


#### Download solr package from apache archive ####
$_IF_TERSE echo "Installing solr version: ${SOLR_VERSION}"
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


#### Extract and stage package in the solr installation directory ####
$_IF_VERBOSE echo "Extracting solr-${SOLR_VERSION}.tgz to $SOLR_INSTALL_DIR ..."
tar -C $SOLR_INSTALL_DIR -zxf solr-${SOLR_VERSION}.tgz
if [[ $? == 0 ]]; then
  $_IF_VERBOSE echo "Extraction successful"
else
  echo "solr-${SOLR_VERSION}.tgz could NOT be extracted to $SOLR_INSTALL_DIR!" >&2
  echo "Installation has failed!" >&2
  exit 1
fi
$_IF_VERBOSE echo "Creating link ${SOLR_INSTALL_DIR}/solr -\> ${SOLR_INSTALL_DIR}/solr-${SOLR_VERSION}"
ln -s "${SOLR_INSTALL_DIR}/solr-${SOLR_VERSION}" "${SOLR_INSTALL_DIR}/solr"


#### Install init.d/solr service script ####
$_IF_TERSE echo "Installing /etc/init.d/solr service control script ..."
if [[ -e ${SOLR_INSTALL_DIR}/solr/bin/init.d/solr ]]; then
  $_IF_VERBOSE echo "solr-${SOLR_VERSION} package service control script detected."
  $_IF_VERBOSE echo "Copying ${SOLR_INSTALL_DIR}/solr/bin/init.d/solr to /etc/init.d/"
  cp -e ${SOLR_INSTALL_DIR}/solr/bin/init.d/solr /etc/init.d/solr
elif [[ -e ./solr_init_template ]]; then
  $_IF_VERBOSE echo "No solr package control script was found."
  $_IF_VERBOSE echo "Installing custom solr control script from ./solr_init_template"
  cp -e ./solr_init_template /etc/init.d/solr
else
  $_IF_VERBOSE echo "No solr service control script was found or installed."
  $_IF_TERSE echo "solr has not been registered as an init.d service. The solr service will need to be started manually!"
fi

if [[ -e /etc/init.d/solr ]]; then
  $_IF_INFO echo "/etc/init.d/solr service control script installed"
  $_IF_VERBOSE echo "Setting custom solr service parameters ..."
  chown root:root /etc/init.d/solr
  chmod 0744 /etc/init.d/solr
  
  # do some basic variable substitution on the init.d script
  sed_expr1="s#SOLR_INSTALL_DIR=.*#SOLR_INSTALL_DIR=\"${SOLR_INSTALL_DIR}/solr\"#"
  sed_expr2="s#RUNAS=.*#RUNAS=\"${SOLR_OWNER}\"#"
  sed_expr3="s#Provides:.*#Provides: solr#"
  sed -i -e "$sed_expr1" -e "$sed_expr2" -e "$sed_expr3" "/etc/init.d/solr"
fi


#### Install solr service init configuration file ####
$_IF_TERSE echo "Installing /etc/default/solr.in.sh site init configuration ..."
if [[ ! -d /etc/default ]]; then
  $_IF_VERBOSE echo "Creating /etc/default directory to store site init configurations"
  mkdir /etc/default
  chown root:root /etc/default
  chmod 0755 /etc/default
fi

if [[ -e ${SOLR_INSTALL_DIR}/solr/bin/solr.in.sh ]]; then
  $_IF_VERBOSE echo "solr-${SOLR_VERSION} package site init configuration detected."
  $_IF_VERBOSE echo "Copying ${SOLR_INSTALL_DIR}/solr/bin/solr.in.sh to /etc/default/"
  cp -e ${SOLR_INSTALL_DIR}/solr/bin/solr.in.sh /etc/default/solr.in.sh
elif [[ -e ./solr_site_template ]]; then
  $_IF_VERBOSE echo "No solr package site init configuration template was found."
  $_IF_VERBOSE echo "Installing custom solr site init configuration from ./solr_site_template"
  cp -e ./solr_site_template /etc/default/solr.in.sh
else
  $_IF_VERBOSE echo "No solr site init configuration template was found or installed."
  $_IF_TERSE echo "This solr site initiation procedures may need to be configured manually!"
fi  

if [[ -e /etc/default/solr.in.sh ]]; then
  $_IF_INFO echo "/etc/default/solr.in.sh site init configuration installed"
  $_IF_VERBOSE echo "Setting custom site init configuration parameters ..."

  echo "SOLR_PID_DIR=\"${SOLR_DOMAIN_DIR}\"
SOLR_HOME=\"${SOLR_HOME_DIR}\"
SOLR_PORT=\"8983\"
" >> "/etc/default/solr.in.sh"

  chown root:root /etc/init.d/solr
  chmod 0644 /etc/init.d/solr
fi

if [[ -e /etc/init.d/solr ]]; then
  $_IF_VERBOSE echo "Setting /etc/init.d/solr to see custom site init configuration /etc/default/solr.in.sh ..."
  sed_expr="s#SOLR_ENV=.*#SOLR_ENV=\"/etc/default/solr.in.sh\"#"
  sed -i -e "$sed_expr" "/etc/init.d/solr"
fi


#### Configure solr service log4j properties ####
$_IF_INFO echo "Configuring solr service logging properties"
if [[ -e ${SOLR_INSTALL_DIR}/solr/server/resources/log4j.properties ]]; then
  $_IF_VERBOSE echo "solr-${SOLR_VERSION} package log4j configuration detected."
  $_IF_VERBOSE echo "Copying ${SOLR_INSTALL_DIR}/solr/server/resources/log4j.properties to ${SOLR_HOME_DIR}/"
  cp -e ${SOLR_INSTALL_DIR}/solr/server/resources/log4j.properties ${SOLR_HOME_DIR}/log4j.properties
elif [[ -e ${SOLR_INSTALL_DIR}/solr/example/resources/log4j.properties ]]; then
  $_IF_VERBOSE echo "solr-${SOLR_VERSION} example log4j configuration detected."
  $_IF_VERBOSE echo "Copying ${SOLR_INSTALL_DIR}/solr/example/resources/log4j.properties to ${SOLR_HOME_DIR}/"
  cp -e ${SOLR_INSTALL_DIR}/solr/example/resources/log4j.properties ${SOLR_HOME_DIR}/log4j.properties
elif [[ -e ./solr_log4j_template ]]; then
  $_IF_VERBOSE echo "No solr package log4j configuration template was found."
  $_IF_VERBOSE echo "Installing custom solr log4j configuration from ./solr_log4j_template"
  cp -e ./solr_log4j_template ${SOLR_HOME_DIR}/log4j.properties
else
  $_IF_VERBOSE echo "No solr site log4j template was found or installed."
  $_IF_TERSE echo "This solr sites log4j.properties may need to be configured manually!"
fi 

if [[ -e ${SOLR_HOME_DIR}/log4j.properties ]]; then
  $_IF_INFO echo "${SOLR_HOME_DIR}/log4j.properties installed."
  sed_expr="s#solr.log=.*#solr.log=\${SOLR_LOG_DIR}#"
  sed -i -e "$sed_expr" "${SOLR_HOME_DIR}/log4j.properties"
fi


#### Add logging properties to solr site init configuration #### 
$_IF_VERBOSE echo "Adding solr service log properties to site configuration"
if [[ ( -e ${SOLR_HOME_DIR}/log4j.properties ) && ( -e /etc/default/solr.in.sh ) ]]; then
  $_IF_VERBOSE echo "Adding log4j properties to custom site configuration ..."

  echo "LOG4J_PROPS=\"${SOLR_HOME_DIR}/log4j.properties\"
SOLR_LOGS_DIR=\"${SOLR_LOG_DIR}\"
" >> "/etc/default/solr.in.sh"

fi


#### Configure solr domain settings solr.xml ####
$_IF_INFO echo "Configuring solr domain settings"
if [[ -e ${SOLR_INSTALL_DIR}/solr/server/solr/solr.xml ]]; then
  $_IF_VERBOSE echo "solr-${SOLR_VERSION} package solr.xml configuration detected."
  $_IF_VERBOSE echo "Copying ${SOLR_INSTALL_DIR}/solr/server/solr/solr.xml to ${SOLR_DOMAIN_DIR}/"
  cp -e ${SOLR_INSTALL_DIR}/solr/server/solr/solr.xml ${SOLR_DOMAIN_DIR}/solr.xml
elif [[ -e ${SOLR_INSTALL_DIR}/solr/example/solr/solr.xml ]]; then
  $_IF_VERBOSE echo "solr-${SOLR_VERSION} example solr.xml configuration detected."
  $_IF_VERBOSE echo "Copying ${SOLR_INSTALL_DIR}/solr/example/solr/solr.xml to ${SOLR_DOMAIN_DIR}/"
  cp -e ${SOLR_INSTALL_DIR}/solr/example/solr/solr.xml ${SOLR_DOMAIN_DIR}/solr.xml
elif [[ -e ./solr_solrxml_template ]]; then
  $_IF_VERBOSE echo "No solr package solr.xml configuration template was found."
  $_IF_VERBOSE echo "Installing custom solr.xml configuration from ./solr_solrxml_template"
  cp -e ./solr_solrxml_template ${SOLR_DOMAIN_DIR}/solr.xml
else
  $_IF_VERBOSE echo "No solr domain solr.xml template was found or installed."
  $_IF_TERSE echo "This solr somains solr.xml may need to be configured manually!"
fi 

if [[ -e ${SOLR_DOMAIN_DIR}/solr.xml ]]; then
  $_IF_INFO echo "${SOLR_DOMAIN_DIR}/solr.xml installed."
  ## If there is a need for custom configuration of solr.xml THIS IS THE PLACE!
fi


#### Transfer ownerships to SOLR_OWNER ####
chown -R ${SOLR_OWNER}:${SOLR_OWNER} $SOLR_HOME_DIR
chown -R ${SOLR_OWNER}:${SOLR_OWNER} $SOLR_DOMAIN_DIR
chown -R ${SOLR_OWNER}:${SOLR_OWNER} $SOLR_LOG_DIR
chown -R ${SOLR_OWNER}:${SOLR_OWNER} ${SOLR_INSTALL_DIR}/solr-${SOLR_VERSION}
chown -R ${SOLR_OWNER}:${SOLR_OWNER} ${SOLR_INSTALL_DIR}/solr


#### Attempt to start solr service ####
if [[ -e /etc/init.d/solr ]]; then
  #### Configure solr service autostart ####
  $_IF_INFO echo "Configuring solr to start on system boot"
  $_IF_VERBOSE chkconfig solr on

  #### Start the solr service ####
  $_IF_INFO echo "Starting the solr service"
  $_IF_VERBOSE 2>&1 service solr start
  if [[ $? == 0 ]]; then
    $_IF_TERSE echo "solr-$SOLR_VERSION installed and started"
  else
    $_IF_TERSE echo "solr-$SOLR_VERSION installed but there was a problem starting the service!" >&2
  fi
else
  $_IF_TERSE echo "solr-$SOLR_VERSION installed successfully!"
  $_IF_TERSE echo "This solr service requires a manual start!"
fi
