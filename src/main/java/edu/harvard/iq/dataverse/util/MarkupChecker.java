/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * Wrapper for Jsoup clean
 * 
 * @author rmp553
 */
public class MarkupChecker {
    
    
    
    /**
     * Wrapper around Jsoup clean method with the basic White list
     *   http://jsoup.org/cookbook/cleaning-html/whitelist-sanitizer
     * @param unsafe
     * @return 
     */
    public static String sanitizeBasicHTML(String unsafe){
        
        if (unsafe == null){
            return null;
        }
        
         // a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul
        return Jsoup.clean(unsafe, Whitelist.basic());
        
    }

    /**
     * Strip all HTMl tags
     * 
     * http://jsoup.org/apidocs/org/jsoup/safety/Whitelist.html#none%28%29
     * 
     * @param unsafe
     * @return 
     */
    public static String stripAllTags(String unsafe){
        
        if (unsafe == null){
            return null;
        }        
        
        return Jsoup.clean(unsafe, Whitelist.none());
        
    }

}
