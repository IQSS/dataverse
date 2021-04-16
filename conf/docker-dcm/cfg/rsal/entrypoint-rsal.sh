#!/bin/sh

#/usr/bin/rsync --no-detach --daemon --config /etc/rsyncd.conf
/usr/bin/rsync --daemon --config /etc/rsyncd.conf
lighttpd -D -f /etc/lighttpd/lighttpd.conf
