# Copyright 2022 Forschungszentrum Jülich GmbH
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
#  - Their image is less optimised for production usage and Dataverse by design choices
#  - We provide multi-arch images
#  - We provide some tweaks for development and monitoring
#

# Make the Java base image and version configurable (useful for trying newer Java versions and flavors)
ARG JAVA_IMAGE="eclipse-temurin:11-jre"
FROM $JAVA_IMAGE

# Default payara ports to expose
# 4848: admin console
# 9009: debug port (JDWP)
# 8080: http
# 8181: https - but http-listener-2 is disabled here!
# 8686: JMX
EXPOSE 4848 9009 8080 8686

ENV HOME_DIR="/opt/payara"
ENV PAYARA_DIR="${HOME_DIR}/appserver" \
    SCRIPT_DIR="${HOME_DIR}/scripts" \
    CONFIG_DIR="${HOME_DIR}/config" \
    DEPLOY_DIR="${HOME_DIR}/deployments" \
    STORAGE_DIR="/dv" \
    SECRETS_DIR="/secrets" \
    DUMPS_DIR="/dumps" \
    PASSWORD_FILE="${HOME_DIR}/passwordFile" \
    ADMIN_USER="admin" \
    ADMIN_PASSWORD="admin" \
    DOMAIN_NAME="domain1" \
    PAYARA_ARGS=""
ENV PATH="${PATH}:${PAYARA_DIR}/bin:${SCRIPT_DIR}" \
    DOMAIN_DIR="${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}" \
    DEPLOY_PROPS="" \
    PREBOOT_COMMANDS="${CONFIG_DIR}/pre-boot-commands.asadmin" \
    POSTBOOT_COMMANDS="${CONFIG_DIR}/post-boot-commands.asadmin" \
    JVM_ARGS="" \
    MEM_MAX_RAM_PERCENTAGE="70.0" \
    MEM_XSS="512k" \
    # Source: https://github.com/fabric8io-images/run-java-sh/blob/master/TUNING.md#recommandations
    MEM_MIN_HEAP_FREE_RATIO="20" \
    MEM_MAX_HEAP_FREE_RATIO="40" \
    MEM_MAX_GC_PAUSE_MILLIS="500" \
    MEM_METASPACE_SIZE="256m" \
    MEM_MAX_METASPACE_SIZE="2g" \
    # Make heap dumps on OOM appear in DUMPS_DIR
    ENABLE_DUMPS=0 \
    JVM_DUMPS_ARG="-XX:+HeapDumpOnOutOfMemoryError" \
    ENABLE_JMX=0 \
    ENABLE_JDWP=0 \
    ENABLE_RELOAD=0

### PART 1: SYSTEM ###
ARG UID=1000
ARG GID=1000
USER root
WORKDIR /
SHELL ["/bin/bash", "-euo", "pipefail", "-c"]
# Mark these directories as mutuable data containers to avoid cluttering the images overlayfs at runtime.
VOLUME ${STORAGE_DIR} ${SECRETS_DIR} ${DUMPS_DIR}
RUN <<EOF
    # Create pathes
    mkdir -p "${HOME_DIR}" "${PAYARA_DIR}" "${DEPLOY_DIR}" "${CONFIG_DIR}" "${SCRIPT_DIR}"
    mkdir -p "${STORAGE_DIR}" "${SECRETS_DIR}" "${DUMPS_DIR}"
    # Create user
    addgroup --gid ${GID} payara
    adduser --system --uid ${UID} --no-create-home --shell /bin/bash --home "${HOME_DIR}" --gecos "" --ingroup payara payara
    echo payara:payara | chpasswd
    # Set permissions
    chown -R payara: "${HOME_DIR}"
    chown -R payara: "${STORAGE_DIR}" "${SECRETS_DIR}" "${DUMPS_DIR}"
EOF

ARG JATTACH_VERSION="v2.1"
ARG JATTACH_CHECKSUM="07885fdc782e02e7302c6d190f54c3930afa10a38140365adf54076ec1086a8e"
ARG WAIT_FOR_VERSION="v2.2.3"
ARG WAIT_FOR_CHECKSUM="70271181be69cd2c7265b2746f97fccfd7e8aa1059894138a775369c23589ff4"
ARG PKGS="jq imagemagick curl unzip wget acl dirmngr gpg lsof procps netcat dumb-init"

