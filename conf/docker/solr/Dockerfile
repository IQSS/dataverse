FROM centos:7.2.1511
MAINTAINER Dataverse (support@dataverse.org)

RUN yum install -y wget unzip perl git java-1.8.0-openjdk-devel postgresql.x86_64

# Install Solr 7.3.0
# The context of the build is the "conf" directory.
COPY solr/7.3.0/schema.xml /tmp
COPY solr/7.3.0/solrconfig.xml /tmp

RUN cd /tmp && wget https://archive.apache.org/dist/lucene/solr/7.3.0/solr-7.3.0.tgz

EXPOSE 8983

COPY docker/solr/Dockerfile /Dockerfile
COPY docker/solr/entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD ["solr"]
