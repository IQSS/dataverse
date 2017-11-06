import psycopg2
import sys
import pprint
from config import (ConfigSectionMap)

def query_database(sinceTimestamp=None):
    Host = ConfigSectionMap("Database")['host']
    Port = ConfigSectionMap("Database")['port']
    Database = ConfigSectionMap("Database")['database']
    Username = ConfigSectionMap("Database")['username']
    Password = ConfigSectionMap("Database")['password']

    print "Database Host: %s" % (Host)
    print "Database Port: %s" % (Port)
    print "Database Name: %s" % (Database)
    print "Username: %s" % (Username)
    print "Password: %s" % (Password)

    #Define our connection string                                                                                                                                   
    #conn_string = "host='localhost' dbname='dvndb4' user='dvnapp' password='secret'"                                                                               
    conn_string = "host='"+Host+"' dbname='"+Database+"' user='"+Username+"' password='"+Password+"'"

    # print the connection string we will use to connect                                                                                                            
    print "Connecting to database\n->%s" % (conn_string)

    # get a connection, if a connect cannot be made an exception will be raised here                                                                                
    conn = psycopg2.connect(conn_string)

    # conn.cursor will return a cursor object, you can use this cursor to perform queries                                                                           
    cursor = conn.cursor()
    print "Connected!\n"

    # select data files from the database 
    if sinceTimestamp is None:
        cursor.execute("SELECT s.authority,s.identifier,o.storageidentifier FROM dataset s, datafile f, dvobject o WHERE o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null")
    else:
        cursor.execute("SELECT s.authority,s.identifier,o.storageidentifier FROM dataset s, datafile f, dvobject o WHERE o.id = f.id AND o.owner_id = s.id AND s.harvestingclient_id IS null AND o.createdate > '"+sinceTimestamp+"'")

    records = cursor.fetchall()

    return records
 
