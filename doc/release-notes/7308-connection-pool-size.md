### Notes to Installation Admins

New JVM Option for connection pool size

Larger installations may want to increase the number of open S3 connections allowed (default is 256): For example, 

``./asadmin create-jvm-options "-Ddataverse.files.<id>.connection-pool-size=4096"`

(link to config guide)