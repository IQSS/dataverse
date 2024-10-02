package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2LoginBackingBean;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

/**
 *
 * @author skraffmiller
 */
public class StringUtil {
       
    private static final Logger logger = Logger.getLogger(StringUtil.class.getCanonicalName());
    public static final Set<String> TRUE_VALUES = Collections.unmodifiableSet(new TreeSet<>( Arrays.asList("1","yes", "true","allow")));
    
    public static final boolean nonEmpty( String str ) {
        return ! isEmpty(str);
    }
    
    public static final boolean isEmpty(String str) {
        return str==null || str.trim().equals("");        
    }
    
    public static  String nullToEmpty(String inString) {
        return inString == null ? "" : inString;
    }

    public static final boolean isAlphaNumeric(String str) {
      final char[] chars = str.toCharArray();
      for (int x = 0; x < chars.length; x++) {      
        final char c = chars[x];
        if(! isAlphaNumericChar(c)) {
            return false;
        }
      }  
      return true;
    }
    
    public static String substringIncludingLast(String str, String separator) {
      if (isEmpty(str)) {
          return str;
      }
      if (isEmpty(separator)) {
          return "";
      }
      int pos = str.lastIndexOf(separator);
      if (pos == -1 || pos == (str.length() - separator.length())) {
          return "";
      }
      return str.substring(pos);
  }
    
    public static Optional<String> toOption(String s) {
        if ( isEmpty(s) ) {
            return Optional.empty();
        } else {
            return Optional.of(s.trim());
        }
    }
    
    
    /**
     * Checks if {@code s} contains a "truthy" value.
     * @param s
     * @return {@code true} iff {@code s} is not {@code null} and is "truthy" word.
     * @see #TRUE_VALUES
     */
    public static boolean isTrue( String s ) {
        return (s != null ) && TRUE_VALUES.contains(s.trim().toLowerCase());
    }
    
    public static final boolean isAlphaNumericChar(char c) {
        // TODO: consider using Character.isLetterOrDigit(c)
        return ( (c >= 'a') && (c <= 'z') ||
                 (c >= 'A') && (c <= 'Z') ||
                 (c >= '0') && (c <= '9') );
    }

    public static String html2text(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.parse(html).text();
    }

    /**
     * @return A list of clean strings or an empty list.
     */
    public static List<String> htmlArray2textArray(List<String> htmlArray) {
        List<String> cleanTextArray = new ArrayList<>();
        if (htmlArray == null) {
            return cleanTextArray;
        }
        for (String html : htmlArray) {
            cleanTextArray.add(Jsoup.parse(html).text());
        }
        return cleanTextArray;
    }
    
    private final static SecureRandom secureRandom = new SecureRandom();
    // 12 bytes is recommended by GCM spec
    private final static int GCM_IV_LENGTH = 12;

    /**
     * Generates an AES-encrypted version of the string. Resultant string is URL safe.
     * @param value The value to encrypt.
     * @param password The password.
     * @return encrypted string, URL-safe.
     */
    public static String encrypt(String value, String password ) {

        byte[] baseBytes = value.getBytes();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH]; //NEVER REUSE THIS IV WITH SAME KEY
            secureRandom.nextBytes(iv);
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            final SecretKeySpec secretKeySpec = generateKeyFromString(password);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv); 
            aes.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);
            byte[] encrypted = aes.doFinal(baseBytes);
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            String base64ed = new String(Base64.getEncoder().encode(byteBuffer.array()));
            return base64ed.replaceAll("\\+", ".")
                    .replaceAll("=", "-")
                    .replaceAll("/", "_");
            
        } catch (  InvalidKeyException | NoSuchAlgorithmException | BadPaddingException
                  | IllegalBlockSizeException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(OAuth2LoginBackingBean.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    public static String decrypt(String value, String password ) {
        String base64 = value.replaceAll("\\.", "+")
                    .replaceAll("-", "=")
                    .replaceAll("_", "/");
        
        byte[] baseBytes = Base64.getDecoder().decode(base64);
        try {
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            //use first 12 bytes for iv
            AlgorithmParameterSpec gcmIv = new GCMParameterSpec(128, baseBytes, 0, GCM_IV_LENGTH);
            aes.init( Cipher.DECRYPT_MODE, generateKeyFromString(password),gcmIv);
            byte[] decrypted = aes.doFinal(baseBytes,GCM_IV_LENGTH, baseBytes.length - GCM_IV_LENGTH);
            return new String(decrypted);
            
        } catch ( InvalidKeyException | NoSuchAlgorithmException | BadPaddingException
                  | IllegalBlockSizeException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(OAuth2LoginBackingBean.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    public static String sanitizeFileDirectory(String value) {
        return sanitizeFileDirectory(value, false);
    }
    
    public static String sanitizeFileDirectory(String value, boolean aggressively){        
        // Replace all the combinations of slashes and backslashes with one single 
        // backslash:
        value = value.replaceAll("[\\\\/][\\\\/]*", "/");

        if (aggressively) {
            value = value.replaceAll("[^A-Za-z0-9_ ./\\-]+", ".");
            value = value.replaceAll("\\.\\.+", ".");
        }
        
        // Strip any leading or trailing slashes, whitespaces, '-' or '.':
        while (value.startsWith("/") || value.startsWith("-") || value.startsWith(".") || value.startsWith(" ")){
            value = value.substring(1);
        }
        while (value.endsWith("/") || value.endsWith("-") || value.endsWith(".") || value.endsWith(" ")){
            value = value.substring(0, value.length() - 1);
        }
        
        if ("".equals(value)) {
            return null;
        }
        
        return value;
    }
    
    
    private static SecretKeySpec generateKeyFromString(final String secKey) throws NoSuchAlgorithmException {
        byte[] key = (secKey).getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bits

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;
    }
    
    /**
     * Normalize sentence
     * 
     * @author francesco.cadili@4science.it
     *
     *
     * @param sentence full name or organization name
     * @return normalize string value
     */
    static public String normalize(String sentence) {
        if (StringUtils.isBlank(sentence)) {
            return "";
        }

        sentence = sentence.trim().replaceAll(", *", ", ").replaceAll(" +", " ");

        return sentence;
    }
}
