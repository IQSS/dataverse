#!/bin/bash
# https://docs.aws.amazon.com/cli/latest/userguide/controlling-output.html
INSTANCES=$(aws ec2 describe-instances --query 'Reservations[].Instances[].[InstanceId,KeyName,State.Name,PublicDnsName]' --output text)
if [[ "$?" -ne 0 ]]; then
  echo "Error listing instances."
  exit 1
else
  echo "$INSTANCES"
fi
