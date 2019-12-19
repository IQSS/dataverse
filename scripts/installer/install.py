#!/usr/bin/python

from ConfigParser import SafeConfigParser
import os.path
from os import fdopen
from os import environ
from os import getuid
import psycopg2
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT
import re
from shutil import copy2, move
import subprocess 
import getopt
import sys
import pwd
from tempfile import mkstemp
import xml.etree.cElementTree as ET
from installUtils import (checkUser, linuxRAM, macosRAM)
from installGlassfish import runAsadminScript

# process command line arguments: 
# (all the options supported by the old installer replicated verbatim!)
shortOptions = "vyf"
longOptions = ["verbose", 
               "yes", 
               "force",
               "noninteractive", 
               "pg_only", 
               "skip_db_setup", 
               "nogfpasswd", 
               "hostname=", 
               "gfuser=", 
               "gfdir=", 
               "mailserver=", 
               "admin_email=",
               "config_file="]

# default values of the command line options:
nonInteractive=False
verbose=False
force=False
pgOnly=False
skipDatabaseSetup=False
noGlassfishPassword=False
hostName=""
gfUser=""
gfDir=""
mailServer=""
adminEmail=""
configFile="default.config"

cmdLineArguments = sys.argv

try:
   parsedArguments, values = getopt.getopt(cmdLineArguments[1:], shortOptions, longOptions)
except getopt.error as err:
   print (str(err))
   sys.exit(2)

for currentArgument, currentValue in parsedArguments:
   if currentArgument in ("-v", "--verbose"):
      print ("enabling verbose mode")
      verbose=True
   elif currentArgument in ("-y", "--noninteractive", "--yes"):
      print ("enabling non-interactive mode")
      nonInteractive=True
   elif currentArgument in ("--force"):
      print ("enabling \"force\" mode - will continue even if the database specified already exists!")
      force=True
   elif currentArgument in ("--pg_only"):
      pgOnly=True
   elif currentArgument in ("--skip_db_setup"):
      skipDatabaseSetup=True
   elif currentArgument in ("--nogfpasswd"):
      noGlassfishPassword=True
   elif currentArgument in ("--hostname"):
      hostName=currentValue
   elif currentArgument in ("--gfuser"):
      gfUser=currentValue
   elif currentArgument in ("--gfdir"):
      gfDir=currentValue
   elif currentArgument in ("--mailserver"):
      mailServer=currentValue
   elif currentArgument in ("--admin_email"):
      adminEmail=currentValue
   elif currentArgument in ("--config_file"):
      configFile=currentValue

# "podName" is specified as an env. variable and used in Docker scripts;
# (@todo: is it really needed though? can we use the existing option "skipDatabaseSetup" instead?)
# (ok, there may be some custom code that relies on this MY_POD_NAME being set to a cetain value)
podName = os.environ.get('MY_POD_NAME')

# ----- configuration ----- #

if pgOnly: 
   configSections = ["database"]
else:
   configSections = ["glassfish",
                     "database",
                     "rserve",
                     "system",
                     "doi"]


# read pre-defined defaults:
config = SafeConfigParser()
if not os.path.exists(configFile):
   sys.exit("Configuration file "+configFile+" not found.")
config.read(configFile)

# expected dataverse defaults
apiUrl = "http://localhost:8080/api"

# there's now a single driver that works for all supported versions:
# jodbc.postgresql.org recommends 4.2 for Java 8.
# updated drivers may be obtained from
#  https://jdbc.postgresql.org/download.html
pgJdbcDriver = "postgresql-42.2.2.jar"

# 0. A few preliminary checks:                                                                                   
# 0a. OS flavor:
 
try:
   unameToken = subprocess.check_output(["uname", "-a"]).split()[0]
except:
   sys.exit("Warning! Failed to execute \"uname -a\"; aborting")

if unameToken == "Darwin":
   print "\nThis appears to be a MacOS X system; good."
   myOS = "MacOSX"
