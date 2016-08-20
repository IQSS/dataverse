#!/bin/bash
## source-able script to control shell output verbosity

_IF_TERSE='eval >/dev/null'
_IF_INFO='eval >/dev/null'
_IF_VERBOSE='eval >/dev/null'
case $OUTPUT_VERBOSITY in
  [sS0] | [sS][iI][lL][eE][nN][tT] )
    YUM_CMD='yum -q'
    CURL_CMD='curl -s'
    WGET_CMD='wget -q'
    MVN_CMD='mvn -q'
    ;;
  [tT1] | [tT][eE][rR][sS][eE] )
    YUM_CMD='yum -q'
    CURL_CMD='curl -s'
    WGET_CMD='wget -q'
    MVN_CMD='mvn -q'
    _IF_TERSE=''
    ;;
  [iI2] | [iI][nN][fF][oO] )
    YUM_CMD='yum'
    CURL_CMD='curl'
    WGET_CMD='wget'
    MVN_CMD='mvn'
    _IF_TERSE=''
    _IF_INFO=''
    ;;
  [vV3] | [vV][eE][rR][bB][oO][sS][eE] )
    YUM_CMD='yum'
    CURL_CMD='curl'
    WGET_CMD='wget'
    MVN_CMD='mvn'
    _IF_TERSE=''
    _IF_INFO=''
    _IF_VERBOSE=''
    ;;
  * ) ## Default to verbosity level 3 if not set
    YUM_CMD='yum'
    CURL_CMD='curl'
    WGET_CMD='wget'
    MVN_CMD='mvn'
    _IF_TERSE=''
    _IF_INFO=''
    _IF_VERBOSE=''
    ;;
esac
