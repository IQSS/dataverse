# Copyright 2023 Forschungszentrum Jülich GmbH
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# This build arg must be given or build will fail
ARG SOLR_VERSION
# We simply have this intermediate stage here without any activity to copy the default configset over
FROM solr:${SOLR_VERSION} AS solr

# Let's build us a baker
FROM alpine:3

ENV SCRIPT_DIR="/scripts" \
    SECRETS_DIR="/secrets" \
    SOLR_TEMPLATE="/template"
ENV PATH="${PATH}:${SCRIPT_DIR}" \
    BOOTSTRAP_DIR="${SCRIPT_DIR}/bootstrap"

ARG APK_PACKAGES="curl bind-tools netcat-openbsd jq bash dumb-init wait4x ed"

RUN true && \
  # Install necessary software and tools
  apk add --no-cache ${APK_PACKAGES} && \
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
