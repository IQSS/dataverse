package edu.harvard.iq.dataverse.common.files.mime;

public enum PackageMimeType {
    /**
     * Per https://en.wikipedia.org/wiki/Media_type#Vendor_tree just "dataverse"
     * should be fine.
     *
     * @todo Consider registering this at http://www.iana.org/form/media-types
     * or switch to "prs" which "includes media types created experimentally or
     * as part of products that are not distributed commercially" according to
     * the page URL above.
     */
    DATAVERSE_PACKAGE("application/vnd.dataverse.file-package");

    private String mimeValue;

    PackageMimeType(String mimeType) {
        this.mimeValue = mimeType;
    }

    public String getMimeValue() {
        return mimeValue;
    }
}
