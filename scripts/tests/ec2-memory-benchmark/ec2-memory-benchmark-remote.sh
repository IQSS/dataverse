#!/bin/sh
EC2_HTTP_LOCATION=$1
if [ -z "${EC2_HTTP_LOCATION}" ]
then
    EC2_HTTP_LOCATION="<EC2 INSTANCE HTTP ADDRESS>"
fi

DATAVERSE_APP_DIR=/usr/local/payara6/glassfish/domains/domain1/applications/dataverse; export DATAVERSE_APP_DIR

# restart app server

echo "Restarting app server..."

# still "glassfish" until Ansible is updated

systemctl restart glassfish

echo "done."

# obtain the pid of the running app server process:

APP_SERVER_PID=`ps awux | grep ^dataver | awk '{print $2}' `; export APP_SERVER_PID

if [ ${APP_SERVER_PID}"x" = "x" ]
then
    echo "Failed to obtain the process id of the running app server process!"
    exit 1;
fi


# install gnuplot, quietly: 

yum -y install gnuplot >/dev/null 

# install gplotpl if necessary:

GPLOT=gplot-1.11/gplot.pl; export GPLOT
GPLOTDIST=https://pilotfiber.dl.sourceforge.net/project/gplot/gplot/gplot-1.11.tar.gz; export GPLOTDIST
(cd /tmp; curl -L --insecure ${GPLOTDIST} 2>/dev/null | tar xzf - ${GPLOT} >/dev/null)


# Bombard the application with some GET requests. 
# We'll stress-test the dataverse and dataset pages. The latter is more memory- and 
# cpu-intensive, so we'll be sending the requests at different rates, 
# with different numbers of total requests: 20,000 for the dataverse page, 
# and 1,000 for the dataset page. For the dataset page we'll also add 
# some sleep interval between the calls. 
# (we are using a very modest system configuration for this test; the heap 
# size is set to 2GB only). 

for page in dataverse dataset
do
    echo "Beginning the load test of the ${page} page."
    echo "You can monitor the progress of the test by checking the output at ${EC2_HTTP_LOCATION}/memory-benchmark-raw-${page}.txt"

    date > ${DATAVERSE_APP_DIR}/memory-benchmark-raw-${page}.txt

    if [ $page = "dataverse" ]
    then
	repeat_outer=200
	repeat_inner=100
    elif [ $page = "dataset" ]
    then
	repeat_outer=100
	repeat_inner=10

	# we'll use Gary King's dataset for the page test;
	# let's find its id:
	
	dataset_id=`curl http://localhost:8080/api/dataverses/king/contents 2>/dev/null | jq '.data[0].id'`

	if [[ ${dataset_id}"x" = "x" || ${dataset_id} = "null" ]]
	then
	    echo "Failed to obtain the id of Gary King's dataset! Did it fail to import?"
	    exit 1
	fi
    fi

    echo "The basic test of ${repeat_inner} page loads, followed by a snapshot of the "
    echo "heap space and the GC status, will be repeated ${repeat_outer} times total"
    echo "(This means that once the test completes, the output file above will contain the ${repeat_outer} entries total;"
    echo "so looking at the timestamps in the output should give you an idea how long it's going to take to finish)"
    echo 

    for ((i = 0; i < ${repeat_outer}; i++))
    do
	for ((j = 0; j < ${repeat_inner}; j++))
	do
	    # hide the output, standard and stderr:
	    if [ $page = "dataverse" ]
	    then
		curl http://localhost:8080/dataverse/root 2>/dev/null > /dev/null
	    elif [ $page = "dataset" ]
	    then
		curl "http://localhost:8080/dataset.xhtml?id=${dataset_id}" 2>/dev/null > /dev/null
		sleep 2
	    fi	
	done

        # after every {repeat_outer} number of GETs, run jmap and save the output in a temp file:

	sudo -u dataverse jmap -histo ${APP_SERVER_PID} > /tmp/jmap.histo.out
	
        # select the dataverse classes from the histo output: 
	#grep '  edu\.harvard\.iq\.dataverse' /tmp/jmap.histo.out
	# (this will be a lot of output! - up to 50KB per iteration)

        # (alternatively, just select a few more interesting objects:)
	grep '  edu\.harvard\.iq\.dataverse\.DataverseSession$' /tmp/jmap.histo.out
	grep '  edu\.harvard\.iq\.dataverse\.Dataverse$' /tmp/jmap.histo.out
	grep '  edu\.harvard\.iq\.dataverse\.Dataset$' /tmp/jmap.histo.out
	grep '  edu\.harvard\.iq\.dataverse\.DataFile$' /tmp/jmap.histo.out
	grep '  edu\.harvard\.iq\.dataverse\.FileMetadata$' /tmp/jmap.histo.out


	echo
	
        # plus the totals:
	tail -1 /tmp/jmap.histo.out

	echo 

        # run jstat to check on GC: 
	sudo -u dataverse jstat -gcutil ${APP_SERVER_PID} 1000 1 2>/dev/null

	echo 

	date

	echo 
    done >> $DATAVERSE_APP_DIR/memory-benchmark-raw-${page}.txt

    echo "Done!"

    # Create simple plots of page GETs vs. memory utilisation: 

    if [ $page = "dataverse" ] 
    then
	multiplier="0.1"
	xlabel="page GETs x1000"
    elif [ $page = "dataset" ] 
    then
	multiplier="10";
	xlabel="page GETs"
    fi

    grep '^  *[0-9][0-9]*\.[0-9]' ${DATAVERSE_APP_DIR}/memory-benchmark-raw-${page}.txt | awk '{print (NR*'${multiplier}'),$4}' > /tmp/${page}.xhtml

    echo "Generated the data file for plotting the graph of page GETs vs. oldgen memory utilization."
    echo "It is available for download here: ${EC2_HTTP_LOCATION}/plot-${page}.dat"

    if [ -f /tmp/${GPLOT} ]
    then
	/tmp/${GPLOT} -type png -outfile ${DATAVERSE_APP_DIR}/benchmark-${page}.png -xlabel "${xlabel}" -ylabel 'oldgen (%, 2GB total)' -title 'Memory Utilisation, default GC' /tmp/${page}.xhtml
	echo "Generated the plot for the ${page} page..."
	echo "Available here: ${EC2_HTTP_LOCATION}/benchmark-${page}.png"
    else
	echo "(Skipping generating a plot, since gplotpl is not installed;"
	echo "You can still download the raw data, and generate plots locally)"
    fi

    /bin/mv /tmp/${page}.xhtml ${DATAVERSE_APP_DIR}/plot-${page}.dat

    echo
done


