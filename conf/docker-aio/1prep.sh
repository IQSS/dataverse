#!/bin/sh

export LANG="en_US.UTF-8"

# move things necessary for integration tests into build context.
# this was based off the phoenix deployment; and is likely uglier and bulkier than necessary in a perfect world

mkdir -p testdata/doc/sphinx-guides/source/_static/util/
cp ../solr/7.2.1/schema.xml testdata/
cp ../solr/7.2.1/solrconfig.xml testdata/
cp ../jhove/jhove.conf testdata/
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
cp dvinstall.zip ../../conf/docker-aio/dv/install/

