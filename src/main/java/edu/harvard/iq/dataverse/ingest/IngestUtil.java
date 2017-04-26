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
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.dataverse.unf.UNFUtil;
import org.dataverse.unf.UnfException;

/**
 * Various helper methods used by IngestServiceBean.
 *
 * @author bmckinney
 */
public class IngestUtil {

    private static final Logger logger = Logger.getLogger(IngestUtil.class.getCanonicalName());

    /**
     * Checks a list of new data files for duplicate names, renaming any
     * duplicates to ensure that they are unique.
     *
     * @param version the dataset version
     * @param newFiles the list of new data files to add to it
     */
    public static void checkForDuplicateFileNamesFinal(DatasetVersion version, List<DataFile> newFiles) {

        // Step 1: create list of existing path names from all FileMetadata in the DatasetVersion
        // unique path name: directoryLabel + file separator + fileLabel
        Set<String> pathNamesExisting = existingPathNamesAsSet(version);

        // Step 2: check each new DataFile against the list of path names, if a duplicate create a new unique file name
        for (Iterator<DataFile> dfIt = newFiles.iterator(); dfIt.hasNext();) {

            FileMetadata fm = dfIt.next().getFileMetadata();

            fm.setLabel(duplicateFilenameCheck(fm, pathNamesExisting));
        }
    }

    /**
     * Checks if the unique file path of the supplied fileMetadata is already on
     * the list of the existing files; and if so, keeps generating a new name
     * until it is unique. Returns the final file name. (i.e., it only modifies
     * the filename, and not the folder name, in order to achieve uniqueness)
     *
     * @param fileMetadata supplied FileMetadata
     * @param existingFileNames a set of the already existing pathnames
     * @return a [possibly] new unique filename
     */
    public static String duplicateFilenameCheck(FileMetadata fileMetadata, Set<String> existingFileNames) {
        if (existingFileNames == null) {
            existingFileNames = existingPathNamesAsSet(fileMetadata.getDatasetVersion());
        }

        String fileName = fileMetadata.getLabel();
        String directoryName = fileMetadata.getDirectoryLabel();
        String pathName = makePathName(directoryName, fileName);

        while (existingFileNames.contains(pathName)) {
            fileName = IngestUtil.generateNewFileName(fileName);
            pathName = IngestUtil.makePathName(directoryName, fileName);
        }

        existingFileNames.add(pathName);
        return fileName;
    }

    // This method is called on a single file, when we need to modify the name 
    // of an already ingested/persisted datafile. For ex., when we have converted
    // a file to tabular data, and want to update the extension accordingly. 
    public static void modifyExistingFilename(DatasetVersion version, FileMetadata fileMetadata, String newFilename) {
        // Step 1: create list of existing path names from all FileMetadata in the DatasetVersion
        // unique path name: directoryLabel + file separator + fileLabel
        fileMetadata.setLabel(newFilename);
        Set<String> pathNamesExisting = existingPathNamesAsSet(version, fileMetadata);
        fileMetadata.setLabel(duplicateFilenameCheck(fileMetadata, pathNamesExisting));

    }

    // unique path name: directoryLabel + file separator + fileLabel
    public static String makePathName(String directoryName, String fileName) {
        String pathName;
        if (directoryName != null && !directoryName.isEmpty()) {
            pathName = directoryName + File.separator + fileName;
        } else {
            pathName = fileName;
        }
        return pathName;
    }

