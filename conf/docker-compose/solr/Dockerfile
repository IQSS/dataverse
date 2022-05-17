# https://hub.docker.com/_/solr
FROM solr:8.11.1

# https://guides.dataverse.org/en/latest/installation/prerequisites.html#solr

USER root

COPY *.xml /

# increase the number of file descriptors and max processes
RUN echo "solr soft nproc 65000" >> /etc/security/limits.conf && \
    echo "solr hard nproc 65000" >> /etc/security/limits.conf && \
    echo "solr soft nofile 65000" >> /etc/security/limits.conf && \
    echo "solr hard nofile 65000" >> /etc/security/limits.conf

# increase Header size
RUN sed -i "s/name=\"solr.jetty.request.header.size\" default=\"8192\"/name=\"solr.jetty.request.header.size\" default=\"102400\"/g" /opt/solr/server/etc/jetty.xml

COPY --chown=solr:solr startup.sh /startup.sh
RUN chmod +x /startup.sh

# switch back to normal runtime user for security purposes
USER solr

CMD ["/startup.sh"]