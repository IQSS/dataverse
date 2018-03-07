import psycopg2
import sys
import pprint
from time import (time)
from datetime import (datetime, timedelta)
from config import (ConfigSectionMap)

dataverse_db_connection=None
backup_db_connection=None

def create_database_connection(database='database'):
    Host = ConfigSectionMap("Database")['host']
    Port = ConfigSectionMap("Database")['port']
    Database = ConfigSectionMap("Database")[database]
    Username = ConfigSectionMap("Database")['username']
    Password = ConfigSectionMap("Database")['password']

    #print "Database Host: %s" % (Host)
    #print "Database Port: %s" % (Port)
    #print "Database Name: %s" % (Database)
    #print "Username: %s" % (Username)
    #print "Password: %s" % (Password)

    #Define our connection string                                                                                                                                   
    conn_string = "host='"+Host+"' dbname='"+Database+"' user='"+Username+"' password='"+Password+"'"

    #print "Connecting to database\n->%s" % (conn_string)

    # get a connection, if a connect cannot be made an exception will be raised here
    conn = psycopg2.connect(conn_string)

    #print "Connected!\n"

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

    # Select data files from the database
    # The query below is a bit monstrous, as we try to get all the information about the stored file
    # from multiple tables in the single request. Note the "LEFT JOIN" in it - we want it to return 
    # the "datatable" object referencing this datafile, if such exists, or NULL otherwise. If the 
    # value is not NULL, we know this is a tabular data file.
    dataverse_query="SELECT s.authority, s.identifier, o.storageidentifier, f.checksumtype, f.checksumvalue, f.filesize,o.createdate, datatable.id FROM datafile f LEFT JOIN datatable ON f.id = datatable.datafile_id, dataset s, dvobject o WHERE o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null"
    if sinceTimestamp is None:
        cursor.execute(dataverse_query)
    else:
        dataverse_query = dataverse_query+" AND o.createdate > %s"
        cursor.execute(dataverse_query, (sinceTimestamp,))


    records = cursor.fetchall()

    return records

def get_last_timestamp():
    backup_db_connection = get_backupdb_connection()

    cursor = backup_db_connection.cursor()

    # select the last timestamp from the datafilestatus table: 
    dataverse_query="SELECT createdate FROM datafilestatus ORDER BY createdate DESC LIMIT 1"

    cursor.execute(dataverse_query)

    record = cursor.fetchone()

    if record is None:
        #print "table is empty"
        return None

    #timestamp = record[0] + timedelta(seconds=1)
    timestamp = record[0]
    # milliseconds are important!
    timestamp_str = timestamp.strftime('%Y-%m-%d %H:%M:%S.%f')

    return timestamp_str

def get_datafile_status(dataset_authority, dataset_identifier, storage_identifier):
    backup_db_connection = get_backupdb_connection()
    cursor = backup_db_connection.cursor()

    # select the last timestamp from the datafilestatus table: 
    
    dataverse_query="SELECT status FROM datafilestatus WHERE datasetidentifier=%s AND storageidentifier=%s;"

    dataset_id=dataset_authority+"/"+dataset_identifier

    cursor.execute(dataverse_query, (dataset_id, storage_identifier))

    record = cursor.fetchone()

    if record is None:
        #print "no backup status for this file"
        return None

    backupstatus = record[0]
    #print "last backup status: "+backupstatus
    return backupstatus

def record_datafile_status(dataset_authority, dataset_identifier, storage_identifier, status, createdate):
    current_status = get_datafile_status(dataset_authority, dataset_identifier, storage_identifier)

    backup_db_connection = get_backupdb_connection()
    cursor = backup_db_connection.cursor()

    createdate_str = createdate.strftime('%Y-%m-%d %H:%M:%S.%f')
    nowdate_str = datetime.fromtimestamp(time()).strftime('%Y-%m-%d %H:%M:%S')

    if current_status is None:
        query = "INSERT INTO datafilestatus (status, createdate, lastbackuptime, lastbackupmethod, datasetidentifier, storageidentifier) VALUES (%s, %s, %s, %s, %s, %s);"
    else:
        query = "UPDATE datafilestatus SET status=%s, createdate=%s, lastbackuptime=%s, lastbackupmethod=%s WHERE datasetidentifier=%s AND storageidentifier=%s;"

    dataset_id=dataset_authority+"/"+dataset_identifier
    backup_method = ConfigSectionMap("Backup")['storagetype']

    cursor.execute(query, (status, createdate_str, nowdate_str, backup_method, dataset_id, storage_identifier))

    # finalize transaction:
    backup_db_connection.commit()
    cursor.close()
        



