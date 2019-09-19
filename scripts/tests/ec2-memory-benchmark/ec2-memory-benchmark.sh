#!/bin/sh 
dataverse_branch=$1

if [ $dataverse_branch"x" = "x" ]
then
    echo "usage: ./ec2-memory-benchmark.sh <dataverse_branch>"
    exit 1
fi

# download the ec2-create-instance script:
curl -O https://raw.githubusercontent.com/IQSS/dataverse-ansible/master/ec2/ec2-create-instance.sh
chmod 755 ec2-create-instance.sh

# download the sample data ec2 config: 
curl -O https://raw.githubusercontent.com/IQSS/dataverse-sample-data/memory-monitoring-experiments/ec2config_memory_test.yaml

# run the script: 
./ec2-create-instance.sh -a ec2-memory-benchmark -b ${dataverse_branch} -r https://github.com/IQSS/dataverse.git -g ec2config_memory_test.yaml 2>&1 | tee create-instance.log

# obtain the address of the new instance, and the ssh key: 

EC2_SSH_KEY=`grep '^ssh \-i .* centos' create-instance.log | head -1 | awk '{print $3}'`
EC2_SSH_DEST=`grep '^ssh \-i .* centos' create-instance.log | head -1 | awk '{print $4}'`


if [[ "${EC2_SSH_KEY}x" = "x" || "${EC2_SSH_DEST}x" = "x" ]]
then
    echo "Failed to spin up a sample-data branch!"
    echo "(Could not obtain ssh connection key)"
    echo "Consult the log file (create-instance.log) for more details."
    exit 1;
fi

# obtain the http address of the new instance: 

EC2_HTTP_LOCATION=`grep "^Branch ${dataverse_branch} .* has been deployed to " create-instance.log | sed 's/^.* deployed to //'`

if [ ${EC2_HTTP_LOCATION}"x" = "x" ]
then
    echo "Failed to spin up a sample-data branch!"
    echo "(Failed to obtain the http address of the instance)"
    echo "Consult the log file (create-instance.log) for more details."
    exit 1;
fi

# download the benchmark script to be run on the newly-spun instance: 
# (?)

# copy the script: 

scp -i ${EC2_SSH_KEY} -o 'StrictHostKeyChecking no' -o 'UserKnownHostsFile=/dev/null' ec2-memory-benchmark-remote.sh ${EC2_SSH_DEST}:/tmp

# run the remote script: 

ssh -i ${EC2_SSH_KEY} -o 'StrictHostKeyChecking no' -o 'UserKnownHostsFile=/dev/null' -o 'ConnectTimeout=14400' ${EC2_SSH_DEST} 'sudo /tmp/ec2-memory-benchmark-remote.sh'

# check the exit code of the command above here!

echo "Memory benchmark test complete!" 
echo 
echo "The memory utilisation plots should be available here: "
echo "  dataverse page: ${EC2_HTTP_LOCATION}/benchmark-dataverse.png"
echo "  dataset page:   ${EC2_HTTP_LOCATION}/benchmark-dataset.png"
echo "Raw data output (with GC status and jmap listing of all the instantiated Dataverse classes every N page loads,"
echo "where N is 100 and 10, for dataverse and dataset pages respectively):"
echo "  dataverse page: ${EC2_HTTP_LOCATION}/memory-benchmark-raw-dataverse.txt"
echo "  dataset page:   ${EC2_HTTP_LOCATION}/memory-benchmark-raw-dataset.txt"
