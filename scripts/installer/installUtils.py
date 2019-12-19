import subprocess
from os import popen

def checkUser(userName):
    ret = subprocess.call("id "+userName+" > /dev/null 2>&1", shell=True)
    if ret != 0:
        return False

    return True

def linuxRAM():
    totalMemory = os.popen("free -m").readlines()[1].split()[1]
    return int(totalMemory)

def macosRAM():
    totalMemory = subprocess.check_output(["/usr/sbin/sysctl", "-n", "hw.memsize"]).rstrip()
    return int(int(totalMemory) / (1024 * 1024)) # megabytes

