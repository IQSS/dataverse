package edu.harvard.iq.dataverse.authorization.providers.builtin;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Random;

/**
 * Password encryption, supporting multiple encryption algorithms to
 * allow migrations between them.
 * <p>
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
        String encrypt(String plainText);

        boolean check(String plainText, String hashed);
    }
    
    /**
     * SHA512 algorithm run over PBKDF2 function. Same as encryption used in
     * ckan RepOD installation
     */
    private static final Algorithm PBKDF2_SHA512 = new Algorithm() {

        @Override
        public String encrypt(String plainText) {
            int iterations = 1900;
            
            Random random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            byte[] hash = generateHash(plainText, salt, iterations, 512);
            
            Encoder base64Encoder = Base64.getEncoder().withoutPadding();
            String saltString = base64Encoder.encodeToString(salt).replace('+', '.');
            String hashString = base64Encoder.encodeToString(hash).replace('+', '.');
            
            return "$pbkdf2-sha512$" + iterations + "$" + saltString + "$" + hashString;
        }

        @Override
        public boolean check(String plainText, String hashed) {
            Decoder base64Decoder = Base64.getDecoder();
            String[] parts = StringUtils.split(hashed, '$');
            
            int iterations = Integer.parseInt(parts[1]);
            
            byte[] salt = base64Decoder.decode(parts[2].replace('.', '+').getBytes(StandardCharsets.US_ASCII));
            byte[] hash = base64Decoder.decode(parts[3].replace('.', '+').getBytes(StandardCharsets.US_ASCII));
            
            byte[] testHash = generateHash(plainText, salt, iterations, hash.length * 8);
            
            int diff = hash.length ^ testHash.length;
            for(int i = 0; i < hash.length && i < testHash.length; i++)
            {
                diff |= hash[i] ^ testHash[i];
            }
            return diff == 0;
        }
        
        private byte[] generateHash(String password, byte[] salt, int iterations, int keyLength) {
            try {
                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                return skf.generateSecret(spec).getEncoded();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException("Unable to generate password hash", e);
            }
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
            } catch (java.lang.IllegalArgumentException iae) {
                // the password was probably not hashed using bcrypt.
                return false;
            }
        }
    };

    private static final Algorithm[] algorithms;

    static {
        algorithms = new Algorithm[]{PBKDF2_SHA512, BCRYPT_10};
    }

    /**
     * Prevent people instantiating this class.
     */
    private PasswordEncryption() {
    }

    /**
     * @return The current version of the password hashing algorithm.
     */
    public static Algorithm get() {
        return getVersion(getLatestVersionNumber());
    }

    public static int getLatestVersionNumber() {
        return algorithms.length - 1;
    }

    public static Algorithm getVersion(int i) {
        return algorithms[i];
    }

    public static String generateRandomPassword() {
        return RandomStringUtils.randomAlphanumeric(8);
    }

}
