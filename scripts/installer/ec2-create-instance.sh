#!/bin/bash -e

# For docs, see the "Deployment" page in the Dev Guide.

# repo and branch defaults
REPO_URL='https://github.com/IQSS/dataverse.git'
BRANCH='develop'

usage() {
  echo "Usage: $0 -b <branch> -r <repo> -e <environment file>" 1>&2
  echo "default branch is develop"
  echo "default repo is https://github.com/IQSS/dataverse"
  echo "default conf file is ~/.dataverse/ec2.env"
  exit 1
}

while getopts ":r:b:e:" o; do
  case "${o}" in
  r)
    REPO_URL=${OPTARG}
    ;;
  b)
    BRANCH=${OPTARG}
    ;;
  e)
    EC2ENV=${OPTARG}
    ;;
  *)
    usage
    ;;
  esac
done

# test for user-supplied conf files
if [ ! -z "$EC2ENV" ]; then
   CONF=$EC2ENV
elif [ -f ~/.dataverse/ec2.env ]; then
   echo "using environment variables specified in ~/.dataverse/ec2.env."
   echo "override with -e <conf file>"
   CONF="$HOME/.dataverse/ec2.env"
else
   echo "no conf file supplied (-e <file>) or found at ~/.dataverse/ec2.env."
   echo "running script with defaults. this may or may not be what you want."
fi
   
# read environment variables from conf file
if [ ! -z "$CONF" ];then
   set -a
   echo "reading $CONF"
   source $CONF
   set +a
fi

# now build extra-vars string from doi_* env variables
NL=$'\n'
extra_vars="dataverse_branch=$BRANCH dataverse_repo=$REPO_URL"
while IFS='=' read -r name value; do
  if [[ $name == *'doi_'* ]]; then
    extra_var="$name"=${!name}
    extra_var=${extra_var%$NL}
    extra_vars="$extra_vars $extra_var"
  fi
done < <(env)

AWS_CLI_VERSION=$(aws --version)
if [[ "$?" -ne 0 ]]; then
  echo 'The "aws" program could not be executed. Is it in your $PATH?'
  exit 1
fi

if [[ $(git ls-remote --heads $REPO_URL $BRANCH | wc -l) -eq 0 ]]; then
  echo "Branch \"$BRANCH\" does not exist at $REPO_URL"
  usage
  exit 1
fi

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

# The AMI ID may change in the future and the way to look it up is with the
# following command, which takes a long time to run:
#
# aws ec2 describe-images  --owners 'aws-marketplace' --filters 'Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce' --query 'sort_by(Images, &CreationDate)[-1].[ImageId]' --output 'text'
#
# To use this AMI, we subscribed to it from the AWS GUI.
# AMI IDs are specific to the region.
AMI_ID='ami-9887c6e7'
# Smaller than medium lead to Maven and Solr problems.
SIZE='t2.medium'
echo "Creating EC2 instance"
# TODO: Add some error checking for "ec2 run-instances".
INSTANCE_ID=$(aws ec2 run-instances --image-id $AMI_ID --security-groups $SECURITY_GROUP --count 1 --instance-type $SIZE --key-name $KEY_NAME --query 'Instances[0].InstanceId' --block-device-mappings '[ { "DeviceName": "/dev/sda1", "Ebs": { "DeleteOnTermination": true } } ]' | tr -d \")
echo "Instance ID: "$INSTANCE_ID
echo "giving instance 15 seconds to wake up..."
sleep 15
echo "End creating EC2 instance"

PUBLIC_DNS=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[*].Instances[*].[PublicDnsName]" --output text)
PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[*].Instances[*].[PublicIpAddress]" --output text)

USER_AT_HOST="centos@${PUBLIC_DNS}"
echo "New instance created with ID \"$INSTANCE_ID\". To ssh into it:"
echo "ssh -i $PEM_FILE $USER_AT_HOST"

echo "Please wait at least 15 minutes while the branch \"$BRANCH\" from $REPO_URL is being deployed."

# epel-release is installed first to ensure the latest ansible is installed after
# TODO: Add some error checking for this ssh command.
ssh -T -i $PEM_FILE -o 'StrictHostKeyChecking no' -o 'UserKnownHostsFile=/dev/null' -o 'ConnectTimeout=300' $USER_AT_HOST <<EOF
sudo yum -y install epel-release
sudo yum -y install git nano ansible
git clone https://github.com/IQSS/dataverse-ansible.git dataverse
export ANSIBLE_ROLES_PATH=.
echo $extra_vars
ansible-playbook -v -i dataverse/inventory dataverse/dataverse.pb --connection=local --extra-vars "$extra_vars"
EOF

# Port 8080 has been added because Ansible puts a redirect in place
# from HTTP to HTTPS and the cert is invalid (self-signed), forcing
# the user to click through browser warnings.
CLICKABLE_LINK="http://${PUBLIC_DNS}:8080"
echo "To ssh into the new instance:"
echo "ssh -i $PEM_FILE $USER_AT_HOST"
echo "Branch \"$BRANCH\" from $REPO_URL has been deployed to $CLICKABLE_LINK"
echo "When you are done, please terminate your instance with:"
echo "aws ec2 terminate-instances --instance-ids $INSTANCE_ID"
