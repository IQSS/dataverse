FROM centos:7.2.1511
MAINTAINER Dataverse (support@dataverse.org)

RUN yum install -y wget unzip perl git java-1.8.0-openjdk-devel postgresql.x86_64

# Install Solr 7.2.1
# The context of the build is the "conf" directory.
COPY solr/7.2.1/schema.xml /tmp
COPY solr/7.2.1/solrconfig.xml /tmp


RUN cd /tmp && wget https://archive.apache.org/dist/lucene/solr/7.2.1/solr-7.2.1.tgz  && \
	tar xvzf solr-7.2.1.tgz  && \
	mv solr-7.2.1 /usr/local/  && \
	cd /usr/local/solr-7.2.1/server/solr/collection1/conf/  && \
	mv schema.xml schema.xml.backup  && \
        mv solrconfig.xml solrconfig.xml.backup  && \
	cp /tmp/schema.xml . && \
        cp /tmp/solrconfig.xml . && \
	rm /tmp/solr-7.2.1.tgz

RUN ln -s /usr/local/solr-7.2.1/server/logs /var/log/solr

VOLUME  /usr/local/solr-7.2.1/server/solr/collection1/data

EXPOSE 8983

COPY docker/solr/Dockerfile /Dockerfile
COPY docker/solr/entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD ["solr"]
