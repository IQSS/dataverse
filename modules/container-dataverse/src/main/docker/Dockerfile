# this is built off the base container
FROM gdcc/base:unstable as builder

ENV DEBIAN_FRONTEND noninteractive

USER root

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-11-jdk maven ca-certificates && \
    apt-get -y upgrade && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /dataverse

# copy over sourcecode and build files needed to compile the .war
# as well as installer files
COPY pom.xml /dataverse/
COPY modules /dataverse/modules/
COPY local_lib /dataverse/local_lib/

# cache pom files and packages for compilation later
RUN cd /dataverse && mvn verify clean --fail-never

COPY src /dataverse/src/
COPY scripts /dataverse/scripts/
COPY conf/jhove/ /dataverse/conf/jhove/

WORKDIR /dataverse

# this can take some time to download all the dependencies
RUN export dpkgArch="$(dpkg --print-architecture)" && \
  export JAVA_HOME="/usr/lib/jvm/java-11-openjdk-${dpkgArch}" && \
  mvn package -DskipTests --no-transfer-progress -T 1C

FROM gdcc/base:unstable

ENV DEBIAN_FRONTEND noninteractive

USER root

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    python3-dev tzdata nano curl wget unzip jq imagemagick python3 python3-pip python3-psycopg2 wait-for-it ca-certificates && \
    apt-get -y upgrade && \
    rm -rf /var/lib/apt/lists/*

RUN useradd --create-home --shell /bin/bash dataverse

# install awscli
RUN pip3 install --no-cache-dir awscli

# ENV JAVA_HOME "/usr/lib/jvm/java-11-openjdk-${ARCHITECTURE}"
# RUN export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))"

# install Counter Processor
# https://guides.dataverse.org/en/latest/installation/prerequisites.html#counter-processor
# RUN cd /usr/local && \
#   wget https://github.com/CDLUC3/counter-processor/archive/refs/tags/v0.1.04.tar.gz && \
#   tar xvfz v0.1.04.tar.gz && \
#   rm v0.1.04.tar.gz && \
#   cd counter-processor-0.1.04 && \
#   pip3 install -r requirements.txt
# RUN useradd --create-home --shell /bin/bash counter && \
#   chown -R counter:counter /usr/local/counter-processor-0.1.04

WORKDIR /

COPY --chown=dataverse:dataverse --from=builder /dataverse /dataverse

# switch to non-root user as this is more secure
USER dataverse
WORKDIR /

RUN mkdir -p /home/dataverse/.aws/
COPY --chown=dataverse:dataverse ./config /home/dataverse/.aws/config
COPY --chown=dataverse:dataverse ./credentials /home/dataverse/.aws/credentials

# add Payara to PATH
ENV PATH="${PATH}:${PAYARA_DIR}/bin"

USER root
RUN cp -R /home/dataverse/.aws/ "${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}"

# set ownership so later the dataverse user can write to and make changes
RUN chown -R dataverse "${PAYARA_DIR}"

COPY --chown=dataverse:dataverse ./preboot.sh /preboot.sh
RUN chmod +x /preboot.sh
COPY --chown=dataverse:dataverse ./postboot.sh /postboot.sh
RUN chmod +x /postboot.sh

# auto-deploy the .war file
ENV DEPLOY_DIR /dataverse/target/

# pre and post boot commands
ENV PREBOOT_COMMANDS /preboot.sh
ENV POSTBOOT_COMMANDS /postboot.sh

#USER dataverse
#CMD ["wait-for-it", "postgres:5432", "--", "/startup.sh"]

# helpful for debugging purposes to just start up the container
# CMD ["tail", "-f", "/dev/null"]