# Copyright 2019 Forschungszentrum Jülich GmbH
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
#
################################################################################################################
#
# THIS FILE IS TO BE USED WITH MAVEN DOCKER BUILD:
# mvn -Pct clean package docker:build
#
################################################################################################################
#
# Some commands used are inspired by https://github.com/payara/Payara/tree/master/appserver/extras/docker-images.
# Most parts origin from older versions of https://github.com/gdcc/dataverse-kubernetes.
#
# We are not using upstream Payara images because:
#  - Using same base image as Solr (https://hub.docker.com/_/solr) is reducing pulls
#  - Their image is less optimised for production usage by design choices
#
FROM openjdk:11-jre
LABEL maintainer="FDM FZJ <forschungsdaten@fz-juelich.de>"
# Default payara ports to expose
# 4848: admin console
# 9009: debug port (JPDA)
# 8080: http
# 8181: https
EXPOSE 4848 9009 8080 8181

ENV HOME_DIR="/opt/payara"
ENV PAYARA_DIR="${HOME_DIR}/appserver" \
    SCRIPT_DIR="${HOME_DIR}/scripts" \
    CONFIG_DIR="${HOME_DIR}/config" \
    DEPLOY_DIR="${HOME_DIR}/deployments" \
    DATA_DIR="/data" \
    DOCROOT_DIR="/docroot" \
    METADATA_DIR="/metadata" \
    SECRETS_DIR="/secrets" \
    DUMPS_DIR="/dumps" \
    PASSWORD_FILE="${HOME_DIR}/passwordFile" \
    ADMIN_USER="admin" \
    ADMIN_PASSWORD="admin" \
    DOMAIN_NAME="domain1" \
    PAYARA_ARGS=""
ENV PATH="${PATH}:${PAYARA_DIR}/bin" \
    DOMAIN_DIR="${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}" \
    JVM_ARGS="" \
    MEM_MAX_RAM_PERCENTAGE="70.0" \
    MEM_XSS="512k" \
    PREBOOT_COMMANDS="${CONFIG_DIR}/pre-boot-commands.asadmin" \
    POSTBOOT_COMMANDS="${CONFIG_DIR}/post-boot-commands.asadmin" \
    DEPLOY_PROPS="" \
    # Make heap dumps on OOM appear in DUMPS_DIR
    ENABLE_DUMPS=0 \
    JVM_DUMPS_ARGS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=\${ENV=DUMPS_DIR}" \
    # Documenting development options (see init_3_enabledev.sh)
    ENABLE_JMX=0 \
    ENABLE_JDWP=0 \
    ENABLE_JREBEL=0 \
    JREBEL_LIB="${HOME_DIR}/jrebel/lib/libjrebel64.so" \
    DATAVERSE_VERSION="@project.version@"

ARG PAYARA_VERSION="@payara.version@"
ARG ESH_VERSION=0.3.1
ARG ESH_CHECKSUM="1e0bd783f930cba13d6708b11c1ac844bbb1eddd02ac1666fc10d47eb9517bd7"
ARG JATTACH_VERSION="v1.5"
ARG JATTACH_CHECKSUM="adab16e1081aa4fafc91f3f2e44262338a498d8f853d3c6801050889e6389548"
ARG PKGS="jq imagemagick curl unzip wget acl dirmngr gpg lsof procps netcat tini"
ARG ASADMIN="${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE}"

### PART 1: SYSTEM ###
USER root
WORKDIR /
RUN true && \
    # Create pathes
    mkdir -p ${HOME_DIR} ${PAYARA_DIR} ${DEPLOY_DIR} ${CONFIG_DIR} ${SCRIPT_DIR} && \
    mkdir -p ${DATA_DIR} ${METADATA_DIR} ${DOCROOT_DIR} ${SECRETS_DIR} ${DUMPS_DIR} && \
    # Create user
    addgroup --gid 1000 payara && \
    adduser --system --uid 1000 --no-create-home --shell /bin/bash --home "${HOME_DIR}" --gecos "" --ingroup payara payara && \
    echo payara:payara | chpasswd && \
    # Set permissions
    chown -R payara: ${HOME_DIR} && \
    chown -R payara: ${DATA_DIR} ${METADATA_DIR} ${DOCROOT_DIR} ${SECRETS_DIR} ${DUMPS_DIR}

