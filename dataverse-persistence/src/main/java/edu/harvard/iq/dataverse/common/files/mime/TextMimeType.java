package edu.harvard.iq.dataverse.common.files.mime;

import com.google.common.collect.Lists;

import java.util.List;

public enum TextMimeType {

    TSV("text/tsv"),
    TSV_ALT("text/tab-separated-values"),
    CSV("text/csv"),
    CSV_ALT("text/comma-separated-values"),
    PLAIN_TEXT("text/plain"),
    FIXED_FIELD("text/x-fixed-field"),
    NETWORK_GRAPHML("text/xml-graphml"),
    STATA_SYNTAX("text/x-stata-syntax"),
    SPSS_CCARD("text/x-spss-syntax"),
    SAS_SYNTAX("text/x-sas-syntax");

    private String mimeValue;

    TextMimeType(String mimeType) {
        this.mimeValue = mimeType;
    }

    public String getMimeValue() {
        return mimeValue;
    }

    public static List<TextMimeType> retrieveIngestableMimes() {
        return Lists.newArrayList(TextMimeType.CSV,
                                  TextMimeType.CSV_ALT,
                                  TextMimeType.TSV,
                                  TextMimeType.TSV_ALT);
    }
}
