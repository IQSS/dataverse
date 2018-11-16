We now offer an *EXPERIMENTAL* upgrade method allowing users to skip
over a number of releases. E.g., it should be possible now to upgrade
a Dataverse database from v4.8.6 directly to v4.10, without having to
deploy the war files for the 5 releases between these 2 versions and
to run the corresponding database upgrade scripts manually.

One more time, it is *EXPERIMENTAL*! DO NOT attempt to run this script
on your production database WITHOUT BACKING IT UP first!

The script, dbupgrade.sh, must be run in this directory, as follows:

./dbupgrade.sh [VERSION_1] [VERSION_2] [PG_HOST] [PG_PORT] [PG_DB] [PG_USER]

Where 

[VERSION_1] and [VERSION_2] are valid Dataverse release tags, for ex., v4.8.6 and v4.9.4;

NOTE: it is your responsibility to make sure VERSION_1 is the actual
version of your current database!

[PG_HOST] is the server running PostgreSQL used by your Dataverse
[PG_PORT] the port on the PostgreSQL server
[PG_DB]   the name of the PostgreSQL database used by your Dataverse
[PG_USER] the name of the PostgreSQL user used by your Dataverse

The script will also ask you to provide the password for access to the
database above (so that you don't have to enter it on the command
line).

If in doubt as to which PostgreSQL credentials to use for the above,
just use the values listed in the dvnDbPool section of your Glassfish
domain.xml file, for example:

    <jdbc-connection-pool datasource-classname="org.postgresql.ds.PGPoolingDataSource" name="dvnDbPool" res-type="javax.sql.DataSource">
      <property name="ServerName" value="localhost"></property>
      <property name="PortNumber" value="5432"></property>
      <property name="databaseName" value="dvndb"></property>
      <property name="User" value="dvnapp"></property>
      <property name="password" value="xxxxx"></property>
      <property name="create" value="true"></property>
    </jdbc-connection-pool>

An example of the final command line, using the values above: 

./dbupgrade.sh v4.8.6 v4.9.4 localhost 5432 dvndb dvnapp

The script will attempt to validate the values you supply. It will
alert you if the version tags you provided do not correspond to valid
Dataverse releases; or if it fails to connect to the PostgreSQL
database with the credentials you entered. It will exit with an error
message if any of the database scripts fail to run.

The script will remind you to BACK UP YOUR DATABASE before you proceed
with it.

IMPORTANT: This script will run all the create and upgrade scripts for
all the releases up to the version to which you are upgrading. But
please NOTE that this ONLY UPGRADES THE DATABASE. It is still your
responsibility to read the release notes for the releases you have
skipped, and see if there were any additional manual changes
required. For example: new or changed JVM options in the domain.xml
file; upgrades of 3rd party components, such as Solr search engine;
Solr schema updates; - changes like these will still have to be made
manually.