# Installing the packages in an extra container layer for better caching
RUN <<EOF
    # Install packages
    apt-get update -q
    apt-get install -qqy --no-install-recommends ${PKGS}
    rm -rf "/var/lib/apt/lists/*"

    # Install jattach
    curl -sSfL -o /usr/bin/jattach "https://github.com/apangin/jattach/releases/download/${JATTACH_VERSION}/jattach"
    echo "${JATTACH_CHECKSUM} /usr/bin/jattach" | sha256sum -c -
    chmod +x /usr/bin/jattach

    # Install wait-for
    curl -sSfL -o /usr/bin/wait-for "https://github.com/eficode/wait-for/releases/download/${WAIT_FOR_VERSION}/wait-for"
    echo "${WAIT_FOR_CHECKSUM} /usr/bin/wait-for" | sha256sum -c -
    chmod +x /usr/bin/wait-for
EOF

### PART 2: PAYARA ###
# After setting up system, now configure Payara

ARG ASADMIN="${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE}"

USER payara
WORKDIR ${HOME_DIR}

# Copy Payara from build context (cached by Maven)
COPY --chown=payara:payara maven/appserver ${PAYARA_DIR}/

# Copy the system (appserver level) scripts like entrypoint, etc
COPY --chown=payara:payara maven/scripts ${SCRIPT_DIR}/

