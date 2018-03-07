/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

import java.sql.Timestamp;

/**
 *
 * @author rmp553
 */
public class UserUtil {
    
    
    /**
     * Convenience method to format dbResult
     * @param dbResult
     * @return 
     */
    public static String getStringOrNull(Object dbResult){
        
        if (dbResult == null){
            return null;
        }
        return (String)dbResult;
    }
    
    /**
     * Convenience method to format dbResult
     * @param dbResult
     * @return 
     */
    public static String getStringOrBlankForNull(Object dbResult){
        
        if (dbResult == null){
            return "";
        }
        return (String)dbResult;
    }
    
    /**
     * Convenience method to format dbResult
     * @param dbResult
     * @return 
     */
    public static String getTimestampStringOrNull(Object dbResult){
        
        if (dbResult == null){
            return null;
        }
        return ((Timestamp)dbResult).toString();
    }

        /**
     * Convenience method to format dbResult
     * @param dbResult
     * @return 
     */
    public static Timestamp getTimestampOrNull(Object dbResult){
        
        if (dbResult == null){
            return null;
        }
        return (Timestamp)dbResult;
    }

}
