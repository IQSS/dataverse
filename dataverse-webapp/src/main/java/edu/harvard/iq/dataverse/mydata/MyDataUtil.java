/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

/**
 *
 * @author rmp553
 */
public class MyDataUtil {
    
    
    public static String formatUserIdentifierAsAssigneeIdentifier(String userIdentifier){
        if (userIdentifier == null){
            return null;
        }
        if (userIdentifier.startsWith("@")){
            return userIdentifier;
        }
        return "@" + userIdentifier;
    }
    
    public static String formatUserIdentifierForMyDataForm(String userIdentifier){
        if (userIdentifier == null){
            return null;
        }
        if (userIdentifier.startsWith("@")){
            if (userIdentifier.length() == 1){
                return null;
            }
            return userIdentifier.substring(1);
        }else{
            return userIdentifier;
        }
    }
}
