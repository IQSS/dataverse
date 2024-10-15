ARG PAYARA_VERSION=6.2024.10
FROM payara/server-full:${PAYARA_VERSION}-jdk17

# The first section requires root privileges
USER root

# Change application user and group id
ARG USER_ID=1000
ARG GROUP_ID=1000
RUN usermod -u ${USER_ID} payara \
 && groupmod -g ${GROUP_ID} payara
RUN chown -R -h payara:payara ${HOME_DIR} &>/dev/null || true

# The rest needs to run as application user
USER payara

# deploy WAR file
ARG WAR_FILE=target/dataverse-6.4.war
COPY --chown=payara:payara ${WAR_FILE} ${DEPLOY_DIR}

VOLUME ${DOMAIN_DIR}/docroot
WORKDIR ${DOMAIN_DIR}/docroot