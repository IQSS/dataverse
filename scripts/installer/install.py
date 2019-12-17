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
import subprocess # for java / asadmin commands
import getopt
import sys
import pwd
from tempfile import mkstemp
import xml.etree.cElementTree as ET

# process command line arguments: 

shortOptions = "vyf"
longOptions = ["verbose", 
               "yes", 
               "noninteractive", 
               "pg_only", 
               "skip_db_setup", 
               "nogfpasswd", 
               "hostname=", 
               "gfuser=", 
               "gfdir=", 
               "mailserver=", 
               "admin_email="]

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
      print ("enabling \"force\" mode - will continue if the database specified already exists!")
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
config.read("default.config")

# expected dataverse defaults
apiUrl = "http://localhost:8080/api"

# enumerate postgres drivers (no longer necessary!)
# there's now a single driver that works for all supported versions:
# jodbc.postgresql.org recommends 4.2 for Java 8.
# updated drivers may be obtained from
#  https://jdbc.postgresql.org/download.html
pgJdbcDriver = "postgresql-42.2.2.jar"

# 0. A few preliminary checks:                                                                                   
# 0a. OS: @todo (assuming linux)

# 0b. host name:
# the name entered on the command line as --hostname overrides other methods:
if hostName is not "":
   config.set('glassfish', 'HOST_DNS_ADDRESS', hostName)
else:
   try:
      hostNameFromCommand = subprocess.check_output(["hostname"]).rstrip()
      config.set('glassfish', 'HOST_DNS_ADDRESS', hostNameFromCommand)
   except:
      print "Warning! Failed to execute \"hostname\"; assuming \"" + config.get('glassfish', 'HOST_DNS_ADDRESS') + "\""

# 0c. current OS user. 
# try to get the current username from the environment; the first one we find wins:
currentUser = os.environ.get('LOGNAME')
if currentUser is None or currentUser is '':
   currentUser = os.environ.get('USER')
if currentUser is None or currentUser is '':
   currentUser = pwd.getpwuid(os.getuid())[0]
# we may need this username later (when we decide if we need to sudo to run asadmin?) 

# if the username was specified on the command line, it takes precendece:
if gfUser is not "":
   config.set('glassfish', 'GLASSFISH_USER', gfUser)
   # @todo: check if the glassfish user specified actually exists
   # (replicate from the old installer)
else:
   # use the username from the environment that we just found, above: 
   config.set('glassfish', 'GLASSFISH_USER', currentUser)

# @todo: warn the user that runnning the installer as root is not recommended, if that's what they are doing
# (replicate from the old installer)

# 0d. the following 3 options can also be specified on the command line, and                                      
# also take precedence over the default values that are hard-coded and/or                                         
# provided in the default.config file:                                                                            

if adminEmail is not "":
   config.set('system', 'ADMIN_EMAIL', adminEmail)

if mailServer is not "":
   config.set('system', 'MAIL_SERVER', mailServer)

if gfDir is not "":
   config.set('glassfish', 'GLASSFISH_DIRECTORY', gfDir)

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

   # 1b. current working directory:
   # @todo (move it under 0. as well?)

   # 1c. check for reference-data.sql
   if not os.path.isfile('../database/reference_data.sql'):
      sys.exit("WARNING: Can't find .sql data template!\nAre you running the installer from the right directory?")
   else:
      print "found ../database/reference_data.sql... good"
   # @todo: add checks for the location of the sql file in the release bundle. 

   # 1d. check for existence of jq
   # (but we're only doing it if it's not that weird "pod name" mode)
   if podName is not "start-glassfish":
      print "Checking if jq is available..."
      try:
         subprocess.call(["jq", "--version"])
         print "good!"
      except:
         sys.exit("Can't find the jq utility in my path. Is it installed?")

   # 1e. check java version
   java_version = subprocess.check_output(["java", "-version"], stderr=subprocess.STDOUT)
   print "Found java version "+java_version
   if not re.search("1.8", java_version):
      sys.exit("Dataverse requires Java 1.8. Please install it, or make sure it's in your PATH, and try again")

   # 1f. check if the setup scripts - setup-all.sh, are available as well, maybe?
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
            if configPrompt is not '':
               configHelp = interactiveConfig.get('comments', option)
               configHelp = re.sub('\\\\n', "\n", configHelp)
               if configHelp is '':
                  promptLine = configPrompt + ": [" + config.get(section, option) + "] "
               else:
                  promptLine = configPrompt + configHelp + "[" + config.get(section, option) + "] "
                  
               userInput = raw_input(promptLine)
            
               if userInput is not '':
                  config.set(section, option, userInput)

               # @todo: for certain specific options, we want to do validation in real time:
               # (replicate from the old installer)

               print

      # 2b. Verify that they are happy with what they have entered:
      print "\nOK, please confirm what you've entered:\n"
      for section in configSections:
         for option in config.options(section):
            if interactiveConfig.get('prompts', option) is not '':
               print interactiveConfig.get('prompts', option) + ": " + config.get(section, option)

      yesno = raw_input("\nIs this correct? [y/n] ")

      while ( yesno is not 'y' and yesno is not 'n' ):
         yesnoPrompt = "Please enter 'y' or 'n'!\n(or ctrl-C to exit the installer)\n"
         yesno = raw_input(yesnoPrompt)

      if yesno is 'y':
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

