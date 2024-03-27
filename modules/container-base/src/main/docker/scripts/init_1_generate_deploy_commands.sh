#!/bin/bash
##########################################################################################################
#
# A script to append deploy commands to the post boot command file at
# $PAYARA_HOME/scripts/post-boot-commands.asadmin file. All applications in the
# $DEPLOY_DIR (either files or folders) will be deployed.
# The $POSTBOOT_COMMANDS file can then be used with the start-domain using the
#  --postbootcommandfile parameter to deploy applications on startup.
#
# Usage:
# ./generate_deploy_commands.sh
#
# Optionally, any number of parameters of the asadmin deploy command can be
# specified as parameters to this script.
# E.g., to deploy applications with implicit CDI scanning disabled:
#
# ./generate_deploy_commands.sh --properties=implicitCdiEnabled=false
#
# Environment variables used:
#   - $PREBOOT_COMMANDS - the pre boot command file.
#   - $POSTBOOT_COMMANDS - the post boot command file.
#
# Note that many parameters to the deploy command can be safely used only when
# a single application exists in the $DEPLOY_DIR directory.
#
##########################################################################################################
#
#  This script is a fork of https://github.com/payara/Payara/blob/master/appserver/extras/docker-images/
#  server-full/src/main/docker/bin/init_1_generate_deploy_commands.sh and licensed under CDDL 1.1
#  by the Payara Foundation.
#
##########################################################################################################

set -euo pipefail

# Check required variables are set
if [ -z "$DEPLOY_DIR" ]; then echo "Variable DEPLOY_DIR is not set."; exit 1; fi
if [ -z "$PREBOOT_COMMANDS" ]; then echo "Variable PREBOOT_COMMANDS is not set."; exit 1; fi
if [ -z "$POSTBOOT_COMMANDS" ]; then echo "Variable POSTBOOT_COMMANDS is not set."; exit 1; fi

# Create pre and post boot command files if they don't exist
touch "$POSTBOOT_COMMANDS"
touch "$PREBOOT_COMMANDS"

deploy() {

  if [ -z "$1" ]; then
    echo "No deployment specified";
    exit 1;
  fi

  DEPLOY_STATEMENT="deploy $DEPLOY_PROPS $1"
  if grep -q "$1" "$POSTBOOT_COMMANDS"; then
    echo "post boot commands already deploys $1";
  else
    if [ -n "$SKIP_DEPLOY" ] && { [ "$SKIP_DEPLOY" = "1" ] || [ "$SKIP_DEPLOY" = "true" ]; }; then
      echo "Skipping deployment of $1 as requested.";
    else
      echo "Adding deployment target $1 to post boot commands";
      echo "$DEPLOY_STATEMENT" >> "$POSTBOOT_COMMANDS";
    fi
  fi
}

# RAR files first
find "$DEPLOY_DIR" -mindepth 1 -maxdepth 1 -name "*.rar" -print0 \
  | while IFS= read -r -d '' file; do deploy "$file"; done

# Then every other WAR, EAR, JAR or directory
find "$DEPLOY_DIR" -mindepth 1 -maxdepth 1 \
  \( ! -name "*.rar" -a -name "*.war" -o -name "*.ear" -o -name "*.jar" -o -type d \) \
  -print0 | while IFS= read -r -d '' file; do deploy "$file"; done
