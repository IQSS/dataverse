#!/bin/bash

while getopts :s:v FLAG; do
  case $FLAG in
    s)  #set option solr version "s"
      SOLR_VERSION=$OPTARG
      ;;
    v)  #set output verbosity level "v"
      OUTPUT_VERBOSITY=$OPTARG
    \?) #unknown option
      echo "Unknown option: -$OPTARG" >&2
      ;;
    :)  #valid option requires adjacent argument
      exit 1
      ;;
  esac
done

ECHO_IF_TERSE='echo >/dev/null'
ECHO_IF_INFO='echo >/dev/null'
ECHO_IF_VERBOSE='echo >/dev/null'
case $OUTPUT_VERBOSITY in
  [sS0] | [sS][iI][lL][eE][nN][tT] )
    YUM_CMD='yum -q'
    CURL_CMD='curl -s'
    WGET_CMD='wget -q'
    ;;
  [tT1] | [tT][eE][rR][sS][eE] )
    YUM_CMD='yum -q'
    CURL_CMD='curl -s'
    WGET_CMD='wget -q'
    ECHO_IF_TERSE='echo'
    ;;
  [iI2] | [iI][nN][fF][oO] )
    YUM_CMD='yum'
    CURL_CMD='curl'
    WGET_CMD='wget'
    ECHO_IF_TERSE='echo'
    ECHO_IF_INFO='echo'
    ;;
  [vV3] | [vV][eE][rR][bB][oO][sS][eE] )
    YUM_CMD='yum'
    CURL_CMD='curl'
    WGET_CMD='wget'
    ECHO_IF_TERSE='echo'
    ECHO_IF_INFO='echo'
    ECHO_IF_VERBOSE='echo'
    ;;
  * ) ## Default to Terse verbosity if not set
    YUM_CMD='yum -q'
    CURL_CMD='curl -s'
    WGET_CMD='wget -q'
    ECHO_IF_TERSE='echo'
    ;;
esac

$ECHO_IF_INFO "Checking java ..."
if ( type -p java ); then
  $ECOH_IF_VERBOSE "found java executable in PATH"
  _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
  $ECOH_IF_VERBOSE "found java executable in JAVA_HOME"     
  _java="$JAVA_HOME/bin/java"
fi

JAVA_VERSION=0
if [[ "$_java" ]]; then
  JAVA_VERSION=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
  $ECHO_IF_VERBOSE "java version: ${JAVA_VERSION}"
fi

MIN_JAVA_VERSION=1.8
if [[ ( -z ${JAVA_VERSION+x} ) || ( $JAVA_VERSION < $MIN_JAVA_VERSION ) ]]; then
  $ECHO_IF_TERSE "A suitable version of java could not be found."
  $ECHO_IF_TERSE "Installing Java..."
  $yummyPkg = "java-1.8.0-openjdk-devel"
  $YUM_CMD install -y $yummyPkg
  if ( type -p alternatives ); then
    alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java
    alternatives --set javac /usr/lib/jvm/java-1.8.0-openjdk.x86_64/bin/javac
  fi
  $ECHO_IF_TERSE "${yummyPkg} installed"
fi

case $SOLR_VERSION in
  4.6.0*)
    SOLR_VERSION='4.6.0'
    ;;
  4.6.1*)
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
  4.7*
    SOLR_VERSION='4.7.2'
    ;;
  4.8.0*)
    SOLR_VERSION='4.8.0'
    ;;
  4.8.1*)
  4.8*)
    SOLR_VERSION='4.8.1'
    ;;
  4.9.0*)
    SOLR_VERSION='4.9.0'
    ;;
  4.9.1*)
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
  5.3*)
    SOLR_VERSION='5.3.2'
    ;;
  5.4.0*)
    SOLR_VERSION='5.4.0'
    ;;
  5.4.1*)
  5.4*)
    SOLR_VERSION='5.4.1'
    ;;
  *)
    SOLR_VERSION='4.6.0'
    ;;
esac

$ECHO_IF_TERSE "Installing solr version: ${SOLR_VERSION}"
        
#$CURL_CMD -L -O "https://archive.apache.org/dist/lucene/solr/${SOLR_VERSION}/solr-${SOLR_VERSION}.tgz

$ECHO_IF_TERSE "Solr $SOLR_VERSION installed successfully!"