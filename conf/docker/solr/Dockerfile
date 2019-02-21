FROM centos:7
MAINTAINER Dataverse (support@dataverse.org)

RUN yum install -y unzip java-1.8.0-openjdk-devel lsof

# Install Solr 7.3.0
# The context of the build is the "conf" directory.
COPY solr-7.3.0.tgz /tmp
RUN cd /tmp \
    && tar xvfz solr-7.3.0.tgz \
    && rm solr-7.3.0.tgz \
    && mkdir /usr/local/solr \
    && mv solr-7.3.0 /usr/local/solr/

COPY schema.xml /tmp
COPY solrconfig_master.xml /tmp
COPY solrconfig_slave.xml /tmp

RUN chmod g=u /etc/passwd

RUN chgrp -R 0 /usr/local/solr && \
    chmod -R g=u /usr/local/solr

EXPOSE 8983

COPY Dockerfile /
COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
USER 1001
CMD ["solr"]
