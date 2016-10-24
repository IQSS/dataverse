/*
   Copyright (C) 2005-2016, by the President and Fellows of Harvard College.

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
   Version 4.0.
*/

package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Various helper methods used by IngestServiceBean.
 *
 * @author bmckinney
 */
public class IngestServiceBeanHelper {

    private static final Logger logger = Logger.getLogger(IngestServiceBeanHelper.class.getCanonicalName());

    /**
     * Checks a list of new data files for duplicate names, renaming any duplicates to ensure that they are unique.
     *
     * @param version  the dataset version
     * @param newFiles the list of new data files to add to it
     */
     static void checkForDuplicateFileNamesFinal(DatasetVersion version, List<DataFile> newFiles) {

        // list of existing unique path name: directoryLabel + file separator + fileLabel
        Set<String> pathNamesExisting = new HashSet<>();

        // Step 1: create list of existing path names from all FileMetadata in the DatasetVersion
        for (Iterator<FileMetadata> fmIt = version.getFileMetadatas().iterator(); fmIt.hasNext();) {
            FileMetadata fm = fmIt.next();
            if (fm.getId() != null) {
                String existingName = fm.getLabel();
                String existingDir = fm.getDirectoryLabel();
                // add file separator to end of any non-empty directory label
                if (!existingDir.isEmpty()) {
                    existingDir = existingDir + File.separator;
                }
                String existingPath = existingDir + existingName;

                if (!existingPath.isEmpty()) {
                    // if it's a tabular file, we need to restore the original file name; otherwise, we may miss a 
                    // match. e.g. stata file foobar.dta becomes foobar.tab once ingested!
                    if (fm.getDataFile().isTabularData()) {
                        String originalMimeType = fm.getDataFile().getDataTable().getOriginalFileFormat();
                        if (originalMimeType != null) {
                            String origFileExtension = FileUtil.generateOriginalExtension(originalMimeType);
                            existingPath = existingPath.replaceAll(".tab$", origFileExtension);
                        } else {
                            existingPath = existingPath.replaceAll(".tab$", "");
                        }
                    }
                    pathNamesExisting.add(existingPath);
                }
            }
        }

        // Step 2: check each new DataFile against the list of path names, if a duplicate create a new unique file name
        for (Iterator<DataFile> dfIt = newFiles.iterator(); dfIt.hasNext();) {
            FileMetadata fm = dfIt.next().getFileMetadata();
            String fileName = fm.getLabel();
            String dirName = fm.getDirectoryLabel();
            if (!dirName.isEmpty()) {
                dirName = dirName + File.separator;
            }
            String pathName = dirName + fileName;
            while (pathNamesExisting.contains(pathName)) {
                fileName = generateNewFileName(fileName);
                logger.log(Level.FINE, "Renamed file pathname: " + pathName + " to: " + dirName + fileName);
                pathName = dirName + fileName;
            }
            if (!fm.getLabel().equals(fileName)) {
                fm.setLabel(fileName);
                pathNamesExisting.add(pathName);
            }
        }
    }

    /**
     * Generates a new unique filename by adding -[number] to the base name.
     *
     * @param fileName original filename
     * @return a new unique filename
     */
     static String generateNewFileName(final String fileName) {
        String newName;
        String baseName;
        String extension = null;

        int extensionIndex = fileName.lastIndexOf(".");
        if (extensionIndex != -1) {
            extension = fileName.substring(extensionIndex + 1);
            baseName = fileName.substring(0, extensionIndex);
        } else {
            baseName = fileName;
        }

        if (baseName.matches(".*-[0-9][0-9]*$")) {
            int dashIndex = baseName.lastIndexOf("-");
            String numSuffix = baseName.substring(dashIndex + 1);
            String basePrefix = baseName.substring(0, dashIndex);
            int numSuffixValue = Integer.parseInt(numSuffix);
            numSuffixValue++;
            baseName = basePrefix + "-" + numSuffixValue;
        } else {
            baseName = baseName + "-1";
        }

        newName = baseName;
        if (extension != null) {
            newName = newName + "." + extension;
        }
        
        return newName;
    }

}
