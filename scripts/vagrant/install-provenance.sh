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
echo "Install Swig 3.x (2.x from yum is too old), requires prce-devel"
sudo yum install -y pcre-devel
cd
mkdir swig
cd swig
wget http://prdownloads.sourceforge.net/swig/swig-3.0.12.tar.gz
tar xvfz swig-3.0.12.tar.gz
cd swig-3.0.12
./configure
make
sudo make install
echo "Done building and installing Swig"
cd /home/vagrant/prov-cpl/bindings/python
sudo yum install -y python-devel
make release
sudo make install
