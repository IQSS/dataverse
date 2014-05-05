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
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.sav;


import java.util.*;

/**
 * This class lists various data-format-related contants shared by 
 * SPSS SAV and POR formats.
 * 
 * Akio Sone's original DVN 2.* implementation, intact. 
 * 
 * @author Akio Sone
 */
public final class SPSSConstants {

    private SPSSConstants() {
    }

    private static final int[] FORMAT_KEYS_SAV = {
      0,  1,  2,  3,  4,  5,  6,  7,  8,  9,
     10, 11, 12, 15, 16, 17,
     20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
     30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
     
     
     private static final int[] FORMAT_KEYS_POR = {
          0,  1,  2,  3,  4,  5,  6,  7,  8,  9,
         10, 11, 12, 15, 16, 17,
        102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 
        112, 113, 114, 115, 116, 117, 118, 119, 120, 121};

    private static final String[] FORMAT_VALUES ={
    "CONTINUE","A", "AHEX","COMMA","DOLLAR","F","IB","PIBHEX","P","PIB",
    "PK", "RB", "RBHEX","Z","N","E", "DATE",
    "TIME", "DATETIME","ADATE","JDATE","DTIME","WKDAY","MONTH","MOYR","QYR","WKYR",
    "PCT","DOT","CCA","CCB","CCC","CCD","CCE","EDATE","SDATE"};

    private static final String[] FORMAT_CATEGORIES = {
    "other","other","other","other","currency","other","other","other","other","other",
    "other","other","other","other","other","other","date",
    "time","time","date","date","time","other","other","date","date","date",
    "other","other","currency","currency","currency","currency","currency","date","date"};

    /**
     * A mapping table from a <code>Integer</code> value to 
     * SPSS SAV data-format code.
     */
    public static Map<Integer, String> FORMAT_CODE_TABLE_SAV =
            new LinkedHashMap<Integer, String>();
            
    /**
     * A mapping table from a <code>Integer</code> value to 
     * SPSS POR data-format code.
     * <p>Note: after 17, SPSS POR and SAV formats no longer
     * coincide.
     */
    public static Map<Integer, String> FORMAT_CODE_TABLE_POR =
            new LinkedHashMap<Integer, String>();
            
    /**
     * A mapping table that groups data-formats into three categories
     * (date, time, and other).
     */
    public static Map<String, String> FORMAT_CATEGORY_TABLE =
            new LinkedHashMap<String, String>();

    private static List<Integer> ORDINARY_FORMAT_CODE = Arrays.asList(0, 1, 5);

    /**
     * A <code>Set</code> instance that tells whether a given format code
     * is not a date/time type.
     */
    public static final Set<Integer> ORDINARY_FORMAT_CODE_SET =
            new LinkedHashSet<Integer>(ORDINARY_FORMAT_CODE);

    /**
     * A <code>String</code> array of short weekday names in English 
     */
    public static String[] WEEKDAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    
    /**
     * A <code>String</code> array of short month names in English 
     */
    public static String[] MONTHS  = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
    
    /**
     * A mapping table from an <code>Integer</code> to
     * a short-weekday name in English.
     */
    public static final Map<Integer, String> WEEKDAY_LIST= new LinkedHashMap<Integer, String>();
    /**
     * A mapping table from an <code>Integer</code> to
     * a short-month name in English.
     */
    public static final Map<Integer, String> MONTH_LIST= new LinkedHashMap<Integer, String>();

    static{
        for (int i=0; i< FORMAT_KEYS_SAV.length; i++){
            FORMAT_CODE_TABLE_SAV.put(FORMAT_KEYS_SAV[i], FORMAT_VALUES[i]);
            FORMAT_CODE_TABLE_POR.put(FORMAT_KEYS_POR[i], FORMAT_VALUES[i]);
            FORMAT_CATEGORY_TABLE.put(FORMAT_VALUES[i], FORMAT_CATEGORIES[i]);

        }
        
        for (int i=0; i< WEEKDAYS.length;i++){
            WEEKDAY_LIST.put(i, WEEKDAYS[i]);
        }
        for (int i=0; i< MONTHS.length;i++){
            MONTH_LIST.put(i, MONTHS[i]);
        }
        
    }


}
