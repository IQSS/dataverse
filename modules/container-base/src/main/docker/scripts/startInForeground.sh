#!/bin/bash
##########################################################################################################
#
# This script is to execute Payara Server in foreground, mainly in a docker environment.
# It allows to avoid running 2 instances of JVM, which happens with the start-domain --verbose command.
#
# Usage:
#   Running
#        startInForeground.sh <arguments>
#   is equivalent to running
#        asadmin start-domain <arguments>
#
# It's possible to use any arguments of the start-domain command as arguments to startInForeground.sh
#
# Environment variables used:
#   - $ADMIN_USER - the username to use for the asadmin utility.
#   - $PASSWORD_FILE - the password file to use for the asadmin utility.
#   - $PREBOOT_COMMANDS - the pre boot command file.
#   - $POSTBOOT_COMMANDS - the post boot command file.
#   - $DOMAIN_NAME - the name of the domain to start.
#   - $JVM_ARGS - extra JVM options to pass to the Payara Server instance.
#   - $AS_ADMIN_MASTERPASSWORD - the master password for the Payara Server instance.
#
# This script executes the asadmin tool which is expected at ~/appserver/bin/asadmin.
#
##########################################################################################################
#
#  This script is a fork of https://github.com/payara/Payara/blob/master/appserver/
#  extras/docker-images/server-full/src/main/docker/bin/startInForeground.sh and licensed under CDDL 1.1
#  by the Payara Foundation.
#
##########################################################################################################

# Check required variables are set
if [ -z "$PAYARA_ADMIN_USER" ]; then echo "Variable ADMIN_USER is not set."; exit 1; fi
if [ -z "$PAYARA_ADMIN_PASSWORD" ]; then echo "Variable ADMIN_PASSWORD is not set."; exit 1; fi
if [ -z "$DOMAIN_PASSWORD" ]; then echo "Variable DOMAIN_PASSWORD is not set."; exit 1; fi
if [ -z "$PREBOOT_COMMANDS_FILE" ]; then echo "Variable PREBOOT_COMMANDS_FILE is not set."; exit 1; fi
if [ -z "$POSTBOOT_COMMANDS_FILE" ]; then echo "Variable POSTBOOT_COMMANDS_FILE is not set."; exit 1; fi
if [ -z "$DOMAIN_NAME" ]; then echo "Variable DOMAIN_NAME is not set."; exit 1; fi

# Check if dumps are enabled - add arg to JVM_ARGS in this case
if [ -n "${ENABLE_DUMPS}" ] && [ "${ENABLE_DUMPS}" = "1" ]; then
  JVM_ARGS="${JVM_DUMPS_ARG} ${JVM_ARGS}"
fi

# For safety reasons, do no longer expose the passwords - malicious code could extract it!
# (We need to save the master password for booting the server though)
MASTER_PASSWORD="${DOMAIN_PASSWORD}"
export LINUX_PASSWORD="have-some-scrambled-eggs"
export PAYARA_ADMIN_PASSWORD="have-some-scrambled-eggs"
export DOMAIN_PASSWORD="have-some-scrambled-eggs"

# The following command gets the command line to be executed by start-domain
# - print the command line to the server with --dry-run, each argument on a separate line
# - remove -read-string argument
# - surround each line except with parenthesis to allow spaces in paths
# - remove lines before and after the command line and squash commands on a single line

# Create pre and post boot command files if they don't exist
touch "$POSTBOOT_COMMANDS_FILE" || exit 1
touch "$PREBOOT_COMMANDS_FILE" || exit 1

# This workaround is necessary due to limitations of asadmin
PASSWORD_FILE=$(mktemp)
echo "AS_ADMIN_MASTERPASSWORD=$MASTER_PASSWORD" > "$PASSWORD_FILE"
# shellcheck disable=SC2068
#   -- Using $@ is necessary here as asadmin cannot deal with options enclosed in ""!
OUTPUT=$("${PAYARA_DIR}"/bin/asadmin --user="${PAYARA_ADMIN_USER}" --passwordfile="$PASSWORD_FILE" start-domain --dry-run --prebootcommandfile="${PREBOOT_COMMANDS_FILE}" --postbootcommandfile="${POSTBOOT_COMMANDS_FILE}" $@ "$DOMAIN_NAME")
STATUS=$?
rm "$PASSWORD_FILE"
if [ "$STATUS" -ne 0 ]
  then
    echo ERROR: "$OUTPUT" >&2
    exit 1
fi

echo "Booting now..."

COMMAND=$(echo "$OUTPUT"\
 | sed -n -e '2,/^$/p'\
 | sed "s|glassfish.jar|glassfish.jar $JVM_ARGS |g")

echo Executing Payara Server with the following command line:
echo "$COMMAND" | tr ' ' '\n'
echo

# Run the server in foreground - read master password from variable or file or use the default "changeit" password
# shellcheck disable=SC2086
#   -- Unquoted exec var is necessary, as otherwise things get escaped that may not be escaped (parameters for Java)
exec ${COMMAND} < <(echo "AS_ADMIN_MASTERPASSWORD=$MASTER_PASSWORD")
