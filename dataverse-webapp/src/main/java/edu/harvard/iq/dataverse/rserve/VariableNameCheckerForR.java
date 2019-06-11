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
package edu.harvard.iq.dataverse.rserve;


import org.apache.commons.lang.*;
import java.util.*;
import java.util.regex.*;


/**
 *
 * @author Akio Sone
 * (borrowed from DVN v2-3.* intact)
 */
public class VariableNameCheckerForR {
    
    
    public VariableNameCheckerForR(String[] rawVarNames){
        this.rawVarNames = rawVarNames;
    }
    
    private String[] rawVarNames;
    
    private String[] safeVarNames;
    
    private static String[] R_RESERVED_WORDS={
        "NULL","NA","TRUE","FALSE","Inf","NaN",
        "function","while","repeat",
        "for","if","in","else","next","break"
    };
    
    private static String[] R_RESERVED_WORDS_REPLACEMENT ={
        "null","na","true","false",
        "inf","naN","Function","While",
        "Repeat","For","If","In",
        "Else","Next","Break"
    };
    public static Map<String, String> R_RESERVED_WORDS_MAPPING_TABLE = 
        new HashMap<String, String>();
    
    public static Set<String> R_RESERVED_WORD_SET = new HashSet<String>();

    static {
        for (int i=0; i< R_RESERVED_WORDS.length;i++){
            R_RESERVED_WORDS_MAPPING_TABLE.put(R_RESERVED_WORDS[i],R_RESERVED_WORDS_REPLACEMENT[i]);
            R_RESERVED_WORD_SET.add(R_RESERVED_WORDS[i]);
        }
    }
    
    //private static String[] unsafeChar = {"#","$","@","_","?"};
    //private static String[] safeChar = {"hex23","hex24","hex40","hex5F","hex3F"};
    private static String[] unsafeChar = {"#","$","@","?"};
    private static String[] safeChar = {"hex23","hex24","hex40","hex3F"};
    
    private Map<String, String> raw2safeTable = new HashMap<String, String>();
    
    private Map<String, String> safe2rawTable = new HashMap<String, String>();
    
    private List<String> renamedVars = new ArrayList<String>();
    private List<String> renamedResults = new ArrayList<String>();
    

    // ----------------------------------------------------- public method
    
    
    public String[] getFilteredVarNames(){
        // safeVarNames: all variables
        safeVarNames = new String[rawVarNames.length];
        int counter =0;
        for (int i=0; i< rawVarNames.length;i++){
        
            if (R_RESERVED_WORDS_MAPPING_TABLE.containsKey(rawVarNames[i])){
                safeVarNames[i] = R_RESERVED_WORDS_MAPPING_TABLE.get(rawVarNames[i]);
                raw2safeTable.put(rawVarNames[i], safeVarNames[i]);
                safe2rawTable.put(safeVarNames[i], rawVarNames[i]);
                renamedVars.add(rawVarNames[i]);
                renamedResults.add(safeVarNames[i]);
            } else {
                safeVarNames[i] = StringUtils.replaceEachRepeatedly(rawVarNames[i], unsafeChar, safeChar);
                if (!safeVarNames[i].equals(rawVarNames[i])){
                    raw2safeTable.put(rawVarNames[i], safeVarNames[i]);
                    safe2rawTable.put(safeVarNames[i],rawVarNames[i]);
                    renamedVars.add(rawVarNames[i]);
                    renamedResults.add(safeVarNames[i]);
                }
            }
        }
        return safeVarNames;
    }
    
    
    
    public boolean hasRenamedVariables(){
        boolean result = false;
        if (raw2safeTable.size() > 0){
            result = true;
        }
        return result;
    }
    
    public List<String> getRenamedVariableList(){
        return renamedVars;
    }
    
    public List<String> getRenamedResultList(){
        return renamedResults;
    }
    
    public String[] getRenamedVariableArray(){
        return (String[])renamedVars.toArray(new String[renamedVars.size()]);
    }
    
    public String[] getRenamedResultArray(){
        return (String[])renamedResults.toArray(new String[renamedResults.size()]);
    }

    public Map<String, String> getRaw2safeTable(){
        return raw2safeTable;
    }
    
    public Map<String, String> getSafe2rawTable(){
        return safe2rawTable;
    }

    
}
