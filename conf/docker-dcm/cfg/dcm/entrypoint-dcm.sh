#!/bin/sh

/etc/init.d/sshd start
/etc/init.d/redis start
/etc/init.d/rq start
lighttpd -D -f /etc/lighttpd/lighttpd.conf
