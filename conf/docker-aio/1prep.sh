#!/bin/bash

# move things necessary for integration tests into build context.
# this was based off the phoenix deployment; and is likely uglier and bulkier than necessary in a perfect world

mkdir -p testdata/doc/sphinx-guides/source/_static/util/
cp ../solr/7.7.2/schema*.xml testdata/
cp ../solr/7.7.2/solrconfig.xml testdata/
cp ../solr/7.7.2/updateSchemaMDB.sh testdata/
cp ../jhove/jhove.conf testdata/
cp ../jhove/jhoveConfig.xsd testdata/
cd ../../
cp -r scripts conf/docker-aio/testdata/
cp doc/sphinx-guides/source/_static/util/createsequence.sql conf/docker-aio/testdata/doc/sphinx-guides/source/_static/util/

wget -q https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar xfz apache-maven-3.6.3-bin.tar.gz
mkdir maven
mv apache-maven-3.6.3/* maven/
echo "export JAVA_HOME=/usr/lib/jvm/jre-openjdk" > maven/maven.sh
echo "export M2_HOME=../maven" >> maven/maven.sh
echo "export MAVEN_HOME=../maven" >> maven/maven.sh
echo "export PATH=../maven/bin:${PATH}" >> maven/maven.sh
chmod 0755 maven/maven.sh

# not using dvinstall.zip for setupIT.bash; but still used in install.bash for normal ops
source maven/maven.sh && mvn clean
./scripts/installer/custom-build-number
source maven/maven.sh && mvn package
cd scripts/installer
make clean
make
mkdir -p ../../conf/docker-aio/dv/install
cp dvinstall.zip ../../conf/docker-aio/dv/install/

# ITs sometimes need files server-side
# yes, these copies could be avoided by moving the build root here. but the build 
#  context is already big enough that it seems worth avoiding.
cd ../../
cp src/test/java/edu/harvard/iq/dataverse/makedatacount/sushi_sample_logs.json conf/docker-aio/testdata/
