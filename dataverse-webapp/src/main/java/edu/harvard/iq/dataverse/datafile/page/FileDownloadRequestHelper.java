package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.datafile.FilePermissionsService;
import edu.harvard.iq.dataverse.license.dto.RestrictedTermsOfUseDTO;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ViewScoped
@Named("FileDownloadRequestHelper")
public class FileDownloadRequestHelper implements Serializable {
    
    @EJB
    private FilePermissionsService filePermissionsService;

    private List<DataFile> filesForRequestAccess = new ArrayList<DataFile>();
    
    
    public void replaceRequestAccessWithSingleFile(DataFile dataFile) {
        this.filesForRequestAccess.clear();
        this.filesForRequestAccess.add(dataFile);
    }

    public void clearRequestAccessFiles() {
        this.filesForRequestAccess.clear();
    }

    public void addFileForRequestAccess(DataFile dataFile) {
        this.filesForRequestAccess.add(dataFile);
    }

    /**
     * Returns restricted files grouped by distinct terms of uses
     * that user requested to access.
     * Terms of use are showed to the user in dialog
     * window so he can decide whether it accepts terms
     * associated with requested files.
     */
    public Map<RestrictedTermsOfUseDTO, List<DataFile>> getFilesForRequestAccessByTermsOfUse() {
        Map<RestrictedTermsOfUseDTO, List<DataFile>> distinctTermsOfUse = new HashMap<RestrictedTermsOfUseDTO, List<DataFile>>();
        
        for (DataFile file: filesForRequestAccess) {
            FileTermsOfUse fileTermsOfUse = file.getFileMetadata().getTermsOfUse();
            RestrictedTermsOfUseDTO restrictedTermsOfUse = new RestrictedTermsOfUseDTO(
                    fileTermsOfUse.getRestrictType(), fileTermsOfUse.getRestrictCustomText());
            
            distinctTermsOfUse.putIfAbsent(restrictedTermsOfUse, new ArrayList<>());
            distinctTermsOfUse.get(restrictedTermsOfUse).add(file);
        }
        
        return distinctTermsOfUse;
    }
    
    public void requestAccessIndirect() {
        //Called when there are multiple files and no popup
        // or there's a popup with sigular or multiple files
        // The list of files for Request Access is set in the Dataset Page when
        // user clicks the request access button in the files fragment
        // (and has selected one or more files)
        filePermissionsService.requestAccessToFiles(this.filesForRequestAccess);
    }
}
