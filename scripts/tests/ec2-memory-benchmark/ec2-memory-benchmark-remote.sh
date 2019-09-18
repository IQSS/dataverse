#!/bin/sh

DATAVERSE_APP_DIR=/usr/local/glassfish4/glassfish/domains/domain1/applications/dataverse; export DATAVERSE_APP_DIR

# restart glassfish: 

systemctl restart glassfish

# obtain the pid of the running glassfish process:

GLASSFISH_PID=`ps awux | grep ^glassfi | awk '{print $2}' `; export GLASSFISH_PID 

if [ ${GLASSFISH_PID}"x" = "x" ]
then
    echo "Failed to obtain the process id of the running Glassfish process!"
    exit 1;
fi


# install gnuplot: 

yum -y install gnuplot >/dev/null 

# install gplotpl if necessary:

# 

# bombard the root dataverse page with 20,000 GETs:

for ((i = 0; i < 200; i++))
do
    for ((j = 0; j < 20; j++))
    do
	# 5 GETs on the root dataverse, then sleep for 1 second:
	for ((k = 0; k < 5; k++))
	do
	    # hide the output, standard and stderr:
	    curl http://localhost:8080/dataverse/root 2>/dev/null > /dev/null
	done

	sleep 1
    done

    # after every 100 GETs, run jmap and save the output in a temp file:

    sudo -u glassfish jmap -histo ${GLASSFISH_PID} > /tmp/jmap.histo.out

    # select the dataverse classes from the histo output: 
    grep '  edu\.harvard\.iq\.dataverse' /tmp/jmap.histo.out

    echo

    # plus the totals:
    tail -1 /tmp/jmap.histo.out

    echo 

    # run jstat to check on GC: 
    sudo -u glassfish jstat -gcutil ${GLASSFISH_PID} 1000 1 2>/dev/null

    echo 
done > $DATAVERSE_APP_DIR/memory-benchmark-out.txt


# Create a simple plot of page GETs vs. memory utilisation: 

grep '^  0\.00' $DATAVERSE_APP_DIR/memory-benchmark-out.txt  | awk '{print (NR*100),$4}' > /tmp/benchmark.data
gplot.pl -type png -outfile $DATAVERSE_APP_DIR/benchmark.png -xlabel 'dataverse page GETs' -ylabel 'oldgen (%)' -title 'Memory Utilisation' /tmp/benchmark.data
