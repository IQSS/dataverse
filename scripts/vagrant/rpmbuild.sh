#!/bin/sh
rpm -Uvh http://dl.fedoraproject.org/pub/epel/7/x86_64/e/epel-release-7-7.noarch.rpm
yum install -y rpm-build httpd-devel libapreq2-devel R-devel