elif unameToken == "Linux":
   if os.path.exists("/etc/redhat-release"):
      print "\nThis appears to be a RedHat system; good."
      myOS = "RedHat"
   else:
      print "\nThis appears to be a non-RedHat Linux system;"
      print "this installation *may* succeed; but we're not making any promises!"
      myOS = "Linux"
else:
   print "\nERROR: The installer only works on a RedHat Linux or MacOS X system!"
   print "This appears to be neither. Aborting";
   sys.exit(1)

# 0b. host name:
# the name entered on the command line as --hostname overrides other methods:
if hostName != "":
   config.set('glassfish', 'HOST_DNS_ADDRESS', hostName)
else:
   # (@todo? skip this if in --noninteractive mode? i.e., just use what's in default.config?)
   try:
      hostNameFromCommand = subprocess.check_output(["hostname"]).rstrip()
      config.set('glassfish', 'HOST_DNS_ADDRESS', hostNameFromCommand)
   except:
      print "Warning! Failed to execute \"hostname\"; assuming \"" + config.get('glassfish', 'HOST_DNS_ADDRESS') + "\""

# 0c. current OS user. 
# try to get the current username from the environment; the first one we find wins:
currentUser = os.environ.get('LOGNAME')
if currentUser is None or currentUser == "":
   currentUser = os.environ.get('USER')
if currentUser is None or currentUser == "":
   currentUser = pwd.getpwuid(os.getuid())[0]
# we may need this username later (when we decide if we need to sudo to run asadmin?) 

# if the username was specified on the command line, it takes precedence:
if gfUser != "":
   config.set('glassfish', 'GLASSFISH_USER', gfUser)
   # check if the glassfish user specified actually exists
#   ret = subprocess.call("id "+gfUser+" > /dev/null 2>&1", shell=True)
#   if ret != 0:
   if not checkUser(gfUser):
      sys.exit("Couldn't find user "+gfUser+". Please ensure the account exists.")
else:
   # use the username from the environment that we just found, above: 
   # (@todo? skip this if in --noninteractive mode? i.e., just use what's in default.config?)
   if currentUser is not None and currentUser != "":
      config.set('glassfish', 'GLASSFISH_USER', currentUser)

# warn the user that runnning the installer as root is not recommended, if that's what they are doing
if not nonInteractive:
   if os.getuid() == 0:
      print "\n####################################################################"
      print "     It is recommended that this script not be run as root."
      print " Consider creating a glassfish service account, giving it ownership"
      print "  on the glassfish/domains/domain1/ and glassfish/lib/ directories,"
      print "    along with the JVM-specified files.dir location, and running"
      print "       this installer as the user who will launch Glassfish."
      print "####################################################################\n"
      ret = raw_input("hit return to continue (or ctrl-C to exit the installer)")

# 0d. the following 3 options can also be specified on the command line, and
# also take precedence over the default values that are hard-coded and/or
# provided in the default.config file:

if adminEmail != "":
   config.set('system', 'ADMIN_EMAIL', adminEmail)

if mailServer != "":
   config.set('system', 'MAIL_SERVER', mailServer)

if gfDir != "":
   config.set('glassfish', 'GLASSFISH_DIRECTORY', gfDir)

# 0e. current working directory:
# @todo - do we need it still?

# 1. CHECK FOR SOME MANDATORY COMPONENTS (war file, etc.)                                                         
# since we can't do anything without these things in place, better check for                                      
# them before we go into the interactive config mode.                                                             
# (skip if this is a database-only setup)

