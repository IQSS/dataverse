# Copyright 2019 Forschungszentrum Jülich GmbH
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# TODO: replace tag with $VERSION arg once d-m-p supports it (expected for 0.34.0 release)
FROM payara/server-full:@payara.version@
LABEL maintainer="FDM FZJ <forschungsdaten@fz-juelich.de>"

ENV DATA_DIR=/data\
    DOCROOT_DIR=/docroot\
    METADATA_DIR=/metadata\
    SECRETS_DIR=/secrets\
    DUMPS_DIR=/dumps\
    DOMAIN_DIR=${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}\
    PGDRIVER_PKG=https://jdbc.postgresql.org/download/postgresql-42.2.12.jar\
    # Make heap dumps on OOM appear in DUMPS_DIR
    JVM_ARGS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=\${ENV=DUMPS_DIR}"

# Create basic pathes
USER root
RUN mkdir -p ${DATA_DIR} ${METADATA_DIR} ${DOCROOT_DIR} ${SECRETS_DIR} ${DUMPS_DIR} && \
    chown -R payara: ${DATA_DIR} ${METADATA_DIR} ${DOCROOT_DIR} ${SECRETS_DIR} ${DUMPS_DIR}

# Install prerequisites
RUN apt-get -qq update && \
    apt-get -qqy install postgresql-client jq imagemagick curl wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Install esh template engine from Github
RUN wget --no-verbose -O esh https://raw.githubusercontent.com/jirutka/esh/v0.3.0/esh && \
    echo 'fe030e23fc1383780d08128eecf322257cec743b esh' | sha1sum -c - && \
    chmod +x esh && mv esh /usr/local/bin

# Install PostgreSQL JDBC driver in AppServer
# TODO: remove this once upstream includes the Postgres Client lib in the WAR.
USER payara
RUN wget --no-verbose -O postgresql.jar ${PGDRIVER_PKG} && \
    mv postgresql.jar ${PAYARA_DIR}/glassfish/lib

# Make docroot of Payara reside in higher level directory for easier targeting
# Due to IQSS/dataverse-kubernetes#177: create the generated pathes so they are
# writeable by us. TBR with #178.
RUN rm -rf ${DOMAIN_DIR}/docroot && \
    ln -s ${DOCROOT_DIR} ${DOMAIN_DIR}/docroot && \
    mkdir -p ${DOMAIN_DIR}/generated/jsp/dataverse

# Copy app and deps from assembly in proper layers
COPY --chown=payara:payara maven/deps ${DEPLOY_DIR}/dataverse/WEB-INF/lib/
COPY --chown=payara:payara maven/app ${DEPLOY_DIR}/dataverse/

# Copy across docker scripts
#COPY --chown=payara:payara docker/dataverse-k8s/bin/* docker/dataverse-k8s/payara/bin/*.sh ${SCRIPT_DIR}/
RUN chmod +x ${SCRIPT_DIR}/*
