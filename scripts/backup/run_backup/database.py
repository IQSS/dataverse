import psycopg2
import sys
import pprint
from config import (ConfigSectionMap)

dataverse_db_connection=None
backup_db_connection=None

def create_database_connection(database='database'):
    Host = ConfigSectionMap("Database")['host']
    Port = ConfigSectionMap("Database")['port']
    Database = ConfigSectionMap("Database")[database]
    Username = ConfigSectionMap("Database")['username']
    Password = ConfigSectionMap("Database")['password']

    print "Database Host: %s" % (Host)
    print "Database Port: %s" % (Port)
    print "Database Name: %s" % (Database)
    print "Username: %s" % (Username)
    print "Password: %s" % (Password)

    #Define our connection string                                                                                                                                   
    conn_string = "host='"+Host+"' dbname='"+Database+"' user='"+Username+"' password='"+Password+"'"

    print "Connecting to database\n->%s" % (conn_string)

    # get a connection, if a connect cannot be made an exception will be raised here
    conn = psycopg2.connect(conn_string)

    return conn

def get_backupdb_connection():
    global backup_db_connection

    if backup_db_connection is None:
        backup_db_connection = create_database_connection('backupdatabase')

    return backup_db_connection

def query_database(sinceTimestamp=None):
    global dataverse_db_connection

    dataverse_db_connection = create_database_connection()

    cursor = dataverse_db_connection.cursor()
    print "Connected!\n"

    # select data files from the database 
    dataverse_query="SELECT s.authority,s.identifier,o.storageidentifier,f.checksumtype,f.checksumvalue,f.filesize,o.createdate FROM dataset s, datafile f, dvobject o WHERE o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null"
    if sinceTimestamp is not None:
        dataverse_query = dataverse_query+" AND o.createdate > '"+sinceTimestamp+"'"

    cursor.execute(dataverse_query)

    records = cursor.fetchall()

    return records

def get_last_timestamp():
    backup_db_connection = get_backupdb_connection()

    cursor = backup_db_connection.cursor()
    print "Connected!\n"

    # select the last timestamp from the datafilestatus table: 
    dataverse_query="SELECT lastbackuptime FROM datafilestatus ORDER BY lastbackuptime DESC LIMIT 1"

    cursor.execute(dataverse_query)

    record = cursor.fetchone()

    if record is None:
        print "table is empty"
        return None

    timestamp = record[0]
    return timestamp

def get_datafile_status(dataset_authority, dataset_identifier, storage_identifier):
    backup_db_connection = get_backupdb_connection()
    cursor = backup_db_connection.cursor()

    # select the last timestamp from the datafilestatus table: 
    dataverse_query="SELECT status FROM datafilestatus WHERE datasetidentifier='"+dataset_authority+"/"+dataset_identifier+"' AND storageidentifier='"+storage_identifier+"';"

    cursor.execute(dataverse_query)

    record = cursor.fetchone()

    if record is None:
        print "no backup status for this file"
        return None

    backupstatus = record[0]
    return backupstatus

def record_datafile_status(dataset_authority, dataset_identifier, storage_identifier, status, timestamp):
    current_status = get_datafile_status(dataset_authority, dataset_identifier, storage_identifier)

    backup_db_connection = get_backupdb_connection()
    cursor = backup_db_connection.cursor()

    timestamp_str = timestamp.strftime('%Y-%m-%d %H:%M:%S')

    if current_status is None:
        uquery = "INSERT INTO datafilestatus (datasetidentifier, storageidentifier, status, lastbackuptime, lastbackupmethod) VALUES ('"+dataset_authority+"/"+dataset_identifier+"', '"+storage_identifier+"', '"+status+"', '"+timestamp_str+"', '" + ConfigSectionMap("Backup")['storagetype'] + "');"
        print uquery
        cursor.execute(uquery)

#    else:
#        uquery = "UPDATE datafilestatus SET status='" + status + "', lastbackuptime=" + timestamp + ", lastbackupmethod='" = ConfigSectionMap("Backup")['storagetype'] + "' WHERE datasetidentifier='"+dataset_authority+"/"+dataset_identifier+"' AND storageidentifier='"+storage_identifier+"';"


