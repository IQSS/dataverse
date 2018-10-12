#!/usr/bin/env bash

# run through all the steps to setup docker-aio to run integration tests

# hard-codes several assumptions: image is named dv0, container is named dv, port is 8084

# setupIT.bash retries
n_retries=10

# glassfish healthy/ready retries
n_wait=5

cd conf/docker-aio
./0prep_deps.sh
./1prep.sh
docker build -t dv0 -f c7.dockerfile .
i_retry=0
setup_ok=0
while [ $i_retry -lt $n_retries ]
do
	# cleanup from previous runs if necessary
	docker rm -f dv
	# start container
	docker run -d -p 8084:80 -p 8083:8080 -p 9010:9009 --name dv dv0

	# wait for glassfish to be healthy
	i_wait=0
	d_wait=10
	while [ $i_wait -lt $n_wait ]
	do
		h=`docker inspect -f "{{.State.Health.Status}}" dv`
		if [ "healthy" == "${h}" ]; then
			break
		else
			sleep $d_wait
		fi
		i_wait=$(( $i_wait + 1 ))
		
	done

	# try setupIT.bash
	docker exec -it dv /opt/dv/setupIT.bash
	err=$?
	if [ $err -eq 0 ]
	then
		setup_ok=1
		echo "setupIT worked ($i_retry tries)"
		break # success!
	fi
	echo "$i_retry try in loop"
	i_retry=$(( $i_retry + 1 ))
done
if [ $setup_ok -ne 1 ]
then
	echo "failed setupIT after $i_retry tries ; bailing out"
	exit 1
fi

# handle config for the private url test
./seturl.bash
err=$?
if [ $err -ne 0 ]; then
	echo "seturl fail; bailing out"
	exit 1
fi
# configure DOI provider based on docker build arguments / environmental variables
docker exec -it dv /opt/dv/configure_doi.bash


cd ../..
echo "docker-aio ready to run integration tests ($i_retry)"
curl http://localhost:8084/api/info/version
echo $?

