# Copyright 2019 Forschungszentrum Jülich GmbH
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

FROM payara/server-full:@payara.version@
LABEL maintainer="FDM FZJ <forschungsdaten@fz-juelich.de>"

ENV DATA_DIR=/data\
    DOCROOT_DIR=/docroot\
    METADATA_DIR=/metadata\
    SECRETS_DIR=/secrets\
    DUMPS_DIR=/dumps\
    DOMAIN_DIR=${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}\
    JREBEL_LIB=${HOME_DIR}/jrebel/lib/libjrebel64.so\
    # Make heap dumps on OOM appear in DUMPS_DIR
    JVM_ARGS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=\${ENV=DUMPS_DIR}"\
    # Documenting development options (see init_3_enabledev.sh)
    ENABLE_JMX=0\
    ENABLE_JDWP=0\
    ENABLE_JREBEL=0

# Create basic pathes
USER root
RUN mkdir -p ${DATA_DIR} ${METADATA_DIR} ${DOCROOT_DIR} ${SECRETS_DIR} ${DUMPS_DIR} && \
    chown -R payara: ${DATA_DIR} ${METADATA_DIR} ${DOCROOT_DIR} ${SECRETS_DIR} ${DUMPS_DIR}

# Install prerequisites
RUN apt-get -qq update && \
    apt-get -qqy install jq imagemagick curl wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Install esh template engine from Github
RUN wget --no-verbose -O esh https://raw.githubusercontent.com/jirutka/esh/v0.3.0/esh && \
    echo 'fe030e23fc1383780d08128eecf322257cec743b esh' | sha1sum -c - && \
    chmod +x esh && mv esh /usr/local/bin

# Install JRebel library (for development use)
USER payara
RUN wget --no-verbose -O "${HOME_DIR}/jrebel.zip" http://dl.zeroturnaround.com/jrebel-stable-nosetup.zip && \
    unzip -q "${HOME_DIR}/jrebel.zip" -d "${HOME_DIR}"

# Make docroot of Payara reside in higher level directory for easier targeting
# Due to IQSS/dataverse-kubernetes#177: create the generated pathes so they are
# writeable by us. TBR with #178.
RUN rm -rf ${DOMAIN_DIR}/docroot && \
    ln -s ${DOCROOT_DIR} ${DOMAIN_DIR}/docroot && \
    mkdir -p ${DOMAIN_DIR}/generated/jsp/dataverse

# Copy app and deps from assembly in proper layers
COPY --chown=payara:payara maven/deps ${DEPLOY_DIR}/dataverse/WEB-INF/lib/
COPY --chown=payara:payara maven/app ${DEPLOY_DIR}/dataverse/
COPY --chown=payara:payara maven/supplements ${DEPLOY_DIR}/dataverse/supplements/

# Create symlinks for jHove
RUN ln -s ${DEPLOY_DIR}/dataverse/supplements/jhove.conf ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhove.conf && \
    ln -s ${DEPLOY_DIR}/dataverse/supplements/jhoveConfig.xsd ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhoveConfig.xsd && \
    sed -i ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhove.conf -e "s:/usr/local/payara5/glassfish/domains/domain1:${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}:g"

# Copy init and application scripts
COPY --chown=payara:payara maven/scripts ${SCRIPT_DIR}/
RUN chmod +x ${SCRIPT_DIR}/*
