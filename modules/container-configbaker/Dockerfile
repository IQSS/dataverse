# Copyright 2023 Forschungszentrum Jülich GmbH
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# This build arg must be given or build will fail
ARG SOLR_VERSION
ARG BASE_IMAGE="ubuntu:noble"

# We simply have this intermediate stage here without any activity to copy the default configset over
FROM solr:${SOLR_VERSION} AS solr

# Let's build ourselves a baker
FROM ${BASE_IMAGE}

ENV SCRIPT_DIR="/scripts" \
    SECRETS_DIR="/secrets" \
    SOLR_TEMPLATE="/template"
ENV PATH="${PATH}:${SCRIPT_DIR}" \
    BOOTSTRAP_DIR="${SCRIPT_DIR}/bootstrap"

ARG PKGS="curl dnsutils dumb-init ed jq netcat-openbsd postgresql-client"
# renovate: datasource=github-releases depName=wait4x/wait4x
ARG WAIT4X_VERSION="v3.2.0"
# renovate: datasource=pypi depName=awscli
ARG AWSCLI_VERSION="1.40.15"
ARG PYTHON_PKGS="awscli==${AWSCLI_VERSION}"

# Auto-populated by BuildKit / buildx
ARG TARGETARCH
SHELL ["/bin/bash", "-eu", "-c"]

RUN true && \
  # Make our working directories
  mkdir -p ${SCRIPT_DIR} ${SECRETS_DIR} ${SOLR_TEMPLATE} && \

  # Install packages
  apt-get update -q && \
  apt-get install -qqy --no-install-recommends ${PKGS} && \

  # Workaround to install Python and pipx 1.5+ on Ubuntu 24.04 LTS: first install Python and pipx 1.4 \
  # Adapted from https://github.com/pypa/pipx/issues/1481#issuecomment-2593124603
  apt -qqy --no-install-recommends install python3 python3-venv pipx && \
  # Now install 1.5+ in ~/.local/bin/
  pipx install pipx && \
  # Remove 1.4 again
  apt purge -qqy --autoremove pipx && \
  # Install 1.5+ in /usr/local/bin/pipx
  ~/.local/bin/pipx install --global pipx && \
  # Remove the virtual env install of pipx
  rm -rf "~/.local" && \

  # Cleanup apt cache
  rm -rf "/var/lib/apt/lists/*"

# New step (and shell) as this is a different manager and we need pipx around as command
RUN true && \
  # Install things not available as packages \
  ARCH="${TARGETARCH:-$(uname -m | sed -e 's/x86_64/amd64/' -e 's/\(arm\)\(64\)\?.*/\1\2/' -e 's/aarch64$/arm64/')}" && \

  # 1. wait4x \
  curl -sSfL -o /usr/bin/wait4x.tar.gz "https://github.com/wait4x/wait4x/releases/download/${WAIT4X_VERSION}/wait4x-linux-${ARCH}.tar.gz" && \
  curl -sSfL -o /tmp/w4x-checksum "https://github.com/wait4x/wait4x/releases/download/${WAIT4X_VERSION}/wait4x-linux-${ARCH}.tar.gz.sha256sum" && \
  echo "$(cat /tmp/w4x-checksum | cut -f1 -d" ") /usr/bin/wait4x.tar.gz" | sha256sum -c - && \
  tar -xzf /usr/bin/wait4x.tar.gz -C /usr/bin && chmod +x /usr/bin/wait4x && \

  # 2. Python packages
  pipx install --global ${PYTHON_PKGS}

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

# Workaround for fabric8io/docker-maven-plugin#1865
ARG APP_IMAGE_VERSION
LABEL org.opencontainers.image.created="@git.build.time@" \
      org.opencontainers.image.authors="Research Data Management at FZJ <forschungsdaten@fz-juelich.de>" \
      org.opencontainers.image.url="https://guides.dataverse.org/en/latest/container/" \
      org.opencontainers.image.documentation="https://guides.dataverse.org/en/latest/container/" \
      org.opencontainers.image.source="https://github.com/IQSS/dataverse/tree/develop/modules/container-configbaker" \
      org.opencontainers.image.version="$APP_IMAGE_VERSION" \
      org.opencontainers.image.revision="@git.commit.id.abbrev@" \
      org.opencontainers.image.vendor="Global Dataverse Community Consortium" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.title="Dataverse Config Baker Image" \
      org.opencontainers.image.description="This container image configures Dataverse and provides other tooling" \
      org.opencontainers.image.base.name="$BASE_IMAGE"
