package edu.harvard.iq.dataverse.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;

/**
 *
 * @author skraffmiller
 */
public class StringUtil {
       
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
    public static final boolean isAlphaNumericChar(char c) {
        // TODO: consider using Character.isLetterOrDigit(c)
        return ( (c >= 'a') && (c <= 'z') ||
                 (c >= 'A') && (c <= 'Z') ||
                 (c >= '0') && (c <= '9') );
}

    public static String truncateString(String originalString, int maxLength) {
        maxLength = Math.max( 0, maxLength);
        String finalString = originalString;
        if (finalString != null && finalString.length() > maxLength) {
            String regexp = "[A-Za-z0-9][\\p{Space}]";
            Pattern pattern = Pattern.compile(regexp);
            String startParsedString = finalString.substring(0, maxLength);
            String endParsedString = finalString.substring(maxLength, finalString.length());
            Matcher matcher = pattern.matcher(endParsedString);
            boolean found = matcher.find();
            if (found) {
                endParsedString = endParsedString.substring(0, matcher.end());
                finalString = startParsedString + endParsedString + "<span class='dvn_threedots'>...</span>";
            }
        }
        
        return finalString;             
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

}
