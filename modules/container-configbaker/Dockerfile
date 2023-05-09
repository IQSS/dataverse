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
ENV PATH="${PATH}:${SCRIPT_DIR}"

ARG APK_PACKAGES="curl bind-tools netcat-openbsd jq bash dumb-init wait4x"

RUN true && \
  # Install necessary software and tools
  apk add --no-cache ${APK_PACKAGES} && \
  # Make our working directories
  mkdir -p ${SCRIPT_DIR} ${SECRETS_DIR} ${SOLR_TEMPLATE}

# Get in the scripts and make them executable (just in case...)
COPY maven/scripts ${SCRIPT_DIR}/
RUN chmod +x ${SCRIPT_DIR}/*.sh ${BOOTSTRAP_DIR}/*/*.sh

# Copy the Solr config bits
COPY --from=solr /opt/solr/server/solr/configsets/_default ${SOLR_TEMPLATE}/
COPY maven/solr/*.xml ${SOLR_TEMPLATE}/conf/


RUN chmod +x scripts/*.sh

# Set the entrypoint to tini (as a process supervisor)
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
# By default run a script that will print a help message and terminate
CMD ["help.sh"]

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
