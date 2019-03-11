FROM centos:7
ARG RPMFILE=rsal-0.1-0.noarch.rpm
RUN yum update; yum install -y epel-release 
COPY ${RPMFILE} /tmp/
RUN yum localinstall -y /tmp/${RPMFILE}
COPY cfg/rsal/rsyncd.conf /etc/rsyncd.conf
COPY cfg/rsal/entrypoint-rsal.sh /
COPY cfg/rsal/lighttpd-modules.conf /etc/lighttpd/modules.conf
COPY cfg/rsal/lighttpd.conf /etc/lighttpd/lighttpd.conf
RUN mkdir -p /public/FK2 
RUN pip2 install -r /opt/rsal/scn/requirements.txt
#COPY doc/testdata/ /hold/
ARG DV_HOST=http://dv_srv:8080
ARG DV_API_KEY=burrito
ENV DV_HOST ${DV_HOST}
ENV DV_API_KEY ${DV_API_KEY}
EXPOSE 873
EXPOSE 80
HEALTHCHECK CMD curl --fail http://localhost/hw.py || exit 1
CMD ["/entrypoint.sh"]
