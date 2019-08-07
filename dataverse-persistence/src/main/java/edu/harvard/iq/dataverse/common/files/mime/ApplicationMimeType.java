package edu.harvard.iq.dataverse.common.files.mime;

import com.google.common.collect.Lists;

import java.util.List;

public enum ApplicationMimeType {

    FITS("application/fits"),
    SPSS_SAV("application/x-spss-sav"),
    SPSS_POR("application/x-spss-por"),
    R_SYNTAX("application/x-r-syntax"),
    SAS_SYSTEM("application/x-sas-system"),
    DOCUMENT_PDF("application/pdf"),
    SAS_TRANSPORT("application/x-sas-transport"),
    GEO_SHAPE("application/zipped-shapefile"),
    UNDETERMINED_DEFAULT("application/octet-stream"),
    UNDETERMINED_BINARY("application/binary"),
    ZIP("application/zip"),
    STATA("application/x-stata"),
    STATA13("application/x-stata-13"),
    STATA14("application/x-stata-14"),
    STATA15("application/x-stata-15"),
    RDATA("application/x-rlang-transport"),
    DOCUMENT_MSWORD("application/msword"),
    DOCUMENT_MSEXCEL("application/vnd.ms-excel"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    DOCUMENT_MSWORD_OPENXML("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private String mimeValue;

    ApplicationMimeType(String mimeType) {
        this.mimeValue = mimeType;
    }

    public String getMimeValue() {
        return mimeValue;
    }

    public static List<ApplicationMimeType> retrieveIngestableMimes() {
        return Lists.newArrayList(ApplicationMimeType.STATA,
                                  ApplicationMimeType.STATA13,
                                  ApplicationMimeType.STATA14,
                                  ApplicationMimeType.STATA15,
                                  ApplicationMimeType.RDATA,
                                  ApplicationMimeType.XLSX,
                                  ApplicationMimeType.SPSS_SAV,
                                  ApplicationMimeType.SPSS_POR);
    }
}
