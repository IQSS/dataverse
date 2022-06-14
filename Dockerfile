# https://hub.docker.com/_/ubuntu
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc python3-dev tzdata nano dos2unix curl wget openjdk-11-jdk maven unzip jq imagemagick python3 python3-pip python3-psycopg2 wait-for-it ca-certificates && \
    apt-get -y upgrade && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /

RUN useradd --create-home --shell /bin/bash dataverse

# https://guides.dataverse.org/en/5.8/installation/prerequisites.html
RUN wget https://s3-eu-west-1.amazonaws.com/payara.fish/Payara+Downloads/5.2022.1/payara-5.2022.1.zip

RUN unzip payara-5.2022.1.zip && \
  mv payara5 /usr/local && \
  rm payara-5.2022.1.zip

RUN chown -R root:root /usr/local/payara5 && \
  chown dataverse /usr/local/payara5/glassfish/lib && \
  chown -R dataverse:dataverse /usr/local/payara5/glassfish/domains/domain1

# ENV JAVA_HOME "/usr/lib/jvm/java-11-openjdk-${ARCHITECTURE}"
# RUN export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))"

# install Counter Processor
# https://guides.dataverse.org/en/latest/installation/prerequisites.html#counter-processor
RUN cd /usr/local && \
  wget https://github.com/CDLUC3/counter-processor/archive/refs/tags/v0.1.04.tar.gz && \
  tar xvfz v0.1.04.tar.gz && \
  rm v0.1.04.tar.gz && \
  cd counter-processor-0.1.04 && \
  pip3 install -r requirements.txt

RUN useradd --create-home --shell /bin/bash counter && \
  chown -R counter:counter /usr/local/counter-processor-0.1.04

# install awscli
RUN pip3 install --no-cache-dir awscli

# switch to non-root user as this is more secure
USER dataverse
WORKDIR /

RUN mkdir -p /home/dataverse/.aws/
COPY --chown=dataverse:dataverse ./conf/docker-compose/dataverse/config /home/dataverse/.aws/config
COPY --chown=dataverse:dataverse ./conf/docker-compose/dataverse/credentials /home/dataverse/.aws/credentials

RUN cp -R /home/dataverse/.aws/ /usr/local/payara5/glassfish/domains/domain1/

# if you want to speed up the Maven build you can copy over
# cached packages here
# COPY --chown=dataverse:dataverse ./conf/docker-compose/dataverse/.m2/ /home/dataverse/.m2/

# copy over sourcecode and build files needed to compile the .war
# as well as installer files
COPY --chown=dataverse:dataverse pom.xml /dataverse/
COPY --chown=dataverse:dataverse src /dataverse/src/
COPY --chown=dataverse:dataverse modules /dataverse/modules/
COPY --chown=dataverse:dataverse scripts /dataverse/scripts/
COPY --chown=dataverse:dataverse conf/jhove/ /dataverse/conf/jhove/
COPY --chown=dataverse:dataverse local_lib /dataverse/local_lib/

# this likely isn't needed on Linux but was needed on a Windows build
RUN find /dataverse -type f -print0 | xargs -0 -n 1 -P 4 dos2unix

# this can take some time to download all the dependencies
RUN cd /dataverse/ && \
  export dpkgArch="$(dpkg --print-architecture)" && \
  export JAVA_HOME="/usr/lib/jvm/java-11-openjdk-${dpkgArch}" && \
  mvn package -DskipTests --no-transfer-progress

USER root
COPY --chown=dataverse:dataverse ./conf/docker-compose/dataverse/startup.sh /startup.sh
RUN chmod +x /startup.sh && dos2unix /startup.sh

USER dataverse
CMD ["wait-for-it", "postgres:5432", "--", "/startup.sh"]

# helpful for debugging purposes to just start up the container
# CMD ["tail", "-f", "/dev/null"]