if podName is not "start-glassfish" and podName is not "dataverse-glassfish-0" and not skipDatabaseSetup:
   print "performing database setup"
   # 3a. locate psql (@todo:)
   # (is it still needed though? can we import reference data without it?)

   # 3b. can we connect as the postgres admin user?
   admin_conn_string = "dbname='postgres' user='postgres' password='"+pgAdminPassword+"' host='"+pgHost+"'"

   try:
      conn = psycopg2.connect(admin_conn_string)
      print "Admin database connectivity succeeds."
   except:
      print "Can't connect to PostgresQL as the admin user.\n"
      sys.exit("Is the server running, have you adjusted pg_hba.conf, etc?")

   # 3c. get the Postgres version (do we need it still?)
   try:
      pg_full_version = conn.server_version
      print "PostgresQL version: "+str(pg_full_version)
   except:
      print "Warning: Couldn't determine PostgresQL version."
   conn.close()

   # 3d. create role:

   conn_cmd = "CREATE ROLE "+pgUser+" PASSWORD '"+pgPassword+"' NOSUPERUSER CREATEDB CREATEROLE INHERIT LOGIN;"
   conn = psycopg2.connect(admin_conn_string)
   conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
   cur = conn.cursor()
   try:
      cur.execute(conn_cmd)
   except:
      print "Looks like the user already exists. Continuing."

   # 3e. create database:

   conn_cmd = "CREATE DATABASE "+pgDb+" OWNER "+pgUser+";"
   try:
      cur.execute(conn_cmd)
   except:
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

# ----- a few useful functions ----- #

# for editing lines in domain.xml
# (we really don't want to edit domain.xml - we want to use asadmin for all glassfish tinkering though...)
def replaceLine(file, pattern, subst):
   fh, tempfile = mkstemp()
   with fdopen(fh,'w') as new_file:
      with open(file) as orig_file:
         print file
         for line in orig_file:
            new_file.write(line.replace(pattern, subst))
   backup = file+".bak"
   copy2(file, backup)
   move(tempfile, file)

# determine free system memory (Linux for now)
def linuxRAM():
        totalMemory = os.popen("free -m").readlines()[1].split()[1]
        return int(totalMemory)

# 4. CONFIGURE GLASSFISH

# 4a. Glassfish heap size - let's make it 1/2 of system memory
#totalRAM = linuxRAM()
#gfHeap = int( totalRAM / 2)
#gf_xmx = "Xmx"+str(gfHeap)

#try:
#   replaceLine(gfConfig, "Xmx512", gf_xmx)
#   print "Setting JVM heap to "+str(gfHeap)+". You may want to increase this to suit your system."
#except:
#   print "Unable to adjust JVM heap size. Please check file permissions, etc?"

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
gfAdminLoginStatus = subprocess.check_output([asadmincmd, "login", "--user="+gfAdminUser])

print "asadmin login output: "+gfAdminLoginStatus

# 4f. configure glassfish by running the standalone shell script that executes the asadmin commands as needed.

print "Note: some asadmin commands will fail, and that's ok. Existing settings can't be created; new settings can't be cleared beforehand."

