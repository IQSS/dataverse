# https://hub.docker.com/_/ubuntu
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    tzdata nano r-base r-base-dev libssl-dev libnlopt-dev libcurl4-openssl-dev wait-for-it ca-certificates && \
    apt-get -y upgrade && \
    rm -rf /var/lib/apt/lists/*

RUN R -e "install.packages(c('R2HTML', 'rjson', 'DescTools', 'Rserve', 'haven'))"

# this is the user and password that Dataverse will connect to the Rserve server
RUN echo "rserve rserve" > /rserve.pwd

WORKDIR /

EXPOSE 6311/tcp

# https://github.com/ubc/r-docker/blob/master/Dockerfile
CMD ["R", "-e", "Rserve::run.Rserve(remote=TRUE, auth=TRUE, pwdfile='/rserve.pwd', daemon=FALSE, port=6311, fileio=TRUE, maxinbuf=262144)"]