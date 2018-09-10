#Refering to this doc: https://docs.aws.amazon.com/cli/latest/userguide/tutorial-ec2-ubuntu.html

#This needs to take in an argument of the branch name

#Create security group if it doesn't already exist
echo "*Creating security group"
aws ec2 create-security-group --group-name devenv-sg --description "security group for development environment"
aws ec2 authorize-security-group-ingress --group-name devenv-sg --protocol tcp --port 22 --cidr 0.0.0.0/0
echo "*End creating security group"

#Create key pair. Does this pem need to be saved or just held temporarilly?
# - Probably held, we probably need another script to blow away our spinned-up ec2 instance
# - Should attach the branch name to the key
echo "*Creating key pair"
aws ec2 create-key-pair --key-name devenv-key --query 'KeyMaterial' --output text > devenv-key.pem
chmod 400 devenv-key.pem
echo "*End creating key pair"

#AMI ID acquired by this (very slow) query Sept 10th 2018
#aws ec2 describe-images  --owners 'aws-marketplace' --filters 'Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce' --query 'sort_by(Images, &CreationDate)[-1].[ImageId]' --output 'text'

echo "*Creating ec2 instance"
aws ec2 run-instances --image-id ami-9887c6e7 --security-groups devenv-sg --count 1 --instance-type t2.micro --key-name devenv-key --query 'Instances[0].InstanceId'
echo "*End creating EC2 instance"