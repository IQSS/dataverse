When the installer script (install) can be run either by a developer (inside the source tree), or by an end-user installer, from 
a zip file distribution. 

In the former case, other files that the installer needs can be found in other parts of the source tree. 
for example, the war file (once built) can be found in ../../target/dataverse-4.0.war

When building a distribution archive file however, all the files that the installer needs are piled up in 
the same directory and zipped up; this way it can be run outside the full source tree. 
So the script itself knows to look for these files in 2 places (for example, ../../target/dataverse-4.0.war and ./dataverse-4.0.war)
And a Makefile is provided that copies all the needed files here and makes the final zip file. 

Here's the list of the files that the installer needs: 

the war file:
dataverse-4.0.war

and also:

from scripts/installer:

install
glassfish-setup.sh
pgdriver (the entire directory with all its contents)

from scripts/api:

datasetfields.sh
setup-users.sh
setup-dvs.sh
data (the entire directory with all its contents)

from scripts/database:

reference_data.sql

from conf/jhove:

jhove.conf

