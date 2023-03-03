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
ARG BASE_IMAGE="gdcc/base:unstable"
FROM $BASE_IMAGE

# Copy app and deps from assembly in proper layers
COPY --chown=payara:payara maven/deps ${DEPLOY_DIR}/dataverse/WEB-INF/lib/
COPY --chown=payara:payara maven/app ${DEPLOY_DIR}/dataverse/
COPY --chown=payara:payara maven/supplements ${DEPLOY_DIR}/dataverse/supplements/
COPY --chown=payara:payara maven/scripts ${SCRIPT_DIR}/
RUN chmod +x "${SCRIPT_DIR}"/*

# Create symlinks for jHove
RUN ln -s "${DEPLOY_DIR}/dataverse/supplements/jhove.conf" "${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhove.conf" && \
    ln -s "${DEPLOY_DIR}/dataverse/supplements/jhoveConfig.xsd" "${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhoveConfig.xsd" && \
    sed -i "${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/jhove.conf" -e "s:/usr/local/payara./glassfish/domains/domain1:${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}:g"

LABEL org.opencontainers.image.created="@git.build.time@" \
      org.opencontainers.image.authors="Research Data Management at FZJ <forschungsdaten@fz-juelich.de>" \
      org.opencontainers.image.url="https://guides.dataverse.org/en/latest/container/" \
      org.opencontainers.image.documentation="https://guides.dataverse.org/en/latest/container/" \
      org.opencontainers.image.source="https://github.com/IQSS/dataverse" \
      org.opencontainers.image.version="@project.version@" \
      org.opencontainers.image.revision="@git.commit.id.abbrev@" \
      org.opencontainers.image.vendor="Global Dataverse Community Consortium" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.title="Dataverse Application Image" \
      org.opencontainers.image.description="This container image provides the research data repository software Dataverse in a box."
