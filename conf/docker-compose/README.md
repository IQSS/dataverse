# docker-compose version of Dataverse

## Requirements

* [docker-compose](https://docs.docker.com/compose/)
* [Docker](https://docker.com) (or some other supported container engine)

## Setup

Edit the `.env` file as needed.  Make sure to properly secure it in terms of file permissions.

Edit `./seaweedfs/config.json` and enter your [credential keys](https://github.com/chrislusf/seaweedfs/wiki/Amazon-S3-API#static-configuration) for s3 storage.

If you're running locally and don't have a key, you'll
need to generate it yourself with something like Git Bash.  Make sure
[Posix to Windows path conversion](https://github.com/git-for-windows/git/issues/577#issuecomment-166118846) doesn't
take place with the forward slashes using `MSYS_NO_PATHCONV=1` if you're on Windows.

```shell
MSYS_NO_PATHCONV=1 openssl req -x509 -nodes -days 4096 -newkey rsa:4096 -out traefik.crt -keyout traefik.key -subj "/C=US/ST=New Mexico/L=ABQ/O=Local/CN=127.0.0.1" -addext "subjectAltName = IP:127.0.0.1"
```

Or grab your public/private keys from your sysadmin or provider and renamed them to `traefik.key` and `traefik.crt`.

Then copy the `traefik.key` and `traefik.crt` files into the `traefik` folder.

## Building

Run `prepbuild.sh` once

Pull and build the Docker containers

```shell
# this uses Compose v2, if you're on an older version you may
# need to change this call to docker-compose
docker compose pull
docker compose build
```

## Deploying

```shell
docker-compose up -d
```

Note that this can take a couple minutes to start up.  Wait until it shows `healthy` as the status.

For the bind mounts (see `docker-compose.yml`) you may need to set the permissions
on those folders `*-bind` so they can be written from within the containers.  Alternatively,
you can create local users or do UID/GID mappings.

```shell
docker ps
```

Then go to the following URL in your browser:

[https://localhost](https://localhost)

Default credentials for login are:

* username: `dataverseAdmin`
* password: `admin`

Make sure to change this password right away.

## How It Works

* Builds a copy of the `.war` deployable code from source
* Stands up various services and pieces needed:
  * seaweedfs - for s3 storage
  * traefik - reverse proxy, HTTP is re-routed automatically to HTTPS
  * postgres - database backend
  * solr - text indexing database
  * rserve - R server for running R commands
  * dataverse - the main Dataverse web application
* sets up two storage options, one is the default `<id>=files` for local storage
and the other is `<id>=s3`for s3 storage

## Uninstall / Teardown

```shell
docker-compose down -v
```

## Development References

There are many community led efforts to utilize containers, Kubernetes, and more to help automate
and setup Dataverse.

* [https://github.com/fzappa/rocky-dataverse/blob/main/rocky-dataverse.sh](https://github.com/fzappa/rocky-dataverse/blob/main/rocky-dataverse.sh)
* [https://github.com/IQSS/dataverse/tree/develop/conf/docker-aio](https://github.com/IQSS/dataverse/tree/develop/conf/docker-aio)
* [https://github.com/gdcc/dataverse-kubernetes/blob/develop/docker-compose.yaml](https://github.com/gdcc/dataverse-kubernetes/blob/develop/docker-compose.yaml)
* [https://github.com/gdcc/dataverse-kubernetes](https://github.com/gdcc/dataverse-kubernetes)
* [https://github.com/EOSC-synergy/dataverse-kubernetes](https://github.com/EOSC-synergy/dataverse-kubernetes)
* [https://github.com/IQSS/dataverse-docker](https://github.com/IQSS/dataverse-docker)
