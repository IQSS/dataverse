#!/bin/bash

# @todo - /usr/lib/jvm/jre-openjdk does not exist

# move things necessary for integration tests into build context.
# this was based off the phoenix deployment; and is likely uglier and bulkier than necessary in a perfect world

sudo apt-get update           # update the packages so `make` can be included
sudo apt-get install make     # install `make` since it is not automatically included in the OS

# insert test data configurations
mkdir -p testdata/doc/sphinx-guides/source/_static/util/
cp ../solr/8.11.1/schema*.xml testdata/
cp ../solr/8.11.1/solrconfig.xml testdata/
cp ../jhove/jhove.conf testdata/
cp ../jhove/jhoveConfig.xsd testdata/
cd ../../
cp -r scripts conf/docker-aio/testdata/
cp doc/sphinx-guides/source/_static/util/createsequence.sql conf/docker-aio/testdata/doc/sphinx-guides/source/_static/util/

wget -q https://downloads.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz -O dv/deps/apache-maven-3.8.6-bin.tar.gz
tar xfz dv/deps/apache-maven-3.8.6-bin.tar.gz
mkdir maven
mv apache-maven-3.8.6/* maven/
echo "export JAVA_HOME=/usr/local/openjdk-11/bin/java" > maven/maven.sh                 # if getting error about where java is installed try running `whereis java` to find the directory
echo "export M2_HOME=$(pwd)/maven" >> maven/maven.sh
echo "export MAVEN_HOME=$(pwd)/maven" >> maven/maven.sh
echo "export PATH=$PATH:$(pwd)/maven/bin" >> maven/maven.sh
chmod 0755 maven/maven.sh

echo $PATH

# not using dvinstall.zip for setupIT.bash; but still used in install.bash for normal ops
source maven/maven.sh && mvn clean
./scripts/installer/custom-build-number
source maven/maven.sh && mvn package
cd ./scripts/installer
make clean
make

# ITs sometimes need files server-side
# yes, these copies could be avoided by moving the build root here. but the build 
#  context is already big enough that it seems worth avoiding.
cd ../../
cp src/test/java/edu/harvard/iq/dataverse/makedatacount/sushi_sample_logs.json conf/docker-aio/testdata/
