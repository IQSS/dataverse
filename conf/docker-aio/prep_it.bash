#!/usr/bin/env bash

# run through all the steps to setup docker-aio to run integration tests

# hard-codes several assumptions: image is named dv0, container is named dv, port is 8084

# glassfish healthy/ready retries
n_wait=5

cd conf/docker-aio
./0prep_deps.sh
./1prep.sh
docker build -t dv0 -f c8.dockerfile .
# cleanup from previous runs if necessary
docker rm -f dv
# start container
docker run -d -p 8084:80 -p 8083:8080 -p 9010:9009 -p 8983:8983 --name dv dv0
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
echo -n "Waiting for Dataverse ready "
while [ "$(curl -sk -m 1 -I http://localhost:8084/api/info/version | head -n 1 | cut -d' ' -f2)" != "200" ]; do \
		[[ $? -gt 0 ]] && echo -n 'x' || echo -n '.'; sleep 1; done && true
echo	' OK.'
# configure DOI provider based on docker build arguments / environmental variables
docker exec dv /opt/dv/configure_doi.bash
err=$?
if [ $err -ne 0 ]; then
	echo "error - DOI configuration failure"
	exit 1
fi

cd ../..
#echo "docker-aio ready to run integration tests ($i_retry)"
echo "docker-aio ready to run integration tests"
curl http://localhost:8084/api/info/version
echo $?

cp conf/docker-aio/microprofile-config.properties2 src/main/resources/META-INF/microprofile-config.properties
docker system prune -a -f
