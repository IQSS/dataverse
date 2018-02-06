#!/bin/bash
echo "Installing prov system..."
echo "Building CPL library"
sudo yum install -y centos-release-scl
sudo yum install -y git redhat-lsb-core devtoolset-7 boost-devel unixODBC-devel
git clone https://github.com/ProvTools/prov-cpl.git
cd /home/vagrant/prov-cpl
wget --quiet https://github.com/nlohmann/json/releases/download/v3.0.1/json.hpp
mv json.hpp include
source /opt/rh/devtoolset-7/enable
sudo make install
echo "Building Python bindings for CPL"
sudo yum install -y swig
cd /home/vagrant/prov-cpl/bindings/python
# FIXME: this `make release` is failing with `include/cplxx.h:356: Error: Syntax error in input(1)`. See https://github.com/ProvTools/prov-cpl/issues/7
make release
