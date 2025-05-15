# Copyright 2023 Forschungszentrum Jülich GmbH
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# This build arg must be given or build will fail
ARG SOLR_VERSION
# We simply have this intermediate stage here without any activity to copy the default configset over
FROM solr:${SOLR_VERSION} AS solr

# Let's build ourselves a baker
FROM alpine:3.21

ENV SCRIPT_DIR="/scripts" \
    SECRETS_DIR="/secrets" \
    SOLR_TEMPLATE="/template"
ENV PATH="${PATH}:${SCRIPT_DIR}" \
    BOOTSTRAP_DIR="${SCRIPT_DIR}/bootstrap"

# renovate: datasource=repology depName=alpine_3_21/aws-cli
ENV AWS_CLI_VERSION="2.22.10-r0"
# renovate: datasource=repology depName=alpine_3_21/bash
ENV BASH_VERSION="5.2.37-r0"
# renovate: datasource=repology depName=alpine_3_21/bind-tools
ENV BIND_TOOLS_VERSION="9.18.36-r0"
# renovate: datasource=repology depName=alpine_3_21/curl
ENV CURL_VERSION="8.12.1-r1"
# renovate: datasource=repology depName=alpine_3_21/dumb-init
ENV DUMB_INIT_VERSION="1.2.5-r3"
# renovate: datasource=repology depName=alpine_3_21/ed
ENV ED_VERSION="1.20.2-r0"
# renovate: datasource=repology depName=alpine_3_21/jq
ENV JQ_VERSION="1.7.1-r0"
# renovate: datasource=repology depName=alpine_3_21/netcat-openbsd
ENV NETCAT_VERSION="1.226.1.1-r0"
# renovate: datasource=repology depName=alpine_3_21/postgresql17-client
ENV PGCLIENT17_VERSION="17.5-r0"
# renovate: datasource=repology depName=alpine_3_21/wait4x
ENV WAIT4X_VERSION="2.14.0-r10"

RUN true && \
  # Install necessary software and tools
  apk add --no-cache \
    aws-cli=${AWS_CLI_VERSION} \
    bind-tools=${BIND_TOOLS_VERSION} \
    bash=${BASH_VERSION} \
    curl=${CURL_VERSION} \
    dumb-init=${DUMB_INIT_VERSION} \
    ed=${ED_VERSION} \
    jq=${JQ_VERSION} \
    netcat-openbsd=${NETCAT_VERSION} \
    postgresql17-client=${PGCLIENT17_VERSION} \
    wait4x=${WAIT4X_VERSION} && \
  # Make our working directories
  mkdir -p ${SCRIPT_DIR} ${SECRETS_DIR} ${SOLR_TEMPLATE}

# Get in the scripts 
COPY maven/scripts maven/solr/update-fields.sh ${SCRIPT_DIR}/
# Copy the data from scripts/api that provide the common base setup you'd get from the installer.
# ".dockerignore" will take care of taking only the bare necessities
COPY maven/setup ${SCRIPT_DIR}/bootstrap/base/
# Make the scripts executable
RUN chmod +x ${SCRIPT_DIR}/*.sh ${BOOTSTRAP_DIR}/*/*.sh

# Copy the Solr config bits
COPY --from=solr /opt/solr/server/solr/configsets/_default ${SOLR_TEMPLATE}/
COPY maven/solr/*.xml ${SOLR_TEMPLATE}/conf/
RUN rm ${SOLR_TEMPLATE}/conf/managed-schema.xml


# Set the entrypoint to tini (as a process supervisor)
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
# By default run a script that will print a help message and terminate
CMD ["help.sh"]

LABEL org.opencontainers.image.created="@git.build.time@" \
      org.opencontainers.image.authors="Research Data Management at FZJ <forschungsdaten@fz-juelich.de>" \
      org.opencontainers.image.url="https://guides.dataverse.org/en/latest/container/" \
      org.opencontainers.image.documentation="https://guides.dataverse.org/en/latest/container/" \
      org.opencontainers.image.source="https://github.com/IQSS/dataverse/tree/develop/modules/container-configbaker" \
      org.opencontainers.image.version="@project.version@" \
      org.opencontainers.image.revision="@git.commit.id.abbrev@" \
      org.opencontainers.image.vendor="Global Dataverse Community Consortium" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.title="Dataverse Config Baker Image" \
      org.opencontainers.image.description="This container image configures Dataverse and provides other tooling"
