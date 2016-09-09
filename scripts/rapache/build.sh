#!/bin/sh
mkdir -p ~/rpmbuild/SOURCES
mkdir -p ~/rpmbuild/SPECS
wget https://github.com/jeffreyhorner/rapache/archive/v1.2.7.tar.gz -O rapache-1.2.7.tar.gz
tar xzvf rapache-1.2.7.tar.gz rapache-1.2.7/rpm/rapache.spec --strip-components 2
# Move to build dirs
cp -f rapache-1.2.7.tar.gz ~/rpmbuild/SOURCES/
cp -f rapache.spec ~/rpmbuild/SPECS/
cd ~
rpmbuild -ba ~/rpmbuild/SPECS/rapache.spec
