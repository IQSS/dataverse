FROM amazoncorretto:17-alpine

ENV JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV SHIBBOLETH_IDP_VERSION=5.1.4
ENV JETTY_HOME=/opt/jetty-home
ENV JETTY_BASE=/opt/jetty-base

# Install dependencies
RUN apk update && apk add --no-cache \
    bash \
    curl \
    unzip \
    openjdk17 \
    && rm -rf /var/cache/apk/*

# Download and install Jetty
RUN curl -L https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/12.0.18/jetty-home-12.0.18.zip -o /opt/jetty-home.zip \
    && unzip /opt/jetty-home.zip -d /opt \
    && mv /opt/jetty-home-12.0.18 $JETTY_HOME \
    && rm /opt/jetty-home.zip

# Set up Jetty Base and enable required modules
RUN mkdir -p $JETTY_BASE \
    && cd $JETTY_BASE \
    && java -jar $JETTY_HOME/start.jar --add-modules=server,http,ee9-deploy,ee9-jsp \
    && echo "Jetty base setup completed!"

# Download and install Shibboleth IdP
RUN curl -L https://shibboleth.net/downloads/identity-provider/${SHIBBOLETH_IDP_VERSION}/shibboleth-identity-provider-${SHIBBOLETH_IDP_VERSION}.zip -o /tmp/shibboleth-idp.zip \
    && mkdir -p /opt/shibboleth-idp \
    && unzip /tmp/shibboleth-idp.zip -d /opt/shibboleth-idp \
    && rm /tmp/shibboleth-idp.zip

# Move Shibboleth IdP webapp to Jetty webapps
RUN mkdir -p $JETTY_BASE/webapps \
    && cp -r /opt/shibboleth-idp/shibboleth-identity-provider-${SHIBBOLETH_IDP_VERSION}/webapp $JETTY_BASE/webapps/idp \
    && echo "Shibboleth IdP deployed to Jetty webapps!"

# Copy pre-installed idp configuration
COPY shibboleth-idp/ /opt/shibboleth-idp/

# Download required JSTL API jar
RUN mkdir -p $JETTY_BASE/webapps/idp/WEB-INF/lib \
    && curl -L https://repo1.maven.org/maven2/jakarta/servlet/jsp/jstl/jakarta.servlet.jsp.jstl-api/3.0.0/jakarta.servlet.jsp.jstl-api-3.0.0.jar \
    -o $JETTY_BASE/webapps/idp/WEB-INF/lib/jakarta.servlet.jsp.jstl-api-3.0.0.jar

# Set working directory to Jetty Base
WORKDIR $JETTY_BASE

# Expose Jetty port
EXPOSE 8080

CMD ["java", "-jar", "/opt/jetty-home/start.jar"]
