# this is built off the base container
FROM gdcc/base:unstable

USER root

RUN mkdir -p /dataverse

COPY pom.xml /dataverse/
COPY modules /dataverse/modules/
COPY local_lib /dataverse/local_lib/
COPY scripts /dataverse/scripts/
COPY conf/jhove/ /dataverse/conf/jhove/

RUN useradd --create-home --shell /bin/bash dataverse

# switch to non-root user as this is more secure
USER dataverse
WORKDIR /

# add Payara to PATH
ENV PATH="${PATH}:${PAYARA_DIR}/bin"

USER root

# set ownership so later the dataverse user can write to and make changes
RUN chown -R dataverse "${PAYARA_DIR}"

COPY --chown=dataverse:dataverse ./preboot.sh /preboot.sh
RUN chmod +x /preboot.sh
COPY --chown=dataverse:dataverse ./postboot.sh /postboot.sh
RUN chmod +x /postboot.sh

# auto-deploy the .war file
ENV DEPLOY_DIR /dataverse/target/

# pre and post boot commands
ENV PREBOOT_COMMANDS /preboot.sh
ENV POSTBOOT_COMMANDS /postboot.sh

USER dataverse

# helpful for debugging purposes to just start up the container
# CMD ["tail", "-f", "/dev/null"]