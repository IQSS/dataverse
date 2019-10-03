FROM solr:8.2

USER root

RUN cp -r /opt/solr/server/solr/configsets/_default /opt/solr/server/solr/configsets/dataverse_config
COPY schema.xml /opt/solr/server/solr/configsets/dataverse_config/conf/
COPY solrconfig.xml /opt/solr/server/solr/configsets/dataverse_config/conf/
RUN rm /opt/solr/server/solr/configsets/dataverse_config/conf/managed-schema

RUN ls -la /opt/solr/server/solr/configsets/dataverse_config/conf

EXPOSE 8983

USER $SOLR_USER

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["solr-foreground"]