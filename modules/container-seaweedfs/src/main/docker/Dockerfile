# https://hub.docker.com/r/chrislusf/seaweedfs
FROM chrislusf/seaweedfs:latest

USER root

# use HTTP connection for adding the needed ca-certificates package
# install it and then swap back to HTTPS
RUN sed -i 's,https,http,g' /etc/apk/repositories && \
    apk add --no-cache ca-certificates && \
    update-ca-certificates && \
    sed -i 's,http,https,g' /etc/apk/repositories && \
    apk update && apk upgrade && apk add --no-cache curl bash

# https://github.com/chrislusf/seaweedfs/wiki/Amazon-S3-API#static-configuration
COPY config.json /config.json
