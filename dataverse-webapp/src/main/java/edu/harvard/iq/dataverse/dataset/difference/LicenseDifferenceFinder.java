package edu.harvard.iq.dataverse.dataset.difference;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import java.util.List;

@Stateless
public class LicenseDifferenceFinder {

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public LicenseDifferenceFinder() {
    }

    // -------------------- LOGIC --------------------
    public List<DatasetFileTermDifferenceItem> getLicenseDifference(List<FileMetadata> newVersionFileMetadata, List<FileMetadata> previousVersionFileMetadata) {
        return getFileTermDiffs(newVersionFileMetadata, previousVersionFileMetadata);
    }

    // -------------------- PRIVATE ---------------------
    private boolean areFileTermsEqual(FileTermsOfUse termsOriginal, FileTermsOfUse termsNew) {
        if (termsOriginal.getTermsOfUseType() != termsNew.getTermsOfUseType()) {
            return false;
        }
        if (termsOriginal.getTermsOfUseType() == FileTermsOfUse.TermsOfUseType.LICENSE_BASED) {
            return termsOriginal.getLicense().getId().equals(termsNew.getLicense().getId());
        }
        if (termsOriginal.getTermsOfUseType() == FileTermsOfUse.TermsOfUseType.RESTRICTED) {
            return termsOriginal.getRestrictType() == termsNew.getRestrictType() &&
                    StringUtils.equals(termsOriginal.getRestrictCustomText(), termsNew.getRestrictCustomText());
        }
        return true;
    }

    private List<DatasetFileTermDifferenceItem> getFileTermDiffs(List<FileMetadata> newVersionFileMetadata, List<FileMetadata> previousVersionFileMetadata) {
        List<DatasetFileTermDifferenceItem>  fileTermDiffs = Lists.newArrayList();

        for (FileMetadata fmdo : previousVersionFileMetadata) {
            for (FileMetadata fmdn : newVersionFileMetadata) {
                if (fmdo.getDataFile().equals(fmdn.getDataFile())) {
                    if (!areFileTermsEqual(fmdo.getTermsOfUse(), fmdn.getTermsOfUse())) {
                        DataFile dataFile = fmdo.getDataFile();
                        FileSummary fileSummary = new FileSummary(dataFile.getId().toString(), dataFile.getChecksumType(), dataFile.getChecksumValue());
                        fileTermDiffs.add(new DatasetFileTermDifferenceItem(fileSummary, fmdn.getLabel(), fmdo.getTermsOfUse(), fmdn.getTermsOfUse()));
                    }
                    break;
                }
            }
        }
        return fileTermDiffs;
    }

}
