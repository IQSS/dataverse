import subprocess
from os import popen
import os.path
import socket
import re

def check_user(userName):
    ret = subprocess.call("id "+userName+" > /dev/null 2>&1", shell=True)
    if ret != 0:
        return False

    return True

def linux_ram():
    totalMemory = os.popen("free -m").readlines()[1].split()[1]
    return int(totalMemory)

def macos_ram():
    totalMemory = subprocess.check_output(["/usr/sbin/sysctl", "-n", "hw.memsize"]).rstrip()
    return int(int(totalMemory) / (1024 * 1024)) # megabytes

def validate_admin_email(emailAddress):
    if re.match("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,4}", emailAddress) is not None:
        return True
    return False

def test_glassfish_directory(directory):
    if os.path.isdir(directory+"/glassfish/domains/domain1"):
        return True
    return False

def test_smtp_server(address):
    try:
        smtpHost, smtpPort = address.split(":",1)
    except:
        smtpHost = address
        smtpPort = "25"

    try:
        ip = socket.gethostbyname(smtpHost)
    except:
        print "Failed to look up the ip address of "+smtpHost
        return False

    try: 
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
    except socket.error as err: 
        print "failed to create socket, error %s" %(err) 
        return False
      
    # connecting to the server 
    try:
        s.connect((smtpHost, int(smtpPort))) 
    except socket.error as err:
        print "failed to connect, error %s" %(err)
        return False
  
    return True
    
    

