/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
package edu.harvard.iq.dataverse.util.xml.html;

/**
 * A simple utility that generates formatted HTML
 * to avoid hard-coding html tags by hand. 
 * 
 * @author leonid@hmdc.harvard.edu
 */
public class HtmlFormatUtil  implements java.io.Serializable {
    public static final String HTML_DOCTYPE_HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">";
    public static final String HTML_TAG = "html";
    public static final String HTML_HEAD = "head";
    public static final String HTML_BODY = "body";
    public static final String HTML_TITLE = "title";
    public static final String HTML_TABLE = "table";
    public static final String HTML_TABLE_ROW = "tr";
    public static final String HTML_TABLE_HDR = "th";
    public static final String HTML_TABLE_CELL = "td";
    public static final String HTML_LINK = "a";
    public static final String HTML_HREF = "href";
    public static final String HTML_H1 = "h1";
    
    public static final String HTML_ALIGN_TOP = "valign=\"top\"";
    public static final String HTML_ALIGN_RIGHT = "align=\"right\""; 
    
    public static String formatTable (String tableBody) {
        return formatTag(tableBody, HTML_TABLE);
    }
    
    public static String formatTableRow (String entry) {
        return formatTag(entry, HTML_TABLE_ROW);
    }
    
    public static String formatTableCell (String entry) {
        return formatTag(entry, HTML_TABLE_CELL, null);
    }
    
    public static String formatTableCell (String entry, String attr) {
        return formatTag(entry, HTML_TABLE_CELL, attr);
    }
    
    public static String formatTableCellValignTop(String entry) {
        return formatTableCell (entry, HTML_ALIGN_TOP); 
    }
    
    public static String formatTableCellAlignRight(String entry) {
        return formatTableCell (entry, HTML_ALIGN_RIGHT);
    }
    
    public static String formatLink(String name, String url) {
        String href = HTML_HREF + "=\"" + url + "\""; 
        
        return formatTag(name, HTML_LINK, href);
    }
    
    public static String formatTitle(String title) {
        return formatTag(title, HTML_TITLE);
    }
    
    public static String formatTag(String entry, String tag) {
        return formatTag(entry, tag, null);
    }
    
    public static String formatTag(String entry, String tag, String attributes) {
        StringBuilder sb = new StringBuilder(); 
        sb.append('<');
        sb.append(tag);
        
        if (attributes != null) {
            sb.append(" "+attributes);
        }
        
        sb.append('>');
        sb.append(entry);
        sb.append("</");
        sb.append(tag);
        sb.append('>');
        
        return sb.toString();
    }
    
    public static String formatTagOpen(String tag) {
        StringBuilder sb = new StringBuilder(); 
        sb.append('<');
        sb.append(tag);
        sb.append('>');
        return sb.toString();
    }
    
    public static String formatTagClose(String tag) {
        StringBuilder sb = new StringBuilder(); 
        sb.append("</");
        sb.append(tag);
        sb.append('>');
        return sb.toString();
    }
    
    public static String formatDoc(String header, String body) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(HTML_DOCTYPE_HEADER);
        sb.append("\n");
        sb.append(formatTagOpen(HTML_TAG));
        
        sb.append(formatTag(header, HTML_HEAD));
        sb.append("\n");
        sb.append(formatTag(body, HTML_BODY));
        sb.append("\n");
        sb.append(formatTagClose(HTML_TAG));
        sb.append("\n");
        return sb.toString();
    }
    
}
