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
import java.sql.PreparedStatement;
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
    
    private static final int JOB_TOKEN_LENGTH = 16;
    // A legitimate token is 16 characters long, and is made up of 
    // hex digits and one dash. THERE ARE prettier ways to spell out 
    // this regular expression - I just wanted it to be clear what it does:
    private static final String JOB_TOKEN_REGEX = "^[0-9a-f][0-9a-f]*\\-[0-9a-f][0-9a-f]*$";
    private static final String JOB_LOOKUP_QUERY = "SELECT * FROM CustomZipServiceRequest WHERE key=?";
    private static final String JOB_DELETE_QUERY = "DELETE FROM CustomZipServiceRequest WHERE key=?";

    public static List<String []> lookupZipJob(String jobKey) {
        // Before we do anything, it is super important to sanitize the 
        // supplied token - we don't want to insert anything sketchy into 
        // the db query below (an "injection attack"). 
        // java.sql PreparedStatement.setString() that we are using below 
        // should also be checking against an attemp to insert a sub-query. 
        // But better safe than sorry. 
        if (!validateTokenFormat(jobKey)) {
            return null; // This will result in a "no such job" response.
        }
        
        Connection c = connectToDatabase();
        
        if (c == null) {
            // no connection - no data, return null queitly
	    //System.out.println("could not connect to the database");
            return null; 
        }
        
        PreparedStatement stmt; 
        ResultSet rs; 
        
        List<String[]> ret = new ArrayList<>();
        
        try {
            c.setAutoCommit(false);

            stmt = c.prepareStatement(JOB_LOOKUP_QUERY);
            stmt.setString(1, jobKey);
            rs = stmt.executeQuery();
            
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
        
        try {
            stmt = c.prepareStatement(JOB_DELETE_QUERY);
            stmt.setString(1, jobKey);
            stmt.executeUpdate();
            c.commit();
        } catch (Exception e) {
            // Not much we can or want to do, but complain in the Apache logs:
            // (not even sure about printing any log messages either; the reason
            // this delete failed may be because the admin chose to only give 
            // the zipper read-only access to the db - in which case this will 
            // be happening every time a job is processed. which in turn is 
            // ok - there is a backup cleanup mechanism for deleting older jobs
            // on the application side as well).
            //System.err.println("Failed to delete the job from the db");
        }
        
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
		.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=allow", //&sslfactory=org.postgresql.ssl.NonValidatingFactory",
                            pguser,
                            pgpasswd);
        } catch (Exception e) {
            return null;
        }
        return c;
    }
    
    private static boolean validateTokenFormat(String jobKey) {
        // A legitimate token is 16 characters long, and is made up of 
        // hex digits and one dash. 
        if (jobKey == null 
                || jobKey.length() != JOB_TOKEN_LENGTH 
                || !jobKey.matches(JOB_TOKEN_REGEX)) {
	    //System.out.println("bad jobkey");
            return false;
        }
        //System.out.println("good jobkey");
        return true;
    }
}
