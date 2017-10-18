FROM centos:7.2.1511
MAINTAINER Dataverse (support@dataverse.org)

RUN yum install -y wget unzip perl git java-1.8.0-openjdk-devel postgresql.x86_64

# Install Solr 4.6.0
# The context of the build is the "conf" directory.
COPY solr/4.6.0/schema.xml /tmp

RUN cd /tmp && wget https://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz  && \
	tar xvzf solr-4.6.0.tgz  && \
	mv solr-4.6.0 /usr/local/  && \
	cd /usr/local/solr-4.6.0/example/solr/collection1/conf/  && \
	mv schema.xml schema.xml.backup  && \
	cp /tmp/schema.xml . && \
	rm /tmp/solr-4.6.0.tgz

RUN ln -s /usr/local/solr-4.6.0/example/logs /var/log/solr

VOLUME  /usr/local/solr-4.6.0/example/solr/collection1/data

EXPOSE 8983

COPY docker/solr/Dockerfile /Dockerfile
COPY docker/solr/entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD ["solr"]
