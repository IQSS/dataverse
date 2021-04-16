# Dataverse backup, ssh io module

import sys
import io
import paramiko
import os
import re
from config import (ConfigSectionMap)

my_ssh_client = None

def open_ssh_client():
    ssh_host = ConfigSectionMap("Backup")['sshhost']
    ssh_port = ConfigSectionMap("Backup")['sshport']
    ssh_username = ConfigSectionMap("Backup")['sshusername']

    print "SSH Host: %s" % (ssh_host)
    print "SSH Port: %s" % (ssh_port)
    print "SSH Username: %s" % (ssh_username)


    ssh_client=paramiko.SSHClient()
    ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh_client.connect(hostname=ssh_host,username=ssh_username)

    print "Connected!"

    return ssh_client

# Transfers the file "local_flo" over ssh/sftp to the configured remote server.
# local_flo can be either a string specifying the file path, or a file-like object (stream).
# Note that if a stream is supplied, the method also needs the file size to be specified, 
# via the parameter byte_size.
def transfer_file(local_flo, dataset_authority, dataset_identifier, storage_identifier, byte_size):
    sftp_client=my_ssh_client.open_sftp()

    remote_dir = dataset_authority + "/" + dataset_identifier

    subdirs = remote_dir.split("/")

    cdir = ConfigSectionMap("Backup")['backupdirectory'] + "/"
    for subdir in subdirs:
        try:
            cdir = cdir + subdir + "/"
            sftpattr=sftp_client.stat(cdir)
        except IOError:
            #print "directory "+cdir+" does not exist (creating)"
            sftp_client.mkdir(cdir)
        #else:
        #    print "directory "+cdir+" already exists"

    m = re.search('^([a-z0-9]*)://(.*)$', storage_identifier)
    if m is not None:
        storageTag = m.group(1)
        storage_identifier = re.sub('^.*:', '', storage_identifier)

    remote_file = cdir + storage_identifier

    if (type(local_flo) is str):
        sftp_client.put(local_flo,remote_file)
    else:
        # assume it's a stream:
        # sftp_client.putfo() is convenient, but appears to be unavailable in older 
        # versions of paramiko; so we'll be using .read() and .write() instead:
        #sftp_client.putfo(local_flo,remote_file,byte_size)
        sftp_stream = sftp_client.open(remote_file,"wb")
        while True:
            buffer = local_flo.read(32*1024)
            if len(buffer) == 0:
                break;
            sftp_stream.write (buffer)
        sftp_stream.close()

    sftp_client.close()

    print "File transfered."

    return remote_file

def verify_remote_file(remote_file, checksum_type, checksum_value):
    try: 
        stdin,stdout,stderr=my_ssh_client.exec_command("ls "+remote_file)
        remote_file_checked = stdout.readlines()[0].rstrip("\n\r")
    except:
        raise ValueError("remote file check failed (" + remote_file + ")")

    if (remote_file != remote_file_checked):
        raise ValueError("remote file NOT FOUND! (" + remote_file_checked + ")")

    if (checksum_type == "MD5"):
        remote_command = "md5sum"
    elif (checksum_type == "SHA1"):
        remote_command = "sha1sum"
        
    try: 
        stdin,stdout,stderr=my_ssh_client.exec_command(remote_command+" "+remote_file)
        remote_checksum_value = (stdout.readlines()[0]).split(" ")[0]
    except: 
        raise ValueError("remote checksum check failed (" + remote_file + ")")

    if (checksum_value != remote_checksum_value):
        raise ValueError("remote checksum BAD! (" + remote_checksum_value + ")")


def backup_file_ssh(file_input, dataset_authority, dataset_identifier, storage_identifier, checksum_type, checksum_value, byte_size=0):
    global my_ssh_client
    if (my_ssh_client is None):
        my_ssh_client = open_ssh_client()
        print "ssh client is not defined"
    else:
        print "reusing the existing ssh client"

    try:
        file_transfered = transfer_file(file_input, dataset_authority, dataset_identifier, storage_identifier, byte_size)
    except:
        raise ValueError("failed to transfer file")

    verify_remote_file(file_transfered, checksum_type, checksum_value)

def main():

    print "entering ssh (standalone mode)"


    print "testing local file:"
    try:
        file_path="config.ini"
        backup_file_ssh("config.ini", "1902.1", "XYZ", "config.ini", "MD5", "8e6995806b1cf27df47c5900869fdd27")
    except ValueError:
        print "failed to verify file (\"config.ini\")"
    else:
        print "file ok"

    print "testing file stream:"
    try: 
        file_size = os.stat(file_path).st_size
        print ("file size: %d" % file_size)
        file_stream = io.open("config.ini", "rb")
        backup_file_ssh(file_stream, "1902.1", "XYZ", "config.ini", "MD5", "8e6995806b1cf27df47c5900869fdd27", file_size)
    except ValueError:
        print "failed to verify file (\"config.ini\")"
    else:
        print "file ok"


if __name__ == "__main__":
    main()


