FROM centos:7
# OS dependencies
RUN yum install -y java-1.8.0-openjdk-headless postgresql-server sudo epel-release unzip perl curl httpd
RUN yum install -y jq

# copy and unpack dependencies (solr, glassfish)
COPY dv /tmp/dv
COPY testdata/schema.xml /tmp/dv
COPY testdata/solrconfig.xml /tmp/dv
# IPv6 and localhost appears to be related to some of the intermittant connection issues
COPY disableipv6.conf /etc/sysctl.d/
RUN rm /etc/httpd/conf/*
COPY httpd.conf /etc/httpd/conf 
RUN cd /opt ; tar zxf /tmp/dv/deps/solr-7.3.0dv.tgz 
RUN cd /opt ; tar zxf /tmp/dv/deps/glassfish4dv.tgz

# this copy of domain.xml is the result of running `asadmin set server.monitoring-service.module-monitoring-levels.jvm=LOW` on a default glassfish installation (aka - enable the glassfish REST monitir endpoint for the jvm`
COPY domain-restmonitor.xml /opt/glassfish4/glassfish/domains/domain1/config/domain.xml

RUN sudo -u postgres /usr/bin/initdb -D /var/lib/pgsql/data

# copy configuration related files
RUN cp /tmp/dv/pg_hba.conf /var/lib/pgsql/data/
RUN cp -r /opt/solr-7.3.0/server/solr/configsets/_default /opt/solr-7.3.0/server/solr/collection1
RUN cp /tmp/dv/schema.xml /opt/solr-7.3.0/server/solr/collection1/conf/schema.xml
RUN cp /tmp/dv/solrconfig.xml /opt/solr-7.3.0/server/solr/collection1/conf/solrconfig.xml

# skipping glassfish user and solr user (run both as root)

#solr port
EXPOSE 8983

# postgres port
EXPOSE 5432

# glassfish port
EXPOSE 8080

# apache port, http
EXPOSE 80

RUN mkdir /opt/dv

# yeah - still not happy if glassfish isn't in /usr/local :<
RUN ln -s /opt/glassfish4 /usr/local/glassfish4
COPY dv/install/ /opt/dv/
COPY install.bash /opt/dv/
COPY entrypoint.bash /opt/dv/
COPY testdata /opt/dv/testdata
COPY testscripts/* /opt/dv/testdata/
COPY setupIT.bash /opt/dv
WORKDIR /opt/dv

# healthcheck for glassfish only (assumes modified domain.xml); 
#  does not check dataverse application status.
HEALTHCHECK CMD curl --fail http://localhost:4848/monitoring/domain/server.json || exit 1
CMD ["/opt/dv/entrypoint.bash"]
