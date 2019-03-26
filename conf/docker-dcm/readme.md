This docker-compose setup is intended for use in development, small scale evaluation, and potentially serve as an example of a working (although not production security level) configuration.

Setup:

- build docker-aio image with name dv0 as described in `../docker-aio` (don't start up the docker image or run setupIT.bash)
- work in the `conf/docker-dcm` directory for below commands
- download/prepare dependencies: `./0prep.sh`
- build dcm/dv0dcm images with docker-compose: `docker-compose -f docker-compose.yml build`
- start containers: `docker-compose -f docker-compose.yml up -d`
- wait for container to show "healthy" (aka - `docker ps`), then run dataverse app installation: `docker exec -it dvsrv /opt/dv/install.bash` (until https://github.com/IQSS/dataverse/issues/5374 is sorted, it may be preferable to connect to the container and run configuration scripts inside - e.g. `docker exec -it dvsrv bash ; ./install.bash`)
- for development, you probably want to use the `FAKE` DOI provider: `docker exec -it dvsrv /opt/dv/configure_doi.bash`
- configure dataverse application to use DCM: `docker exec -it dvsrv /opt/dv/configure_dcm.sh`
- configure dataverse application to use RSAL (if desired): `docker exec -it dvsrv /opt/dv/configure_rsal.sh`

Operation:
The dataverse installation is accessible at `http://localhost:8084`.
The `dcm_client` container is intended to be used for executing transfer scripts, and `conf/docker-dcm` is available at `/mnt` inside the container; this container can be accessed with `docker exec -it dcm_client bash`.
The DCM cron job is NOT configured here; for development purposes the DCM checks can be run manually with `docker exec -it dcmsrv /opt/dcm/scn/post_upload.bash`.
The RSAL cron job is similarly NOT configured; for development purposes `docker exec -it rsalsrv /opt/rsal/scn/pub.py` can be run manually.


Cleanup:
- shutdown/cleanup `docker-compose -f docker-compose.yml down -v`

For reference, this configuration was working with docker 17.09 / docker-compose 1.16.

