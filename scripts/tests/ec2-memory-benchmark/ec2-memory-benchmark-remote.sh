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


# install gnuplot, quietly: 

yum -y install gnuplot >/dev/null 

# install gplotpl if necessary:

GPLOT=gplot-1.11/gplot.pl; export GPLOT
GPLOTDIST=https://pilotfiber.dl.sourceforge.net/project/gplot/gplot/gplot-1.11.tar.gz; export GPLOTDIST
(cd /tmp; curl ${GPLOTDIST} 2>/dev/null | tar xvzf - ${GPLOT})


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
	
	dataset_id=`curl http://localhost:8080/api/dataverses/king/contents | jq '.data[0].id'`

	if [[ ${dataset_id}"x" = "x" || ${dataset_id} = "null" ]]
	then
	    echo "Failed to obtain the id of Gary King's dataset! Did it fail to import?"
	    exit 1
	fi
    fi

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

	sudo -u glassfish jmap -histo ${GLASSFISH_PID} > /tmp/jmap.histo.out
	
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
	sudo -u glassfish jstat -gcutil ${GLASSFISH_PID} 1000 1 2>/dev/null

	echo 

	date

	echo 
    done >> $DATAVERSE_APP_DIR/memory-benchmark-raw-${page}.txt

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

    if [ -f /tmp/${GPLOT} ]
    then
	/tmp/${GPLOT} -type png -outfile ${DATAVERSE_APP_DIR}/benchmark-${page}.png -xlabel "${xlabel}" -ylabel 'oldgen (%, 2GB total)' -title 'Memory Utilisation, default GC' /tmp/${page}.xhtml
    else
	echo "Skipping generating a plot, since gplotpl is not installed"
	echo "You can still download the raw data, and generate plots locally"
    fi

    /bin/mv /tmp/${page}.xhtml ${DATAVERSE_APP_DIR}/plot-${page}.dat
done


