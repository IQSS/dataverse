/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.lang.RandomStringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.primefaces.util.Base64;

/**
 *
 * original author: Ellen Kraffmiller
 */
public final class PasswordEncryption  implements java.io.Serializable {
    private static PasswordEncryption instance;

    private PasswordEncryption() {
    }

    public synchronized String encrypt(String plaintext) {
        /**
        * below is how you encrypt a password with jbcrypt. gensalt() has a
        * default log_rounds parameter of 10. To modify the complexity, add
        * the parameter to the call, e.g., Bcrypt.gensalt(12) increases 
        * complexity
        */
        //String bcryptedPassword = BCrypt.hashpw(plaintext, BCrypt.gensalt());
        MessageDigest md = null;
        try {
            /**
             * @todo For better security, switch from SHA to Bcrypt with mode
             * set to SHA256 or SHA512 (SHA2, seeded hash, includes salting)
             * https://github.com/IQSS/dataverse/issues/1034
             *
             * What impact will this change have on migrated passwords that were
             * created with SHA in DVN 3.6?
             */
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        byte[] raw = md.digest();
        String hash = Base64.encodeToString(raw, true);
        return hash;
    }
  
    public static synchronized PasswordEncryption getInstance() {
        if(instance == null) {
            instance = new PasswordEncryption(); 
        } 
        return instance;
    }
  
    public static String generateRandomPassword() {
        return RandomStringUtils.randomAlphanumeric(8);
    }
    
    public static void main(String[] args) {
        for (int i=0;i<10;i++) {
            System.out.println("Random String-"+generateRandomPassword());
        }
    }
} 
