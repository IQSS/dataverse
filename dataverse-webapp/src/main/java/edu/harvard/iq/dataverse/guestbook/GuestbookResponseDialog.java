package edu.harvard.iq.dataverse.guestbook;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadHelper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.commons.lang.StringUtils;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.Serializable;

@ViewScoped
@Named("GuestbookResponseDialog")
public class GuestbookResponseDialog implements Serializable {

    @Inject
    private GuestbookResponseServiceBean guestbookResponseService;
    @Inject
    private FileDownloadHelper fileDownloadHelper;
    @Inject
    private DataverseSession session;
    
    
    private DatasetVersion workingVersion;
    
    private GuestbookResponse guestbookResponse;
    
    
    // -------------------- GETTERS --------------------
    
    public GuestbookResponse getGuestbookResponse() {
        return guestbookResponse;
    }
    public Guestbook getGuestbook() {
        return guestbookResponse.getGuestbook();
    }
    
    // -------------------- LOGIC --------------------
    
    public void initForDatasetVersion(DatasetVersion workingVersion) {
        this.workingVersion = workingVersion;
        if (isDownloadPopupRequired()) {
            guestbookResponse = guestbookResponseService.initGuestbookResponseForFragment(workingVersion, null, session);
        }
    }
    
    public boolean isDownloadPopupRequired() {
        return FileUtil.isDownloadPopupRequired(workingVersion);
    }

    public String saveAndStartDownload() {
        fileDownloadHelper.writeGuestbookAndStartDownloadAccordingToType(guestbookResponse);
        return StringUtils.EMPTY;
    }
    
}
