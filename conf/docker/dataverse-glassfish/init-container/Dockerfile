FROM centos:7
MAINTAINER Dataverse (support@dataverse.org)

### init-container is an Init Container for glassfish service in OpenShift or other Kubernetes environment
# This initContainer will take care of setting up glassfish

# Install dependencies
RUN yum install -y \
    nc \
    perl \
    postgresql \
    sha1sum 

COPY install /

ENTRYPOINT ["/install", "--pg_only", "--yes"]