if not pgOnly:
   print "Checking for required components..."
   # 1a. check to see if warfile is available
   warfile = "dataverse.war"
   warfileVersion = None
   if not os.path.isfile(warfile):
      # get dataverse version from pom.xml
      tree = ET.ElementTree(file='../../pom.xml')
      for elem in tree.iter("*"):
         if elem.tag == '{http://maven.apache.org/POM/4.0.0}version':
            warfileVersion = elem.text
         # only want the first, the rest are dependencies
            break
      # now check for version-ed warfile, or bail
      warfile = '../../target/dataverse-' + warfileVersion + '.war'
      if not os.path.isfile(warfile):
         sys.exit("Sorry, I can't seem to find an appropriate warfile.\nAre you running the installer from the right directory?")
   print warfile+" available to deploy. Good."

   # 1b. check for reference_data.sql
   referenceData = '../database/reference_data.sql'
   if not os.path.isfile(referenceData):
      # if it's not there, then we're probably running out of the 
      # unzipped installer bundle, so it should be right here in the current directory:
      referenceData = 'reference_data.sql'
      if not os.path.isfile(referenceData):
         sys.exit("Can't find reference_data.sql!\nAre you running the installer from the right directory?")

   print "found "+referenceData+"... good"

   # 1c. check if jq is available
   # (but we're only doing it if it's not that weird "pod name" mode)
   if podName != "start-glassfish":
      print "Checking if jq is available..."
      try:
         subprocess.call(["jq", "--version"])
         print "good!"
      except:
         sys.exit("Can't find the jq utility in my path. Is it installed?")

   # 1d. check java version
   java_version = subprocess.check_output(["java", "-version"], stderr=subprocess.STDOUT)
   print "Found java version "+java_version
   if not re.search("1.8", java_version):
      sys.exit("Dataverse requires Java 1.8. Please install it, or make sure it's in your PATH, and try again")

   # 1e. check if the setup scripts - setup-all.sh, are available as well, maybe?
   # @todo (?)

# 2. INTERACTIVE CONFIG SECTION

# 2a. read the input prompts and help messages for the interactive mode:
interactiveConfig = SafeConfigParser()
interactiveConfig.read("interactive.config")

# 2b. run the interactive dialog:
if nonInteractive:
   print "non-interactive mode - skipping the config dialog"
else:
   print "\nWelcome to the Dataverse installer.\n"

   if pgOnly:
      print "You will be guided through the process of configuring your"
      print "PostgreSQL database for use by the Dataverse application."
   else:
      print "You will be guided through the process of setting up a NEW"
      print "instance of the dataverse application"

   print
   print "Please enter the following configuration values:"
   print "(hit [RETURN] to accept the default value)"
   print

   dialogDone = False

   while not dialogDone:
      for section in configSections:
         for option in config.options(section):
            configPrompt = interactiveConfig.get('prompts', option)

            # empty config prompt means 
            # this option is not part of the interactive config:
            if configPrompt != "":
               configHelp = interactiveConfig.get('comments', option)
               configHelp = re.sub('\\\\n', "\n", configHelp)
               if configHelp == "":
                  promptLine = configPrompt + ": [" + config.get(section, option) + "] "
               else:
                  promptLine = configPrompt + configHelp + "[" + config.get(section, option) + "] "
                  
               userInput = raw_input(promptLine)
            
               if userInput != "":
                  config.set(section, option, userInput)

               # @todo: for certain specific options, we want to do validation in real time:
               # (replicate from the old installer)

               print

      # 2b. Verify that they are happy with what they have entered:
      print "\nOK, please confirm what you've entered:\n"
      for section in configSections:
         for option in config.options(section):
            if interactiveConfig.get('prompts', option) != "":
               print interactiveConfig.get('prompts', option) + ": " + config.get(section, option)

      yesno = raw_input("\nIs this correct? [y/n] ")

      while ( yesno != 'y' and yesno != 'n' ):
         yesnoPrompt = "Please enter 'y' or 'n'!\n(or ctrl-C to exit the installer)\n"
         yesno = raw_input(yesnoPrompt)

      if yesno == 'y':
         dialogDone = True
      
# 2c. initialize configuration variables from what we've gathered in the config dict: (for convenience)

