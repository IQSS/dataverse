#!/bin/bash -e

# For docs, see the "Deployment" page in the Dev Guide.

# repo and branch defaults
REPO_URL_DEFAULT='https://github.com/IQSS/dataverse.git'
BRANCH_DEFAULT='develop'
PEM_DEFAULT=${HOME}
AWS_AMI_DEFAULT='ami-9887c6e7'

usage() {
  echo "Usage: $0 -b <branch> -r <repo> -p <pem_dir> -g <group_vars> -a <dataverse-ansible branch> -i aws_image -s aws_size -t aws_tag -l local_log_path" 1>&2
  echo "default branch is develop"
  echo "default repo is https://github.com/IQSS/dataverse"
  echo "default .pem location is ${HOME}"
  echo "example group_vars may be retrieved from https://raw.githubusercontent.com/IQSS/dataverse-ansible/master/defaults/main.yml"
  echo "default AWS AMI ID is $AWS_AMI_DEFAULT"
  echo "default AWS size is t2.medium"
  echo "local log path"
  exit 1
}

while getopts ":a:r:b:g:p:i:s:t:l:" o; do
  case "${o}" in
  a)
    DA_BRANCH=${OPTARG}
    ;;
  r)
    REPO_URL=${OPTARG}
    ;;
  b)
    BRANCH=${OPTARG}
    ;;
  g)
    GRPVRS=${OPTARG}
    ;;
  p)
    PEM_DIR=${OPTARG}
    ;;
  i)
    AWS_IMAGE=${OPTARG}
    ;;
  s)
    AWS_SIZE=${OPTARG}
    ;;
  t)
    TAG=${OPTARG}
    ;;
  l)
    LOCAL_LOG_PATH=${OPTARG}
    ;;
  *)
    usage
    ;;
  esac
done

# test for ansible group_vars
if [ ! -z "$GRPVRS" ]; then
   GVFILE=$(basename "$GRPVRS")
   GVARG="-e @$GVFILE"
   echo "using $GRPVRS for extra vars"
fi

# test for CLI args
if [ ! -z "$REPO_URL" ]; then
   GVARG+=" -e dataverse_repo=$REPO_URL"
   echo "using repo $REPO_URL"
fi

if [ ! -z "$BRANCH" ]; then
   GVARG+=" -e dataverse_branch=$BRANCH"
   echo "building branch $BRANCH"
fi

# The AMI ID may change in the future and the way to look it up is with the following command, which takes a long time to run:
# aws ec2 describe-images  --owners 'aws-marketplace' --filters 'Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce' --query 'sort_by(Images, &CreationDate)[-1].[ImageId]' --output 'text'
# To use an AMI, one must subscribe to it via the AWS GUI.
# AMI IDs are specific to the region.

if [ ! -z "$AWS_IMAGE" ]; then
   AMI_ID=$AWS_IMAGE
else
   AMI_ID="$AWS_AMI_DEFAULT"
fi 
echo "using $AMI_ID"

if [ ! -z "$AWS_SIZE" ]; then
   SIZE=$AWS_SIZE
else
   SIZE="t2.medium"
fi
echo "using $SIZE"

if [ ! -z "$TAG" ]; then
   TAGARG="--tag-specifications ResourceType=instance,Tags=[{Key=name,Value=$TAG}]"
   echo "using tag $TAG"
fi

# default to dataverse-ansible/master
if [ -z "$DA_BRANCH" ]; then
   DA_BRANCH="master"
fi

# ansible doesn't care about pem_dir (yet)
if [ -z "$PEM_DIR" ]; then
   PEM_DIR="$PEM_DEFAULT"
fi

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

PRIVATE_KEY=$(aws ec2 create-key-pair --key-name $PEM_DIR/$KEY_NAME --query 'KeyMaterial' --output text)
if [[ $PRIVATE_KEY == '-----BEGIN RSA PRIVATE KEY-----'* ]]; then
  PEM_FILE="$PEM_DIR/$KEY_NAME.pem"
  printf -- "$PRIVATE_KEY" >$PEM_FILE
  chmod 400 $PEM_FILE
  echo "Your newly created private key file is \"$PEM_FILE\". Keep it secret. Keep it safe."
else
  echo "Could not create key pair. Exiting."
  exit 1
fi