RUN true && \
    # Install packages
    apt-get update -q && \
    apt-get install -qqy ${PKGS} && \

    # Download & check esh template script
    curl -sSfL -o /usr/bin/esh https://raw.githubusercontent.com/jirutka/esh/v${ESH_VERSION}/esh && \
    echo "${ESH_CHECKSUM} /usr/bin/esh" | sha256sum -c - && \
    chmod +x /usr/bin/esh && \

    # Install jattach
    curl -sSfL -o /usr/bin/jattach https://github.com/apangin/jattach/releases/download/${JATTACH_VERSION}/jattach && \
    echo "${JATTACH_CHECKSUM} /usr/bin/jattach" | sha256sum -c - && \
    chmod +x /usr/bin/jattach && \

    # Download & unzip JRebel to $JREBEL_LIB = ${HOME_DIR}/jrebel/lib/libjrebel64.so (for development use)
    curl -sS -f -o ${HOME_DIR}/jrebel.zip http://dl.zeroturnaround.com/jrebel-stable-nosetup.zip && \
    unzip -q "${HOME_DIR}/jrebel.zip" -d "${HOME_DIR}" && \

    # Cleanup
    rm -rf /var/lib/apt/lists/* "${HOME_DIR}/jrebel.zip"

### PART 2: PAYARA ###
# After setting up system, now configure Payara
USER payara
WORKDIR ${HOME_DIR}

# Download, check and install Payara
RUN curl -sSfL -o payara.zip "https://repo1.maven.org/maven2/fish/payara/distributions/payara/${PAYARA_VERSION}/payara-${PAYARA_VERSION}.zip" && \
    curl -sSfL -o payara.zip.sha256 "https://repo1.maven.org/maven2/fish/payara/distributions/payara/${PAYARA_VERSION}/payara-${PAYARA_VERSION}.zip.sha256" && \
    echo "$(cat payara.zip.sha256) payara.zip" | sha256sum -c - && \
    unzip -q payara.zip -d ${HOME_DIR} && \
    mv ${HOME_DIR}/payara5/* ${PAYARA_DIR}/ && \
    rm -rf ${HOME_DIR}/payara5 payara.zip*

# Copy the system (appserver level) scripts like entrypoint, etc
COPY --chown=payara:payara maven/scripts/system ${SCRIPT_DIR}/

# TODO: refactor and make production ready
# Configure the domain to be container and production ready
RUN true && \
    # Set admin password
    echo "AS_ADMIN_PASSWORD=\nAS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/password-change-file.txt && \
    echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" >> ${PASSWORD_FILE}  && \
    asadmin --user=${ADMIN_USER} --passwordfile=/tmp/password-change-file.txt change-admin-password --domain_name=${DOMAIN_NAME} && \
    # Start domain for configuration
    ${ASADMIN} start-domain ${DOMAIN_NAME} && \
    # Allow access to admin with password only
    ${ASADMIN} enable-secure-admin && \

    ### CONTAINER USAGE ENABLEMENT
    # List & delete memory settings from domain
    for MEMORY_JVM_OPTION in $(${ASADMIN} list-jvm-options | grep "Xm[sx]\|Xss\|NewRatio"); \
       do \
         ${ASADMIN} delete-jvm-options $(echo $MEMORY_JVM_OPTION | sed -e 's/:/\\:/g'); \
       done && \
    ${ASADMIN} create-jvm-options '-XX\:+UseContainerSupport:-XX\:MaxRAMPercentage=${ENV=MEM_MAX_RAM_PERCENTAGE}:-Xss${ENV=MEM_XSS}' && \
    # Set logging to console only
    ${ASADMIN} set-log-attributes com.sun.enterprise.server.logging.GFFileHandler.logtoFile=false && \

    ### PRODUCTION READINESS
    ${ASADMIN} create-jvm-options '-XX\:+UseG1GC:-XX\:+UseStringDeduplication:-XX\:MaxGCPauseMillis=500' && \
    ${ASADMIN} create-jvm-options '-XX\:MetaspaceSize=256m:-XX\:MaxMetaspaceSize=2g:-XX\:+IgnoreUnrecognizedVMOptions' && \
    # Enlarge thread pools
    ${ASADMIN} set server-config.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size="50" && \
    ${ASADMIN} set server-config.thread-pools.thread-pool.http-thread-pool.max-queue-size="" && \
    ${ASADMIN} set default-config.thread-pools.thread-pool.thread-pool-1.max-thread-pool-size="250" && \
    # Enable file caching
    ${ASADMIN} set server-config.network-config.protocols.protocol.http-listener-1.http.file-cache.enabled="true" && \
    ${ASADMIN} set server-config.network-config.protocols.protocol.http-listener-2.http.file-cache.enabled="true" && \
    ${ASADMIN} set default-config.network-config.protocols.protocol.http-listener-1.http.file-cache.enabled="true" && \
    ${ASADMIN} set default-config.network-config.protocols.protocol.http-listener-2.http.file-cache.enabled="true" && \
    # Enlarge EJB pools (cannot do this for server-config as set does not create new entries)
    ${ASADMIN} set default-config.ejb-container.max-pool-size="128" && \
    # Misc settings
    ${ASADMIN} create-system-properties fish.payara.classloading.delegate="false" && \
    ${ASADMIN} create-system-properties jersey.config.client.readTimeout="300000" && \
    ${ASADMIN} create-system-properties jersey.config.client.connectTimeout="300000" && \

    ### DATAVERSE APPLICATION SPECIFICS
    ${ASADMIN} set-config-dir --directory=${SECRETS_DIR} && \
    # TODO: what of the below 3 items can be deleted for container usage?
    ${ASADMIN} create-network-listener --protocol=http-listener-1 --listenerport=8009 --jkenabled=true jk-connector && \
    ${ASADMIN} set server-config.network-config.protocols.protocol.http-listener-1.http.comet-support-enabled=true && \
    ${ASADMIN} create-system-properties javax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl && \
    # Always disable phoning home...
    ${ASADMIN} disable-phone-home && \

    ### CLEANUP
    # Stop domain
    ${ASADMIN} stop-domain ${DOMAIN_NAME} && \
    # Delete generated files
    rm -rf \
        /tmp/password-change-file.txt \
        ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/osgi-cache \
        ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/logs

# Make docroot of Payara reside in higher level directory for easier targeting
# Due to gdcc/dataverse-kubernetes#177: create the generated pathes so they are
# writeable by us. TBR with gdcc/dataverse-kubernetes#178.
RUN rm -rf ${DOMAIN_DIR}/docroot && \
    ln -s ${DOCROOT_DIR} ${DOMAIN_DIR}/docroot && \
    mkdir -p ${DOMAIN_DIR}/generated/jsp/dataverse

### PART 3: DATAVERSE INSTALLATION ###
# Copy app and deps from assembly in proper layers
COPY --chown=payara:payara maven/deps ${DEPLOY_DIR}/dataverse/WEB-INF/lib/
COPY --chown=payara:payara maven/app ${DEPLOY_DIR}/dataverse/
COPY --chown=payara:payara maven/supplements ${DEPLOY_DIR}/dataverse/supplements/
# TEMPORARY WORKAROUND FOR DATAVERSE VERSION SUPPORT
# http://github.com/IQSS/dataverse/blob/5dc5db197ab1c0c50cc7f932b45875f5699518e8/src/main/java/edu/harvard/iq/dataverse/util/SystemConfig.java#L176-L176
COPY --chown=payara:payara maven/maven-archiver ${DEPLOY_DIR}/maven-archiver/

# Create symlinks for jHove
RUN ln -s ${DEPLOY_DIR}/dataverse/supplements/jhove.conf ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhove.conf && \
    ln -s ${DEPLOY_DIR}/dataverse/supplements/jhoveConfig.xsd ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhoveConfig.xsd && \
    sed -i ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhove.conf -e "s:/usr/local/payara5/glassfish/domains/domain1:${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}:g"

# Copy init and application scripts
COPY --chown=payara:payara maven/scripts/app ${SCRIPT_DIR}/
RUN chmod +x ${SCRIPT_DIR}/*

# Set the entrypoint
ENTRYPOINT ["/usr/bin/tini", "--"]
CMD "${SCRIPT_DIR}/entrypoint.sh"