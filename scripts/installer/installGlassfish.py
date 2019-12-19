import os
import subprocess

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

   try:
      mailServerHost, mailServerPort = config.get('system','MAIL_SERVER').split(":",1)
      os.environ['SMTP_SERVER'] = mailServerHost
      os.environ['SMTP_SERVER_PORT'] = mailServerPort
   except:
      mailServerHost = config.get('system','MAIL_SERVER')
      os.environ['SMTP_SERVER'] = mailServerHost

   os.environ['FILES_DIR'] = config.get('glassfish','GLASSFISH_DIRECTORY') + "/glassfish/domains/domain1/files"

   # run glassfish setup script:

   print "running glassfish configuration script (glassfish-setup.sh)"
   returncode = subprocess.call(["./glassfish-setup.sh"])
   if returncode != 0:
      return False

   return True


