/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.userdata;

import edu.harvard.iq.dataverse.UserNotification.Type;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    
    /**
     * Convenience method to format UserNotification.Type from flag value
     * @param mutedLongFlags
     * @return 
     */
    public static String getMutedStringOrNull(Set<Type> muted){
        if (muted == null){
            return null;
        }
        final List<String> types = muted
                .stream().map(x -> x.name()).collect(Collectors.toList());
        return String.join(",", types);
    }

}
