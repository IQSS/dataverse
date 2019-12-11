package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.util.FileUtil.ApiBatchDownloadType;
import edu.harvard.iq.dataverse.util.FileUtil.ApiDownloadType;

/**
 * Enum that contains information on what kind of
 * download for file/files was requested by the user.
 * <p>
 * Note that is different than {@link GuestbookResponse#getDownloadtype()}.
 * {@link GuestbookResponse#getDownloadtype()} contains less detailed information.
 * 
 * @author madryk
 */
public enum DownloadType {
    DOWNLOAD(ApiDownloadType.DEFAULT, ApiBatchDownloadType.DEFAULT),
    ORIGINAL(ApiDownloadType.ORIGINAL, ApiBatchDownloadType.ORIGINAL),
    TAB(ApiDownloadType.TAB, null),
    RDATA(ApiDownloadType.RDATA, null),
    VAR(ApiDownloadType.VAR, null),
    SUBSET(null, null),
    WORLDMAP(null, null),
    EXTERNALTOOL(null, null),
    PACKAGE(null, null);
    
    private ApiDownloadType apiDownloadEquivalent;
    private ApiBatchDownloadType apiBatchDownloadEquivalent;
    
    
    // -------------------- CONSTRUCTORS --------------------
    
    private DownloadType(ApiDownloadType apiDownloadEquivalent, ApiBatchDownloadType apiBatchDownloadEquivalent) {
        this.apiDownloadEquivalent = apiDownloadEquivalent;
        this.apiBatchDownloadEquivalent = apiBatchDownloadEquivalent;
    }

    // -------------------- GETTERS --------------------
    
    /**
     * Returns type of api download that is compatible with
     * this download type or {@literal null} if download type
     * can't be performed via download api
     */
    public ApiDownloadType getApiDownloadEquivalent() {
        return apiDownloadEquivalent;
    }

    /**
     * Returns type of batch api download that is compatible with
     * this download type or {@literal null} if download type
     * can't be performed via batch download api
     */
    public ApiBatchDownloadType getApiBatchDownloadEquivalent() {
        return apiBatchDownloadEquivalent;
    }
    
    // -------------------- LOGIC --------------------
    
    /**
     * Returns true if this download type can be performed by api download
     */
    public boolean isCompatibleWithApiDownload() {
        return apiDownloadEquivalent != null;
    }
    
    /**
     * Returns true if this download type can be performed by batch api download
     */
    public boolean isCompatibleWithBatchApiDownload() {
        return apiBatchDownloadEquivalent != null;
    }
}