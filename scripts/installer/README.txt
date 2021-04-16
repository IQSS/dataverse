The installer script (install) can be run either by a developer (inside the source tree), or by an end-user installing the Dataverse. The latter will obtain the script as part of the distribution bundle; and they will be running it inside the unzipped bundle directory. 

In the former (developer) case, the installer will be looking for the files it needs in the other directories in the source tree. 
For example, the war file (once built) can be found in ../../target/. The name of the war file will be dataverse-{VERSION}.war, where
{VERSION} is the version number of the Dataverse, obtained from the pom file (../../pom.xml). For example, as of writing this README.txt (July 2015) the war file is ../../target/dataverse-4.1.war/

When building a distribution archive, the Makefile will pile all the files that the installer needs in one directory (./dvinstall here) and then zip it up. We upload the resulting zip bundle on github as the actual software release. This way the end user only gets the files they actually need to install the Dataverse app. So they can do so without pulling the entire source tree. 


The installer script itself (the perl script ./install) knows to look for all these files in 2 places (for example, it will look for the war file in ../../target/; if it's not there, it'll assume this is a distribution bundle and look for it as ./dataverse.war)

Here's the list of the files that the installer needs: 

the war file:
target/dataverse-{VERSION}.war

and also:

from scripts/installer (this directory):

install
glassfish-setup.sh

from scripts/api:

setup-all.sh
setup-builtin-roles.sh
setup-datasetfields.sh
setup-dvs.sh
setup-identity-providers.sh
setup-users.sh
data (the entire directory with all its contents)

from conf/jhove:

jhove.conf

SOLR schema and config files, from conf/solr/8.8.1: 

schema.xml
schema_dv_cmb_copies.xml
schema_dv_cmb_fields.xml
solrconfig.xml
