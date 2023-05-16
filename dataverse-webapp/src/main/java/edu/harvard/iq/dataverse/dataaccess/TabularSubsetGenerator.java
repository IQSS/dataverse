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

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Leonid Andreev
 * original author:
 * @author a.sone
 */
public class TabularSubsetGenerator {

    // -------------------- LOGIC --------------------

    public void subsetFile(InputStream in, String outfile, List<Integer> columns, Long numCases) {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            for (long caseIndex = 0; caseIndex < numCases; caseIndex++) {
                line = reader.readLine();
                if (line != null) {
                    String[] values = line.split("\t", -1);
                    List<String> ln = new ArrayList<>();
                    for (Integer i : columns) {
                        ln.add(values[i]);
                    }
                    out.write(StringUtils.join(ln, "\t") + "\n");
                } else {
                    throw new RuntimeException("Tab file has fewer rows than the determined number of cases.");
                }
            }
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                    throw new RuntimeException("Tab file has extra nonempty rows than the determined number of cases.");
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Exception while reading file", ioe);
        }
    }
}


