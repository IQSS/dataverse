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

package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class FileMetadataUtil implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(FileMetadataUtil.class.getCanonicalName());

    //Delete the filemetadata from the list if and only if it is in the list
    public static void removeFileMetadataFromList(Collection<FileMetadata> collection, FileMetadata fmToDelete) {
        // With an id, the standard remove will work
        if (fmToDelete.getId() != null) {
            collection.remove(fmToDelete);
        } else {
            Iterator<FileMetadata> fmit = collection.iterator();
            while (fmit.hasNext()) {
                FileMetadata fmd = fmit.next();
                // If not, we can remove based on a match based on the id of the related
                // datafile
                if (fmToDelete.getDataFile().getStorageIdentifier().equals(fmd.getDataFile().getStorageIdentifier())) {
                    // and a match on the datasetversion
                    if(fmToDelete.getDatasetVersion() == null) {
                        //not yet associated with a version (i.e. deleting from the upload screen), so the match on datafile is good enough 
                        fmit.remove();
                    } else if (fmToDelete.getDatasetVersion().getId() == null) {
                        // If the fmd to delete is for a datasetversion with no id, we assume match to
                        // any other fmd with a datasetversion with no id (since there should be only
                        // one)
                        // Otherwise, we don't delete anything (this fmd hasn't been persisted and isn't
                        // in the list.
                        if (fmd.getDatasetVersion().getId() == null) {
                            fmit.remove();
                            break;
                        }
                    } else if (fmToDelete.getDatasetVersion().getId().equals(fmd.getDatasetVersion().getId())) {
                        fmit.remove();
                        break;
                    }
                }
            }
        }
    }

    //Delete datafile from the list even if it's id is null (hasn't yet been persisted)
    public static void removeDataFileFromList(List<DataFile> dfList, DataFile dfToDelete) {
        // With an id, the standard remove will work
        if (dfToDelete.getId() != null) {
            dfList.remove(dfToDelete);
        } else {
            Iterator<DataFile> dfit = dfList.iterator();
            while (dfit.hasNext()) {
                DataFile df = dfit.next();
                if (dfToDelete.getStorageIdentifier().equals(df.getStorageIdentifier())) {
                    dfit.remove();
                    break;
                }
            }
        }
    }

    public static FileMetadata getFmdForFileInEditVersion(FileMetadata fmd, DatasetVersion editVersion) {
        for(FileMetadata editFmd: editVersion.getFileMetadatas()) {
            if(editFmd.getDataFile().getId().equals(fmd.getDataFile().getId())) {
                return editFmd;
            }
        }
        return null;
    }
}
