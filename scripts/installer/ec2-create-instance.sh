#!/bin/bash -x
#Initially Referred to this doc: https://docs.aws.amazon.com/cli/latest/userguide/tutorial-ec2-ubuntu.html

#TODO: allow arbitrary repo, not just IQSS. Will require changing it on the ansible side as well

#Make sure "aws" binary is available
AWS_CLI_VERSION=$(aws --version)
if [[ "$?" -ne 0 ]]; then
  echo 'The "aws" program could not be executed. Is it in your $PATH?'
  exit 1
fi

if [ "$1" = "" ]; then
  echo "No branch name provided"
  exit 1
else
  BRANCH_NAME=$1
  if [[ $(git ls-remote --heads https://github.com/IQSS/dataverse.git $BRANCH_NAME | wc -l) -eq 0 ]]; then
    echo "Branch does not exist on the Dataverse github repo"
    exit 1
  fi
fi

#Create security group if it doesn't already exist
SECURITY_GROUP='dataverse-sg'
GROUP_CHECK=$(aws ec2 describe-security-groups --group-name $SECURITY_GROUP)
if [[ "$?" -ne 0 ]]; then
  echo "Creating security group \"$SECURITY_GROUP\"."
  aws ec2 create-security-group --group-name $SECURITY_GROUP --description "security group for Dataverse"
  aws ec2 authorize-security-group-ingress --group-name $SECURITY_GROUP --protocol tcp --port 22 --cidr 0.0.0.0/0
  aws ec2 authorize-security-group-ingress --group-name $SECURITY_GROUP --protocol tcp --port 80 --cidr 0.0.0.0/0
  aws ec2 authorize-security-group-ingress --group-name $SECURITY_GROUP --protocol tcp --port 443 --cidr 0.0.0.0/0
  aws ec2 authorize-security-group-ingress --group-name $SECURITY_GROUP --protocol tcp --port 8080 --cidr 0.0.0.0/0
fi

RANDOM_STRING="$(uuidgen | cut -c-8)"
KEY_NAME="key-$USER-$RANDOM_STRING"

PRIVATE_KEY=$(aws ec2 create-key-pair --key-name $KEY_NAME --query 'KeyMaterial' --output text)
if [[ $PRIVATE_KEY == '-----BEGIN RSA PRIVATE KEY-----'* ]]; then
  PEM_FILE="$KEY_NAME.pem"
  printf -- "$PRIVATE_KEY" >$PEM_FILE
  chmod 400 $PEM_FILE
  echo "Your newly created private key file is \"$PEM_FILE\". Keep it secret. Keep it safe."
else
  echo "Could not create key pair. Exiting."
  exit 1
fi

#AMI ID for centos7 acquired by this (very slow) query Sept 10th 2018
#This does not need to be run every time, leaving it in here so it is remembered
#aws ec2 describe-images  --owners 'aws-marketplace' --filters 'Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce' --query 'sort_by(Images, &CreationDate)[-1].[ImageId]' --output 'text'

#The AMI ID only works for region us-east-1, for now just forcing that
#Using this image ID a 1-time requires subscription per root account, which was done through the UI
#Also, change the instance size as your own peril. Previous attempts of setting it smaller than medium have caused solr and maven to crash weirdly during install
echo "*Creating ec2 instance"
INSTANCE_ID=$(aws ec2 run-instances --image-id ami-9887c6e7 --security-groups $SECURITY_GROUP --count 1 --instance-type t2.medium --key-name $KEY_NAME --query 'Instances[0].InstanceId' --block-device-mappings '[ { "DeviceName": "/dev/sda1", "Ebs": { "DeleteOnTermination": true } } ]' | tr -d \")
echo "Instance ID: "$INSTANCE_ID
echo "*End creating EC2 instance"

PUBLIC_DNS=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[*].Instances[*].[PublicDnsName]" --output text)
PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[*].Instances[*].[PublicIpAddress]" --output text)

USER_AT_HOST="centos@${PUBLIC_DNS}"
echo "New instance created with ID \"$INSTANCE_ID\". To ssh into it:"
echo "ssh -i $PEM_FILE $USER_AT_HOST"

echo "Please wait at least 15 minutes while the branch \"$BRANCH_NAME\" is being deployed."

#ssh into instance now and run ansible stuff
#Note: an attempt was made to pass the branch name in the ansible-playbook call
# via -e "dataverse.branch=$BRANCH_NAME", but it gets overwritten due to the order
# of operations for where ansible looks for variables.
ssh -T -i $PEM_FILE -o 'StrictHostKeyChecking no' -o 'UserKnownHostsFile=/dev/null' -o 'ConnectTimeout=300' $USER_AT_HOST <<EOF
sudo yum -y install git nano ansible
git clone https://github.com/IQSS/dataverse-ansible.git dataverse
export ANSIBLE_ROLES_PATH=.
sed -i "s/branch:/branch: $BRANCH_NAME/" dataverse/defaults/main.yml
ansible-playbook -i dataverse/inventory dataverse/dataverse.pb --connection=local
EOF

#Port 8080 has been added because Ansible puts a redirect in place
#from HTTP to HTTPS and the cert is invalid (self-signed), forcing
#the user to click through browser warnings.
CLICKABLE_LINK="http://${PUBLIC_DNS}:8080"
echo "To ssh into the new instance:"
echo "ssh -i $PEM_FILE $USER_AT_HOST"
echo "Branch \"$BRANCH_NAME\" has been deployed to $CLICKABLE_LINK"
echo "When you are done, please terminate your instance with:"
echo "aws ec2 terminate-instances --instance-ids $INSTANCE_ID"
