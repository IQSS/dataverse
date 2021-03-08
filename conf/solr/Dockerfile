# Copyright 2019 Forschungszentrum Jülich GmbH
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

FROM solr:@solr.version@

LABEL maintainer="FDM FZJ <forschungsdaten@fz-juelich.de>"

ENV SOLR_OPTS="-Dsolr.jetty.request.header.size=102400" \
    COLLECTION="collection1" \
    CONFIGSET="dataverse" \
    CONFIGSETS_DIR=/opt/solr-@solr.version@/server/solr/configsets

USER root
# Create the Dataverse configset for Solr
# 1) Copy the default configset
# 2) Modify with config files from build context
RUN true && \
    cp -a ${CONFIGSETS_DIR}/_default ${CONFIGSETS_DIR}/${CONFIGSET} && \
    rm ${CONFIGSETS_DIR}/${CONFIGSET}/conf/managed-schema
COPY --chown=root:root maven/config/*.xml ${CONFIGSETS_DIR}/${CONFIGSET}/conf/

USER ${SOLR_USER}
# Make the precreate script build our collection on startup (can change the env vars to use different)
CMD ["sh", "-c", "solr-precreate ${COLLECTION} ${CONFIGSETS_DIR}/${CONFIGSET}"]