echo "Creating EC2 instance"
# TODO: Add some error checking for "ec2 run-instances".
INSTANCE_ID=$(aws ec2 run-instances --image-id $AMI_ID --security-groups $SECURITY_GROUP $TAGARG --count 1 --instance-type $SIZE --key-name $PEM_DIR/$KEY_NAME --query 'Instances[0].InstanceId' --block-device-mappings '[ { "DeviceName": "/dev/sda1", "Ebs": { "DeleteOnTermination": true } } ]' | tr -d \")
echo "Instance ID: "$INSTANCE_ID
echo "giving instance 60 seconds to wake up..."
sleep 60
echo "End creating EC2 instance"

PUBLIC_DNS=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[*].Instances[*].[PublicDnsName]" --output text)
PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[*].Instances[*].[PublicIpAddress]" --output text)

USER_AT_HOST="centos@${PUBLIC_DNS}"
echo "New instance created with ID \"$INSTANCE_ID\". To ssh into it:"
echo "ssh -i $PEM_FILE $USER_AT_HOST"

echo "Please wait at least 15 minutes while the branch \"$BRANCH\" from $REPO_URL is being deployed."

if [ ! -z "$GRPVRS" ]; then
   scp -i $PEM_FILE -o 'StrictHostKeyChecking no' -o 'UserKnownHostsFile=/dev/null' -o 'ConnectTimeout=300' $GRPVRS $USER_AT_HOST:$GVFILE
fi

# epel-release is installed first to ensure the latest ansible is installed after
# TODO: Add some error checking for this ssh command.
ssh -T -i $PEM_FILE -o 'StrictHostKeyChecking no' -o 'UserKnownHostsFile=/dev/null' -o 'ConnectTimeout=300' $USER_AT_HOST <<EOF
sudo yum -y install epel-release
sudo yum -y install https://releases.ansible.com/ansible/rpm/release/epel-7-x86_64/ansible-2.7.9-1.el7.ans.noarch.rpm
sudo yum -y install git nano
git clone -b $DA_BRANCH https://github.com/IQSS/dataverse-ansible.git dataverse
export ANSIBLE_ROLES_PATH=.
ansible-playbook -v -i dataverse/inventory dataverse/dataverse.pb --connection=local $GVARG
EOF

if [ ! -z "$LOCAL_LOG_PATH" ]; then
   echo "copying logs to $LOCAL_LOG_PATH."
   # 1 accept SSH keys
   ssh-keyscan ${PUBLIC_DNS} >> ~/.ssh/known_hosts
   # 2 logdir should exist
   mkdir -p $LOCAL_LOG_PATH
   # 3 grab logs for local processing in jenkins
   rsync -av -e "ssh -i $PEM_FILE" --ignore-missing-args centos@$PUBLIC_DNS:/tmp/dataverse/target/site $LOCAL_LOG_PATH/
   rsync -av -e "ssh -i $PEM_FILE" --ignore-missing-args centos@$PUBLIC_DNS:/tmp/dataverse/target/surefire-reports $LOCAL_LOG_PATH/
   rsync -av -e "ssh -i $PEM_FILE" centos@$PUBLIC_DNS:/usr/local/glassfish4/glassfish/domains/domain1/logs/server* $LOCAL_LOG_PATH/
   # 4 grab mvn.out
   rsync -av -e "ssh -i $PEM_FILE" --ignore-missing-args centos@$PUBLIC_DNS:/tmp/dataverse/mvn.out $LOCAL_LOG_PATH/
   # 5 jacoco
   rsync -av -e "ssh -i $PEM_FILE" --ignore-missing-args centos@$PUBLIC_DNS:/tmp/dataverse/target/coverage-it $LOCAL_LOG_PATH/
   rsync -av -e "ssh -i $PEM_FILE" --ignore-missing-args centos@$PUBLIC_DNS:/tmp/dataverse/target/*.exec $LOCAL_LOG_PATH/
   rsync -av -e "ssh -i $PEM_FILE" --ignore-missing-args centos@$PUBLIC_DNS:/tmp/dataverse/target/classes $LOCAL_LOG_PATH/
   rsync -av -e "ssh -i $PEM_FILE" --ignore-missing-args centos@$PUBLIC_DNS:/tmp/dataverse/src $LOCAL_LOG_PATH/
fi

# Port 8080 has been added because Ansible puts a redirect in place
# from HTTP to HTTPS and the cert is invalid (self-signed), forcing
# the user to click through browser warnings.
CLICKABLE_LINK="http://${PUBLIC_DNS}"
echo "To ssh into the new instance:"
echo "ssh -i $PEM_FILE $USER_AT_HOST"
echo "Branch $BRANCH from $REPO_URL has been deployed to $CLICKABLE_LINK"
echo "When you are done, please terminate your instance with:"
echo "aws ec2 terminate-instances --instance-ids $INSTANCE_ID"
