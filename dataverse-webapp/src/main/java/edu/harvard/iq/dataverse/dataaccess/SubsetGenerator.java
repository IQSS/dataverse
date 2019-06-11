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
package edu.harvard.iq.dataverse.dataaccess;



/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.InputStream;
import java.util.*;
    
/**
 * original author:
 * @author Akio Sone
 * (for DVN v.2-3)
 *
 * @author Leonid Andreev
 */
public interface SubsetGenerator {
        
    public  void subsetFile(String infile, String outfile, List<Integer> columns, Long numCases);

    public void subsetFile(String infile, String outfile, List<Integer> columns, Long numCases,
        String delimiter);

    public void subsetFile(InputStream in, String outfile, List<Integer> columns, Long numCases,
        String delimiter);
}