    /**
     * Generates a new unique filename by adding -[number] to the base name.
     *
     * @param fileName original filename
     * @return a new unique filename
     */
    public static String generateNewFileName(final String fileName) {
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

    // list of existing unique path name: directoryLabel + file separator + fileLabel
    public static Set<String> existingPathNamesAsSet(DatasetVersion version) {
        return existingPathNamesAsSet(version, null);
    }

    private static Set<String> existingPathNamesAsSet(DatasetVersion version, FileMetadata fileMetadata) {
        Set<String> pathNamesExisting = new HashSet<>();

        // create list of existing path names from all FileMetadata in the DatasetVersion
        // (skipping the one specified fileMetadata, if supplied. That's in order to 
        // be able to call this method 
        for (Iterator<FileMetadata> fmIt = version.getFileMetadatas().iterator(); fmIt.hasNext();) {
            FileMetadata fm = fmIt.next();
            if (fm.getId() != null && (fileMetadata == null || !fm.getId().equals(fileMetadata.getId()))) {
                String existingName = fm.getLabel();
                String existingDir = fm.getDirectoryLabel();

                String existingPath = makePathName(existingDir, existingName);

                if (!existingPath.isEmpty()) {
                    pathNamesExisting.add(existingPath);

                    // if it's a tabular file, we need to also restore the original file name; otherwise, we may miss a 
                    // match. e.g. stata file foobar.dta becomes foobar.tab once ingested!
                    if (fm.getDataFile().isTabularData()) {
                        String originalPath;
                        String originalMimeType = fm.getDataFile().getDataTable().getOriginalFileFormat();
                        if (originalMimeType != null) {
                            String origFileExtension = FileUtil.generateOriginalExtension(originalMimeType);
                            originalPath = existingPath.replaceAll(".tab$", origFileExtension);
                        } else {
                            originalPath = existingPath.replaceAll(".tab$", "");
                        }
                        pathNamesExisting.add(originalPath);

                    }
                }
            }
        }

        return pathNamesExisting;
    }

    /**
     * @param version The DatasetVersion to mutate, setting or unsetting the
     * UNF.
     */
    public static void recalculateDatasetVersionUNF(DatasetVersion version) {
        logger.fine("recalculating UNF for dataset version.");
        if (version == null) {
            return;
        }
        List<String> unfValueList = getUnfValuesOfFiles(version);
        if (unfValueList.size() > 0) {
            String[] unfValues = new String[0];
            unfValues = unfValueList.toArray(unfValues);

            logger.fine("Attempting to calculate new UNF from total of " + unfValueList.size() + " file-level signatures.");
            String datasetUnfValue = null;
            try {
                datasetUnfValue = UNFUtil.calculateUNF(unfValues);
            } catch (IOException ex) {
                // It's unclear how to exercise this IOException.
                logger.warning("IO Exception: Failed to recalculate the UNF for the dataset version id=" + version.getId());
            } catch (UnfException uex) {
                // It's unclear how to exercise this UnfException.
                logger.warning("UNF Exception: Failed to recalculate the UNF for the dataset version id=" + version.getId());
            }

            if (datasetUnfValue != null) {
                version.setUNF(datasetUnfValue);
                logger.fine("Recalculated the UNF for the dataset version id=" + version.getId() + ", new signature: " + datasetUnfValue);
            }
        } else {
            // Of course if no files in the version have UNFs, we need to make sure
            // that the version has the NULL UNF too.
            // Otherwise, the version will still have a UNF if the user deletes
            // all the tabular files from the version!
            version.setUNF(null);
        }
    }

    public static List<String> getUnfValuesOfFiles(DatasetVersion version) {
        List<String> unfValueList = new ArrayList<>();
        if (version == null) {
            return unfValueList;
        }
        Iterator<FileMetadata> itfm = version.getFileMetadatas().iterator();
        while (itfm.hasNext()) {
            FileMetadata fileMetadata = itfm.next();
            if (fileMetadata != null
                    && fileMetadata.getDataFile() != null
                    && fileMetadata.getDataFile().isTabularData()
                    && fileMetadata.getDataFile().getUnf() != null) {
                String varunf = fileMetadata.getDataFile().getUnf();
                unfValueList.add(varunf);
            }
        }
        return unfValueList;
    }

    public static boolean shouldHaveUnf(DatasetVersion version) {
        if (version == null) {
            return false;
        }
        List<String> values = getUnfValuesOfFiles(version);
        logger.fine("UNF values for files from Dataset version " + version.getSemanticVersion() + " from " + version.getDataset().getGlobalId() + ": " + values);
        if (values.size() > 0) {
            return true;
        } else {
            return false;
        }

    }
}