# Configure the domain to be container and production ready
# -- This is mostly inherited from the "production domain template", experience with Dataverse and
#    https://blog.payara.fish/fine-tuning-payara-server-5-in-production
RUN <<EOF
    # Set admin password
    echo "AS_ADMIN_PASSWORD=" > /tmp/password-change-file.txt
    echo "AS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" >> /tmp/password-change-file.txt
    echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" >> ${PASSWORD_FILE}
    asadmin --user=${ADMIN_USER} --passwordfile=/tmp/password-change-file.txt change-admin-password --domain_name=${DOMAIN_NAME}
    # Start domain for configuration
    ${ASADMIN} start-domain ${DOMAIN_NAME}
    # Allow access to admin with password only
    ${ASADMIN} enable-secure-admin

    ### CONTAINER USAGE ENABLEMENT
    # List & delete memory settings from domain
    for MEMORY_JVM_OPTION in $(${ASADMIN} list-jvm-options | grep "Xm[sx]\|Xss\|NewRatio");
       do
         ${ASADMIN} delete-jvm-options $(echo $MEMORY_JVM_OPTION | sed -e 's/:/\\:/g');
       done
    # Tweak memory settings for containers
    ${ASADMIN} create-jvm-options "-XX\:+UseContainerSupport"
    ${ASADMIN} create-jvm-options "-XX\:MaxRAMPercentage=\${ENV=MEM_MAX_RAM_PERCENTAGE}"
    ${ASADMIN} create-jvm-options "-Xss\${ENV=MEM_XSS}"
    ${ASADMIN} create-jvm-options "-XX\:MinHeapFreeRatio=\${ENV=MEM_MIN_HEAP_FREE_RATIO}"
    ${ASADMIN} create-jvm-options "-XX\:MaxHeapFreeRatio=\${ENV=MEM_MAX_HEAP_FREE_RATIO}"
    ${ASADMIN} create-jvm-options "-XX\:HeapDumpPath=\${ENV=DUMPS_DIR}"
    # Set logging to console only for containers
    ${ASADMIN} set-log-attributes com.sun.enterprise.server.logging.GFFileHandler.logtoFile=false \

    ### PRODUCTION READINESS
    ${ASADMIN} create-jvm-options '-XX\:+UseG1GC'
    ${ASADMIN} create-jvm-options '-XX\:+UseStringDeduplication'
    ${ASADMIN} create-jvm-options '-XX\:+DisableExplicitGC'
    ${ASADMIN} create-jvm-options '-XX\:MaxGCPauseMillis=${ENV=MEM_MAX_GC_PAUSE_MILLIS}'
    ${ASADMIN} create-jvm-options '-XX\:MetaspaceSize=${ENV=MEM_METASPACE_SIZE}'
    ${ASADMIN} create-jvm-options '-XX\:MaxMetaspaceSize=${ENV=MEM_MAX_METASPACE_SIZE}'
    ${ASADMIN} create-jvm-options '-XX\:+IgnoreUnrecognizedVMOptions'
    # Disable autodeploy and hot reload
    ${ASADMIN} set configs.config.server-config.admin-service.das-config.dynamic-reload-enabled="false"
    ${ASADMIN} set configs.config.server-config.admin-service.das-config.autodeploy-enabled="false"
    # Enlarge thread pools
    ${ASADMIN} set server-config.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size="50"
    ${ASADMIN} set server-config.thread-pools.thread-pool.http-thread-pool.max-queue-size=""
    ${ASADMIN} set default-config.thread-pools.thread-pool.thread-pool-1.max-thread-pool-size="250"
    # Enable file caching
    ${ASADMIN} set server-config.network-config.protocols.protocol.http-listener-1.http.file-cache.enabled="true"
    ${ASADMIN} set server-config.network-config.protocols.protocol.http-listener-2.http.file-cache.enabled="true"
    ${ASADMIN} set default-config.network-config.protocols.protocol.http-listener-1.http.file-cache.enabled="true"
    ${ASADMIN} set default-config.network-config.protocols.protocol.http-listener-2.http.file-cache.enabled="true"
    # Disable the HTTPS listener (we are always fronting our appservers with a reverse proxy handling SSL)
    ${ASADMIN} set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.enabled="false"
    # Enlarge and tune EJB pools (cannot do this for server-config as set does not create new entries)
    ${ASADMIN} set default-config.ejb-container.pool-resize-quantity="2"
    ${ASADMIN} set default-config.ejb-container.max-pool-size="128"
    ${ASADMIN} set default-config.ejb-container.steady-pool-size="10"
    # Misc settings
    ${ASADMIN} create-system-properties fish.payara.classloading.delegate="false"
    ${ASADMIN} create-system-properties jersey.config.client.readTimeout="300000"
    ${ASADMIN} create-system-properties jersey.config.client.connectTimeout="300000" \

    ### DATAVERSE APPLICATION SPECIFICS
    # Configure the MicroProfile directory config source to point to /secrets
    ${ASADMIN} set-config-dir --directory="${SECRETS_DIR}"
    # Make request timeouts configurable via MPCONFIG (default to 900 secs = 15 min)
    ${ASADMIN} set 'server-config.network-config.protocols.protocol.http-listener-1.http.request-timeout-seconds=${MPCONFIG=dataverse.http.timeout:900}'
    # TODO: what of the below 3 items can be deleted for container usage?
    ${ASADMIN} create-network-listener --protocol=http-listener-1 --listenerport=8009 --jkenabled=true jk-connector
    ${ASADMIN} set server-config.network-config.protocols.protocol.http-listener-1.http.comet-support-enabled=true
    ${ASADMIN} create-system-properties javax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl
    # Always disable phoning home...
    ${ASADMIN} disable-phone-home \

    ### CLEANUP
    # Stop domain
    ${ASADMIN} stop-domain "${DOMAIN_NAME}"
    # Disable JSP servlet dynamic reloads
    sed -i 's#<servlet-class>org.apache.jasper.servlet.JspServlet</servlet-class>#<servlet-class>org.apache.jasper.servlet.JspServlet</servlet-class>\n    <init-param>\n      <param-name>development</param-name>\n      <param-value>false</param-value>\n    </init-param>\n    <init-param>\n      <param-name>genStrAsCharArray</param-name>\n      <param-value>true</param-value>\n    </init-param>#' "${DOMAIN_DIR}/config/default-web.xml"
    # Cleanup old CA certificates to avoid unnecessary log clutter during startup
    ${SCRIPT_DIR}/removeExpiredCaCerts.sh
    # Delete generated files
    rm -rf \
        "/tmp/password-change-file.txt" \
        "${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/osgi-cache" \
        "${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/logs"
EOF

# Set the entrypoint to tini (as a process supervisor)
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
# This works because we add ${SCRIPT_DIR} to $PATH above!
CMD ["entrypoint.sh"]

LABEL org.opencontainers.image.created="@git.build.time@" \
      org.opencontainers.image.authors="Research Data Management at FZJ <forschungsdaten@fz-juelich.de>" \
      org.opencontainers.image.url="https://guides.dataverse.org/en/latest/container/" \
      org.opencontainers.image.documentation="https://guides.dataverse.org/en/latest/container/" \
      org.opencontainers.image.source="https://github.com/IQSS/dataverse/tree/develop/modules/container-base" \
      org.opencontainers.image.version="@project.version@" \
      org.opencontainers.image.revision="@git.commit.id.abbrev@" \
      org.opencontainers.image.vendor="Global Dataverse Community Consortium" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.title="Dataverse Base Image" \
      org.opencontainers.image.description="This container image provides an application server tuned for Dataverse software"
