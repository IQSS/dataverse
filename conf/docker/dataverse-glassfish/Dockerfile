FROM centos:7
MAINTAINER Dataverse (support@dataverse.org)

COPY glassfish-4.1.zip /tmp
COPY weld-osgi-bundle-2.2.10.Final-glassfish4.jar /tmp
COPY default.config /tmp
# Install dependencies
#RUN yum install -y unzip
RUN yum install -y \
    cronie \
    git \
    java-1.8.0-openjdk-devel \
    nc \
    perl \
    postgresql \
    sha1sum \
    unzip \
    wget

ENV GLASSFISH_DOWNLOAD_SHA1 d1a103d06682eb08722fbc9a93089211befaa080
ENV GLASSFISH_DIRECTORY "/usr/local/glassfish4"
ENV HOST_DNS_ADDRESS "localhost"
ENV POSTGRES_DB "dvndb"
ENV POSTGRES_USER "dvnapp"
ENV RSERVE_USER "rserve"
ENV RSERVE_PASSWORD "rserve"

#RUN exitEarlyBeforeJq
RUN yum -y install epel-release \
    jq 

COPY dvinstall.zip /tmp

#RUN ls /tmp
#
RUN find /tmp
#
#RUN exitEarly

# Install Glassfish 4.1
RUN cd /tmp \
    && unzip glassfish-4.1.zip \
    && mv glassfish4 /usr/local \
    && cd /usr/local/glassfish4/glassfish/modules \
    && rm weld-osgi-bundle.jar \
    && cp /tmp/weld-osgi-bundle-2.2.10.Final-glassfish4.jar . \
    && cd /tmp && unzip /tmp/dvinstall.zip \
    && chmod 777 -R /tmp/dvinstall/ \
    #FIXME: Patch Grizzly too!
    && echo "Done installing and patching Glassfish"

RUN chmod g=u /etc/passwd

RUN mkdir -p /home/glassfish
RUN chgrp -R 0 /home/glassfish && \
    chmod -R g=u /home/glassfish

RUN mkdir -p /usr/local/glassfish4
RUN chgrp -R 0 /usr/local/glassfish4 && \
    chmod -R g=u /usr/local/glassfish4

#JHOVE
RUN cp /tmp/dvinstall/jhove* /usr/local/glassfish4/glassfish/domains/domain1/config


#SETUP JVM OPTIONS 
ARG DOCKER_BUILD="true" 
RUN echo $DOCKER_BUILD
RUN /tmp/dvinstall/glassfish-setup.sh
###glassfish-setup will handle everything in Dockerbuild

##install jdbc driver 
RUN cp /tmp/dvinstall/pgdriver/postgresql-42.2.2.jar /usr/local/glassfish4/glassfish/domains/domain1/lib

# Customized persistence xml to avoid database recreation
#RUN mkdir -p /tmp/WEB-INF/classes/META-INF/ 
#COPY WEB-INF/classes/META-INF/persistence.xml /tmp/WEB-INF/classes/META-INF/

# Install iRods iCommands
#RUN  cd /tmp \
#   && yum -y install epel-release \
#   && yum -y install ftp://ftp.renci.org/pub/irods/releases/4.1.6/centos7/irods-icommands-4.1.6-centos7-x86_64.rpm 

#COPY config-glassfish /root/dvinstall
#COPY restart-glassfish /root/dvinstall
#COPY config-dataverse /root/dvinstall

#RUN cd /root/dvinstall && ./config-dataverse
COPY ./entrypoint.sh /
#COPY ./ddl /root/dvinstall
#COPY ./init-postgres /root/dvinstall
#COPY ./init-glassfish /root/dvinstall
#COPY ./init-dataverse /root/dvinstall
#COPY ./setup-all.sh /root/dvinstall
#COPY ./setup-irods.sh /root/dvinstall
COPY ./Dockerfile /

EXPOSE      8080 

ENTRYPOINT ["/entrypoint.sh"]
CMD ["dataverse"]
