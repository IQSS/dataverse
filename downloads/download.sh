#!/bin/sh
curl -L -O http://download.java.net/glassfish/4.1/release/glassfish-4.1.zip
curl -L -O https://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz
# Weld patch *may* fix session bug: https://github.com/IQSS/dataverse/issues/647
# http://transcripts.jboss.org/channel/irc.freenode.org/%23weld-dev/2014/%23weld-dev.2014-11-06.log.html#t2014-11-06T14:44:24
# WELD injecting SessionScoped beans from another session https://developer.jboss.org/message/883410#883410
# https://issues.jboss.org/browse/WELD-1704
# WELD-1704 Fix possible ThreadLocal leak during request and session context association https://github.com/weld/core/commit/19dcc077a6ff6f1f6c70fa902491620e09eafd54
# https://github.com/payara/Payara/issues/76  
curl -L -O http://search.maven.org/remotecontent?filepath=org/jboss/weld/weld-osgi-bundle/2.2.4.Final/weld-osgi-bundle-2.2.4.Final.jar
