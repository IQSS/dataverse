#!/bin/sh

echo "dcm configs on dv side to be done"

# in homage to dataverse traditions, reset to insecure "burrito" admin API key
sudo -u postgres psql -c "update apitoken set tokenstring='burrito' where id=1;" dvndb
sudo -u postgres psql -c "update authenticateduser set superuser='t' where id=1;" dvndb

# dataverse configs for DCM
curl -X PUT -d "SHA-1" "http://localhost:8080/api/admin/settings/:FileFixityChecksumAlgorithm"
curl -X PUT "http://localhost:8080/api/admin/settings/:UploadMethods" -d "dcm/rsync+ssh"
curl -X PUT "http://localhost:8080/api/admin/settings/:DataCaptureModuleUrl" -d "http://dcmsrv"

# configure for RSAL downloads; but no workflows or RSAL yet
curl -X PUT "http://localhost:8080/api/admin/settings/:DownloadMethods" -d "rsal/rsync"

# publish root dataverse
curl -X POST -H "X-Dataverse-key: burrito" "http://localhost:8080/api/dataverses/root/actions/:publish"

# symlink `hold` volume 
mkdir -p /usr/local/glassfish4/glassfish/domains/domain1/files/
ln -s /hold /usr/local/glassfish4/glassfish/domains/domain1/files/10.5072

# need to set siteUrl
cd /usr/local/glassfish4
bin/asadmin create-jvm-options "\"-Ddataverse.siteUrl=http\://localhost\:8084\""
