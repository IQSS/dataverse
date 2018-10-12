#!/bin/sh

# move things necessary for integration tests into build context.
# this was based off the phoenix deployment; and is likely uglier and bulkier than necessary in a perfect world

mkdir -p testdata/doc/sphinx-guides/source/_static/util/
cp ../solr/7.3.0/schema.xml testdata/
cp ../solr/7.3.0/solrconfig.xml testdata/
cp ../jhove/jhove.conf testdata/
cp ../jhove/jhoveConfig.xsd testdata/
cd ../../
cp -r scripts conf/docker-aio/testdata/
cp doc/sphinx-guides/source/_static/util/pg8-createsequence-prep.sql conf/docker-aio/testdata/doc/sphinx-guides/source/_static/util/
cp doc/sphinx-guides/source/_static/util/createsequence.sql conf/docker-aio/testdata/doc/sphinx-guides/source/_static/util/

# not using dvinstall.zip for setupIT.bash; but still used in install.bash for normal ops
mvn clean
./scripts/installer/custom-build-number
mvn package
cd scripts/installer
make clean
make
mkdir -p ../../conf/docker-aio/dv/install
cp dvinstall.zip ../../conf/docker-aio/dv/install/

