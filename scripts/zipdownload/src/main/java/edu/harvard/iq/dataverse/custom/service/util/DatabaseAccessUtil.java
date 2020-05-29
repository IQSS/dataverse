/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.custom.service.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for directly accessing the Dataverse database to extract 
 * the file locations and parameters for the zipping jobs. 
 * 
 * @author Leonid Andreev
 */
public class DatabaseAccessUtil implements java.io.Serializable  {

    // The zipper needs to make one database call to initiate each job.
    // So the database connection can be closed immediately.

    public static List<String []> lookupZipJob(String jobKey) {
        Connection c = connectToDatabase();
        
        if (c == null) {
            // no connection - no data, return null queitly
            return null; 
        }
        
        Statement stmt; 
        ResultSet rs; 
        
        List<String[]> ret = new ArrayList<>();
        
        try {
            c.setAutoCommit(false);

            stmt = c.createStatement();
            rs = stmt.executeQuery( "SELECT * FROM CustomZipServiceRequest WHERE key='" + jobKey +"';" );
            
            while ( rs.next() ) {
                String storageLocation = rs.getString("storageLocation");
                String  fileName = rs.getString("fileName");
                
                //System.out.println( "storageLocation = " + storageLocation );
                //System.out.println( "fileName = " + fileName );
                
                String[] entry = new String[2];
                entry[0] = storageLocation;
                entry[1] = fileName;
                
                ret.add(entry);
            }
            rs.close();
            stmt.close();            
        } catch (Exception e) {
            System.err.println( "Database error: " + e.getClass().getName()+" "+ e.getMessage() );
            // return null (but close the connection first):
            try {
                c.close();
            } catch (Exception ex) {}
            return null;
        }
        
        // Delete all the entries associated with the job, now that we are done
        // with it. 
        // Alternatively, the db user whose credentials the zipper is using
        // may be given only read-only access to the table; and it could be the 
        // job of the Dataverse application to, say, automatically delete all the 
        // entries older than 5 min. every time it accesses the table on its side.
        
        /*try {
            stmt = c.createStatement();
            stmt.executeUpdate("DELETE FROM CustomZipServiceRequest WHERE key='" + jobKey +"';");
            c.commit();
        } catch (Exception e) {
            // Not much we can or want to do, but complain in the Apache logs:
            System.err.println("Failed to delete the job from the db");
        }*/
        
        try {
            c.close();
        } catch (Exception e) {}

        return ret;
    }
    
    // Opens the connection to the database. 
    // Uses the credentials supplied via JVM options
    private static Connection connectToDatabase() {
        Connection c = null;

        String host = System.getProperty("db.serverName") != null ? System.getProperty("db.serverName") : "localhost";
        String port = System.getProperty("db.portNumber") != null ? System.getProperty("db.portNumber") : "5432";
        String database = System.getProperty("db.databaseName") != null ? System.getProperty("db.databaseName") : "dvndb";
        String pguser = System.getProperty("db.user") != null ? System.getProperty("db.user") : "dvnapp";
        String pgpasswd = System.getProperty("db.password") != null ? System.getProperty("db.password") : "secret";

        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database,
                            pguser,
                            pgpasswd);
        } catch (Exception e) {
            //e.printStackTrace();
            //System.err.println(e.getClass().getName()+": "+e.getMessage());
            return null;
        }
        //System.out.println("Opened database successfully");
        return c;
    }
}