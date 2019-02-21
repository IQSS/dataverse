This docker-compose setup is intended for use in development, small scale evaluation, and potentially serve as an example of a working (although not production security level) configuration.

Setup:

- build docker-aio image with name dv0 as described in ../docker-aio` (don't start up the docker image or run setupIT.bash)
- work in the `conf/docker-dcm` directory for below commands
- download/prepare dependencies: `./0prep.sh`
- build dcm/dv0dcm images with docker-compose: `docker-compose -f docker-compose.yml build`
- start containers: `docker-compose -f docker-compose.yml up -d`
- wait for container to show "healthy" (aka - `docker ps`), then wait another 4-5 minutes (even though it shows healthy, glassfish is still standing itself up), then run dataverse app installation: `docker exec -it dvsrv /opt/dv/install.bash`
- configure dataverse application to use DCM: `docker exec -it dvsrv /opt/dv/configure_dcm.sh`

Operation:
The dataverse installation is accessible at `http://localhost:8084`.
The `dcm_client` container is intended to be used for executing transfer scripts, and `conf/docker-dcm` is available at `/mnt` inside the container; this container can be accessed with `docker exec -it dcm_client bash`.
The DCM cron job is NOT configured here; for development purposes the DCM checks can be run manually with `docker exec -it dcmsrv /opt/dcm/scn/post_upload.bash`.


Cleanup:
- shutdown/cleanup `docker-compose -f docker-compose.yml down -v`

For reference, this configuration was working with docker 17.09 / docker-compose 1.16.