def runAsadminScript(config):
   # We are going to run a standalone shell script with a bunch of asadmin                                      
   # commands to set up all the glassfish components for the application.                                       
   # All the parameters must be passed to that script as environmental                                          
   # variables:
   os.environ['GLASSFISH_DOMAIN'] = "domain1";
   os.environ['ASADMIN_OPTS'] = "";

   os.environ['HOST_ADDRESS'] = config.get('glassfish','HOST_DNS_ADDRESS')
   os.environ['GLASSFISH_ROOT'] = config.get('glassfish','GLASSFISH_DIRECTORY')
   os.environ['MEM_HEAP_SIZE'] = config.get('glassfish','GLASSFISH_HEAP')
	
   os.environ['DB_PORT'] = config.get('database','POSTGRES_PORT')
   os.environ['DB_HOST'] = config.get('database','POSTGRES_SERVER')
   os.environ['DB_NAME'] = config.get('database','POSTGRES_DATABASE')
   os.environ['DB_USER'] = config.get('database','POSTGRES_USER')
   os.environ['DB_PASS'] = config.get('database','POSTGRES_PASSWORD')
   
   os.environ['RSERVE_HOST'] = config.get('rserve','RSERVE_HOST')
   os.environ['RSERVE_PORT'] = config.get('rserve','RSERVE_PORT')
   os.environ['RSERVE_USER'] = config.get('rserve','RSERVE_USER')
   os.environ['RSERVE_PASS'] = config.get('rserve','RSERVE_PASSWORD')

   os.environ['DOI_BASEURL'] = config.get('doi','DOI_BASEURL')
   os.environ['DOI_USERNAME'] = config.get('doi','DOI_USERNAME')
   os.environ['DOI_PASSWORD'] = config.get('doi','DOI_PASSWORD')
   os.environ['DOI_MDCBASEURL'] = config.get('doi','DOI_MDCBASEURL')

   mailServerEntry = config.get('system','MAIL_SERVER')
   print "mailserver: "+mailServerEntry
   try:
      mailServerHost, mailServerPort = config.get('system','MAIL_SERVER').split(":",1)
      os.environ['SMTP_SERVER'] = mailServerHost
      os.environ['SMTP_SERVER_PORT'] = mailServerPort
   except:
      mailServerHost = config.get('system','MAIL_SERVER')
      os.environ['SMTP_SERVER'] = mailServerHost

   os.environ['FILES_DIR'] = config.get('glassfish','GLASSFISH_DIRECTORY') + "/glassfish/domains/domain1/files"

   # run glassfish setup script:
#   glassfishSetupOutput = subprocess.check_output(["./glassfish-setup.sh"], stderr=subprocess.STDOUT)
#   glassfishSetupOutput = subprocess.check_output(["./glassfish-setup.sh"])

#   print glassfishSetupOutput

   try:
      returncode = subprocess.call(["./glassfish-setup.sh"])
      if returncode != 0:
         return False
   except:
      return False

   return True


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

print "\nCopying additional configuration files... "
try: 
   copy2(jhoveConfigDist, gfConfigDir)
   copy2(jhoveConfigSchemaDist, gfConfigDir)
   print "done."
except: 
   sys.exit("Failed to copy Jhove config files into the domain config dir.")

# 5. Deploy the application: 

try:
   subprocess.call([asadmincmd, "deploy", warfile])
   print "deployed the application"
except:
   sys.exit("Failed to deploy the application! WAR file: " + warfile)

# 6. Import reference data
# @todo

# 7. CHECK IF THE APPLICATION IS RUNNING
# (try restarting glassfish if not?)
# @todo

# 8. RUN SETUP SCRIPTS AND CONFIGURE EXTRA SETTINGS
# (note that we may need to change directories, depending on whether this is a dev., or release installer)
# @todo
# 8a. run setup scripts
if not os.path.exists("setup-all.sh") or not os.path.isdir("data"):
   os.chdir("../api")

# try again:
if not os.path.exists("setup-all.sh") or not os.path.isdir("data"):
   sys.exit("Can't find the api setup scripts; aborting. (are you running the installer in the right directory?)")

try:
   subprocess.call(["./setup-all.sh"])
except:
   sys.exit("Failure to execute setup-all.sh! aborting.")

# 8b. configure admin email in the application settings
try:
   subprocess.call(["curl", "-X", "PUT", "-d", adminEmail, apiUrl+"/admin/settings/:SystemEmail"])
except:
   #sys.exit("failed to configure the admin email in the Dataverse settings!")
   print("WARNING: failed to configure the admin email in the Dataverse settings!")

# 8c. configure remote Solr location, if specified
if solrLocation is not "LOCAL":
   try:
      subprocess.call(["curl", "-X", "PUT", "-d", solrLocation, apiUrl+"/admin/settings/:SolrHostColonPort"])
   except:
      #sys.exit("failed to configure the remote Solr location in the Dataverse settings!")
      print("WARNING: failed to configure the remote Solr location in the Dataverse settings!")

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
# disabled in the current version of the old installer

