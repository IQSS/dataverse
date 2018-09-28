#!/bin/bash

#This script gets all the instances from ec2 and sends terminate to them
#Its pretty basic and probably shouldn't be trusted at this point. Namely:
# - You can kill instances other people are using
# - It will try to kill instances that are already dead, which makes output hard to read
# - If it fails for some reason it's hard to tell the script didn't work right

INSTANCES=$(aws ec2 describe-instances --query 'Reservations[].Instances[].[InstanceId]' --output text)

aws ec2 terminate-instances --instance-ids $INSTANCES