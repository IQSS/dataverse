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
package edu.harvard.iq.dataverse.ingest.tabulardata;

import java.util.*;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
/**
 * A class that stores information about a variables' invalid data.
 * Akio Sone's original DVN v.2.* implementation, virtually unchanged. 
 * 
 * @author Akio Sone
 * 
 * incorporated into Dataverse 4.0 by Leonid Andreev, 2014
 */

public class InvalidData {


    public InvalidData(int type) {
        this.type = type;
    }

    int type;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    List<String> invalidValues;

    public List<String> getInvalidValues() {
        return invalidValues;
    }

    
    public void setInvalidValues(List<String> invalidValues) {
        this.invalidValues = invalidValues;
    }

    List<String> invalidRange;

    public List<String> getInvalidRange() {
        return invalidRange;
    }


    public void setInvalidRange(List<String> invalidRange) {
        this.invalidRange = invalidRange;
    }



    /* 
     * This method used to be used by the old DDIWriter. 
     * TODO: check how these values were affecting the behavior of 
     * the old DDIService import; implement the direct configuration 
     * of DataVariables as appropriate. 
     * -- L.A. 4.0 beta
    */
    public String toDDItag(){
        StringBuilder sb = new StringBuilder();

        switch(type){
            case 1: case 2: case 3:
                    sb.append("\t\t<invalrng>\n");
                    for (int k=0; k < invalidValues.size();k++){
                        sb.append("\t\t\t<item VALUE=\"" + invalidValues.get(k)+"\"/>\n");
                    }
                    sb.append("\t\t</invalrng>\n");
                break;
            case -2:
                    // range-type 1 missing values
                    sb.append("\t\t<invalrng>\n");
                    sb.append("\t\t\t<range");
                    if (!invalidRange.get(0).equals("LOWEST")){
                        sb.append(" min=\""+invalidRange.get(0)+"\"");
                    }
                    if (!invalidRange.get(1).equals("HIGHEST")) {
                        sb.append(" max=\"" + invalidRange.get(1) + "\"");
                    }
                    sb.append("/>\n");
                    sb.append("\t\t</invalrng>\n");
                break;
            case -3:
                    // range-type: 2 missing values
                    sb.append("\t\t<invalrng>\n");
                    sb.append("\t\t\t<range");
                    if (!invalidRange.get(0).equals("LOWEST")) {
                        sb.append(" min=\""+invalidRange.get(0)+"\"");
                    }
                    if (!invalidRange.get(1).equals("HIGHEST")) {
                        sb.append(" max=\"" + invalidRange.get(1) +"\"");
                    }
                    sb.append("/>\n");
                    sb.append("\t\t\t<item VALUE=\"" +invalidValues.get(0)+"\"/>\n");
                    sb.append("\t\t</invalrng>\n");

                break;
            default:
        }
        return sb.toString();
    }

    /**
     * Returns a string representation of this instance.
     * 
     * @return a string representing this instance.
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
            ToStringStyle.MULTI_LINE_STYLE);
    }

}