# database settings/credentials:
pgAdminPassword = config.get('database', 'POSTGRES_ADMIN_PASSWORD')
pgDb = config.get('database', 'POSTGRES_DATABASE')
pgHost = config.get('database', 'POSTGRES_SERVER')
pgPassword = config.get('database', 'POSTGRES_PASSWORD')
pgUser = config.get('database', 'POSTGRES_USER')
# glassfish settings:
hostName = config.get('glassfish', 'HOST_DNS_ADDRESS')
gfDir = config.get('glassfish', 'GLASSFISH_DIRECTORY')
gfUser = config.get('glassfish', 'GLASSFISH_USER')
gfConfig = gfDir+"/glassfish/domains/domain1/config/domain.xml"
gfConfigDir = gfDir+"/glassfish/domains/domain1/config"
gfHeap = config.get('glassfish', 'GLASSFISH_HEAP')
gfAdminUser = config.get('glassfish', 'GLASSFISH_ADMIN_USER')
gfAdminPassword = config.get('glassfish', 'GLASSFISH_ADMIN_PASSWORD')
gfDomain = "domain1"
gfJarPath = gfDir+"/glassfish/modules"
# system settings:
adminEmail = config.get('system', 'ADMIN_EMAIL')
solrLocation = config.get('system', 'SOLR_LOCATION')

# 3. SET UP POSTGRES USER AND DATABASE

if podName != "start-glassfish" and podName != "dataverse-glassfish-0" and not skipDatabaseSetup:
   print "performing database setup"

   # 3a. can we connect as the postgres admin user?
   admin_conn_string = "dbname='postgres' user='postgres' password='"+pgAdminPassword+"' host='"+pgHost+"'"

   try:
      conn = psycopg2.connect(admin_conn_string)
      print "Admin database connectivity succeeds."
   except:
      print "Can't connect to PostgresQL as the admin user.\n"
      sys.exit("Is the server running, have you adjusted pg_hba.conf, etc?")

   # 3b. get the Postgres version (do we need it still?)
   try:
      pg_full_version = conn.server_version
      print "PostgresQL version: "+str(pg_full_version)
   except:
      print "Warning: Couldn't determine PostgresQL version."
   conn.close()

   # 3c. create role:

   conn_cmd = "CREATE ROLE "+pgUser+" PASSWORD '"+pgPassword+"' NOSUPERUSER CREATEDB CREATEROLE INHERIT LOGIN;"
   conn = psycopg2.connect(admin_conn_string)
   conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
   cur = conn.cursor()
   try:
      cur.execute(conn_cmd)
   except:
      print "Looks like the user already exists. Continuing."

   # 3d. create database:

   conn_cmd = "CREATE DATABASE "+pgDb+" OWNER "+pgUser+";"
   try:
      cur.execute(conn_cmd)
   except:
      if force:
         print "WARNING: failed to create the database - continuing, since the --force option was specified"
      else:
         sys.exit("Couldn't create database or database already exists.\n")

   conn_cmd = "GRANT ALL PRIVILEGES on DATABASE "+pgDb+" to "+pgUser+";"
   try:
      cur.execute(conn_cmd)
   except:
      sys.exit("Couldn't grant privileges on "+pgDb+" to "+pgUser)
   cur.close()
   conn.close()

   print "Database and role created!"
   if pgOnly:
      print "postgres-only setup complete."
      sys.exit()

# 4. CONFIGURE GLASSFISH

# 4a. Glassfish heap size - let's make it 1/2 of system memory
# @todo: should we skip doing this in the non-interactive mode? (and just use the value from the config file?)
if myOS == "MacOSX":
   gfHeap = int(macosRAM() / 2)
else:
   # linux
   gfHeap = int(linuxRAM() / 2)
print "Setting Glassfish heap size (Xmx) to "+str(gfHeap)+" Megabytes"
config.set('glassfish','GLASSFISH_HEAP', str(gfHeap))

# 4b. PostgresQL driver:
pg_driver_jarpath = "pgdriver/"+pgJdbcDriver

try:
   copy2(pg_driver_jarpath, gfJarPath)
   print "Copied "+pgJdbcDriver+" into "+gfJarPath
except:
   print "Couldn't copy "+pgJdbcDriver+" into "+gfJarPath+". Check its permissions?"


