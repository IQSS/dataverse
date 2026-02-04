package edu.harvard.iq.dataverse.authorization.providers.builtin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.lang3.RandomStringUtils;
import org.mindrot.jbcrypt.BCrypt;
//import org.primefaces.util.Base64;
import java.util.Base64;

/**
 * Password encryption, supporting multiple encryption algorithms to
 * allow migrations between them.
 * 
 * When adding a new password hashing algorithm, implement the {@link Algorithm}
 * interface, and add an instance of the implementation as the last element
 * of the {@link #algorithms} array. The rest should pretty much happen automatically
 * (e.g system will detect outdated passwords for users and initiate the password reset breakout).
 * 
 * @author Ellen Kraffmiller
 * @author Michael Bar-Sinai
 */
public final class PasswordEncryption implements java.io.Serializable {
    
    public interface Algorithm {
        String encrypt( String plainText );
        boolean check( String plainText, String hashed );
    }
    
    /**
     * The SHA algorithm, now considered not secure enough.
     */
    private static final Algorithm SHA = new Algorithm() {

        @Override
        public String encrypt(String plainText) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(plainText.getBytes(StandardCharsets.UTF_8));
                byte[] raw = md.digest();
                //String hash = Base64.encodeToString(raw, true);
                String hash = Base64.getEncoder().encodeToString(raw);
                return hash;
                
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean check(String plainText, String hashed) {
            return hashed.equals( encrypt(plainText) );
        }
    };
    
    /**
     * BCrypt, using a complexity factor of 10 (considered safe by 2015 standards).
     */
    private static final Algorithm BCRYPT_10 = new Algorithm() {

        @Override
        public String encrypt(String plainText) {
            return BCrypt.hashpw(plainText, BCrypt.gensalt());
        }

        @Override
        public boolean check(String plainText, String hashed) {
            try {
                return BCrypt.checkpw(plainText, hashed);
            } catch (java.lang.IllegalArgumentException iae ) {
                // the password was probably not hashed using bcrypt.
                return false;
            }
        }
    };
    
    private static final Algorithm[] algorithms;
    
    static {
        algorithms = new Algorithm[]{SHA, BCRYPT_10};
    }
    
    /**
     * Prevent people instantiating this class.
     */
    private PasswordEncryption() {}

    /**
     * @return The current version of the password hashing algorithm.
     */
    public static Algorithm get() {
        return getVersion( getLatestVersionNumber() );
    }
    
    public static int getLatestVersionNumber() {
        return algorithms.length-1;
    }
    
    public static Algorithm getVersion(int i){
        return algorithms[i];
    }
    
    public static String generateRandomPassword() {
        return RandomStringUtils.randomAlphanumeric(8);
    }
    
}
