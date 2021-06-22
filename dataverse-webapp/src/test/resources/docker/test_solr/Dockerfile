FROM solr:8.2

USER root

RUN cp -r /opt/solr/server/solr/configsets/_default /opt/solr/server/solr/configsets/dataverse_config
COPY schema.xml /opt/solr/server/solr/configsets/dataverse_config/conf/
COPY solrconfig.xml /opt/solr/server/solr/configsets/dataverse_config/conf/
RUN rm /opt/solr/server/solr/configsets/dataverse_config/conf/managed-schema

RUN cp -r /opt/solr/server/solr/configsets/_default /opt/solr/server/solr/configsets/ror_config
COPY ror/schema.xml /opt/solr/server/solr/configsets/ror_config/conf/
COPY ror/solrconfig.xml /opt/solr/server/solr/configsets/ror_config/conf/
RUN rm /opt/solr/server/solr/configsets/ror_config/conf/managed-schema

RUN ls -la /opt/solr/server/solr/configsets/dataverse_config/conf
RUN ls -la /opt/solr/server/solr/configsets/ror_config/conf

EXPOSE 8983
USER $SOLR_USER

ENTRYPOINT ["docker-entrypoint.sh"]
CMD precreate-core collection1 /opt/solr/server/solr/configsets/dataverse_config; precreate-core rorSuggestions /opt/solr/server/solr/configsets/ror_config; solr start -f