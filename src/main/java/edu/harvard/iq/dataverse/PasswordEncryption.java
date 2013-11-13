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
import org.primefaces.util.Base64;

/**
 *
 * @author xyang
 */
public final class PasswordEncryption  implements java.io.Serializable {
    private static PasswordEncryption instance;

    private PasswordEncryption() {
    }

    public synchronized String encrypt(String plaintext) {
        MessageDigest md = null;
        try {
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
