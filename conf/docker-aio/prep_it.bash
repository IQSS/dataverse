#!/usr/bin/env bash

# run through all the steps to setup docker-aio to run integration tests

# hard-codes several assumptions: image is named dv0, container is named dv, port is 8084

# glassfish healthy/ready retries
n_wait=5

cd conf/docker-aio
./0prep_deps.sh
./1prep.sh
docker build -t dv0 -f c7.dockerfile .
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
docker exec dv /opt/dv/setupIT.bash
err=$?
if [ $err -ne 0 ]; then
	echo "error - setupIT failure"
	exit 1
fi
# configure DOI provider based on docker build arguments / environmental variables
docker exec dv /opt/dv/configure_doi.bash
err=$?
if [ $err -ne 0 ]; then
	echo "error - DOI configuration failure"
	exit 1
fi
# handle config for the private url test (and things like publishing...)
./seturl.bash


cd ../..
#echo "docker-aio ready to run integration tests ($i_retry)"
echo "docker-aio ready to run integration tests"
curl http://localhost:8084/api/info/version
echo $?