# 4c. create glassfish admin credentials file

userHomeDir = pwd.getpwuid(os.getuid())[5]
gfClientDir = userHomeDir+"/.gfclient"
gfClientFile = gfClientDir+"/pass"

print "using glassfish client file: " + gfClientFile

# mkdir gfClientDir
if not os.path.isdir(gfClientDir):
   try:
      os.mkdir([gfClientDir,0700])
   except:
      print "Couldn't create "+gfClientDir+", please check permissions."
 
# write credentials
credstring = "asadmin://"+gfAdminUser+"@localhost:4848"

f = open(gfClientFile, 'w')
try:
   f.write(credstring)
   print "Glassfish admin credentials written to "+gfClientFile+"."
except:
   print "Unable to write Glassfish admin credentials. Subsequent commands will likely fail."
f.close

# 4d. check if glassfish is running, attempt to start if necessary
asadmincmd = gfDir +"/bin/asadmin"
domain_status = subprocess.check_output([asadmincmd, "list-domains"], stderr=subprocess.STDOUT)
if re.match(gfDomain+" not running", domain_status):
   print "Looks like Glassfish isn't running. Attempting to start it..."
   subprocess.call([asadmincmd, "start-domain"], stderr=subprocess.STDOUT)
   # now check again or bail
   print "Checking to be sure "+gfDomain+" is running."
   domain_status = subprocess.check_output([asadmincmd, "list-domains"], stderr=subprocess.STDOUT)
   if not re.match(gfDomain+" running", domain_status):
      sys.exit("There was a problem starting Glassfish. Please ensure that it's running, or that the installer can launch it.")

# 4e. check if asadmin login works
#gf_adminpass_status = subprocess.check_output([asadmincmd, "login", "--user="+gfAdminUser, "--passwordfile "+gfClientFile])
gfAdminLoginStatus = subprocess.call([asadmincmd, "login", "--user="+gfAdminUser])

#print "asadmin login output: "+gfAdminLoginStatus
print "asadmin login return code: "+str(gfAdminLoginStatus)

# 4f. configure glassfish by running the standalone shell script that executes the asadmin commands as needed.

print "Note: some asadmin commands will fail, and that's ok. Existing settings can't be created; new settings can't be cleared beforehand."

if not runAsadminScript(config):
   sys.exit("Glassfish configuration script failed to execute properly; aborting.")

# 4g. Additional config files:

jhoveConfig = "jhove.conf"
jhoveConfigSchema = "jhoveConfig.xsd"

jhoveConfigDist = jhoveConfig
jhoveConfigSchemaDist = jhoveConfigSchema

# (if the installer is being run NOT as part of a distribution zipped bundle, but                                
# from inside the source tree - adjust the locations of the jhove config files:                                  

if not os.path.exists(jhoveConfigDist):
   jhoveConfigDist = "../../conf/jhove/" + jhoveConfig
   jhoveConfigSchemaDist = "../../conf/jhove/" + jhoveConfigSchema

# but if we can't find the files in either location, it must mean                                                
# that they are not running the script in the correct directory - so                                             
# nothing else left for us to do but give up:                                                                    

if not os.path.exists(jhoveConfigDist) or not os.path.exists(jhoveConfigSchemaDist):
   sys.exit("Jhove config files not found; aborting. (are you running the installer in the right directory?)")

print "\nInstalling additional configuration files (Jhove)... "
try: 
   copy2(jhoveConfigDist, gfConfigDir)
   copy2(jhoveConfigSchemaDist, gfConfigDir)
   print "done."
except: 
   sys.exit("Failed to copy Jhove config files into the domain config dir. (check permissions?)")

# @todo: The JHOVE conf file has an absolute PATH of the JHOVE config schema file (uh, yeah...)
# - so it may need to be readjusted, if glassfish lives somewhere other than /usr/local/glassfish4
# (replicate from the old installer)

# 5. Deploy the application: 

