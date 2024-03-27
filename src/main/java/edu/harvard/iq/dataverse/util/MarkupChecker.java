/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.util;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.parser.Parser;

/**
 * Wrapper for Jsoup clean
 * 
 * @author rmp553
 */
public class MarkupChecker {
    
    
    
    /**
     * Wrapper around Jsoup clean method with the basic Safe list
     *   http://jsoup.org/cookbook/cleaning-html/safelist-sanitizer
     * @param unsafe
     * @return 
     */
    public static String sanitizeBasicHTML(String unsafe) {

        if (unsafe == null) {
            return null;
        }
        // basic includes: a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul
        //Whitelist wl = Whitelist.basic().addTags("img", "h1", "h2", "h3", "kbd", "hr", "s", "del");  

        Safelist sl = Safelist.basicWithImages().addTags("h1", "h2", "h3", "kbd", "hr", "s", "del", "map", "area").addAttributes("img", "usemap")
                .addAttributes("map", "name").addAttributes("area", "shape", "coords", "href", "title", "alt")
                .addEnforcedAttribute("a", "target", "_blank");

        return Jsoup.clean(unsafe, sl);

    }
        
    /**
     * Strip all HTMl tags
     * 
     * http://jsoup.org/apidocs/org/jsoup/safety/Safelist.html#none
     * 
     * @param unsafe
     * @return 
     */
    public static String stripAllTags(String unsafe) {

        if (unsafe == null) {
            return null;
        }

        return Parser.unescapeEntities(Jsoup.clean(unsafe, Safelist.none()), true);

    }
    
    public static String escapeHtml(String unsafe) {
         return StringEscapeUtils.escapeHtml4(unsafe);
    }

}