print "Deploying the application ("+warfile+")"
returnCode = subprocess.call([asadmincmd, "deploy", warfile])
if returnCode != 0:
   sys.exit("Failed to deploy the application!")
# @todo: restart/try to deploy again if it failed?
# @todo: if asadmin deploy says it was successful, verify that the application is running... if not - repeat the above?

# 6. Import reference data
print "importing reference data..."
# open the new postgresQL connection (as the application user):
conn_string="dbname='"+pgDb+"' user='"+pgUser+"' password='"+pgPassword+"' host='"+pgHost+"'"
conn = psycopg2.connect(conn_string)
conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
cur = conn.cursor()
try:
   cur.execute(open(referenceData, "r").read())
   print "done."
except: 
   print "WARNING: failed to import reference data!"

cur.close()
conn.close()

# 7. RUN SETUP SCRIPTS AND CONFIGURE EXTRA SETTINGS
# (note that we may need to change directories, depending on whether this is a dev., or release installer)
# 7a. run setup scripts
print "Running post-deployment setup script (setup-all.sh)"
if not os.path.exists("setup-all.sh") or not os.path.isdir("data"):
   os.chdir("../api")

# try again:
if not os.path.exists("setup-all.sh") or not os.path.isdir("data"):
   sys.exit("Can't find the api setup scripts; aborting. (are you running the installer in the right directory?)")

# @todo: instead of dumping the output of the script on screen, save it in 
# a log file, like we do in the old installer.
try:
   subprocess.call(["./setup-all.sh"])
except:
   sys.exit("Failure to execute setup-all.sh! aborting.")

# 7b. configure admin email in the application settings
print "configuring system email address..."
returnCode = subprocess.call(["curl", "-X", "PUT", "-d", adminEmail, apiUrl+"/admin/settings/:SystemEmail"])
if returnCode != 0:
   print("\nWARNING: failed to configure the admin email in the Dataverse settings!")
else:
   print "\ndone."

# 8c. configure remote Solr location, if specified
if solrLocation != "LOCAL":
   print "configuring remote Solr location... ("+solrLocation+")"
   returnCode = subprocess.call(["curl", "-X", "PUT", "-d", solrLocation, apiUrl+"/admin/settings/:SolrHostColonPort"])
   if returnCode != 0:
      print("\nWARNING: failed to configure the remote Solr location in the Dataverse settings!")
   else:
      print "\ndone."

# 9. DECLARE VICTORY
# ... and give some additional information to the user

print "\n\nYou should now have a running Dataverse instance at"
print "  http://" + hostName + ":8080\n\n"

# DataCite instructions: 

print "\nYour Dataverse has been configured to use DataCite, to register DOI global identifiers in the "
print "test name space \"10.5072\" with the \"shoulder\" \"FK2\""
print "However, you have to contact DataCite (support\@datacite.org) and request a test account, before you "
print "can publish datasets. Once you receive the account name and password, add them to your domain.xml,"
print "as the following two JVM options:"
print "\t<jvm-options>-Ddoi.username=...</jvm-options>"
print "\t<jvm-options>-Ddoi.password=...</jvm-options>"
print "and restart glassfish"
print "If this is a production Dataverse and you are planning to register datasets as "
print "\"real\", non-test DOIs or Handles, consult the \"Persistent Identifiers and Publishing Datasets\""
print "section of the Installataion guide, on how to configure your Dataverse with the proper registration"
print "credentials.\n"

# Warning for the developers about deployment: 

if warfileVersion is not None:
   print "IMPORTANT!"
   print "If this is a personal development installation, we recommend that you undeploy the currently-running copy"
   print "of the application, with the following asadmin command:\n"
   print "\t" + gfDir + '/bin/asadmin undeploy dataverse-' + warfileVersion + "\n"
   print "before attempting to deploy from your development environment in NetBeans.\n"

sys.exit()

# 10. (OPTIONALLY?) CHECK THE RSERVE SETUP
# @todo
# (it's disabled in the current version of the old installer)

