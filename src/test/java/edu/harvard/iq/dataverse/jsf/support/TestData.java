package edu.harvard.iq.dataverse.jsf.support;

import java.nio.file.Path;

/**
 * Absolute paths to the fixture files in src/test/resources/jsf/, plus the
 * default metadata for datasets the tests create.
 *
 * Always reference uploads through this class rather than with a relative
 * path: relative paths resolve against whatever directory the JVM was
 * launched from, absolute ones don't.
 */
public final class TestData {

    private static final Path ROOT = JsfTestConfig.PROJECT_DIR.resolve("src/test/resources/jsf");

    public static final Path SAMPLE_TEXT = file("sample-dataset-file.txt");
    public static final Path SAMPLE_TEXT_2 = file("sample-dataset-file-2.txt");
    public static final Path REPLACEMENT_TEXT = file("replaced-sample-dataset-file.txt");
    public static final Path CSV = file("sample-data.csv");
    public static final Path ZIP = file("demo-archive.zip");
    public static final Path PDF = file("demo-document.pdf");
    public static final Path R_SCRIPT = file("demo-code.R");
    public static final Path RO_CRATE = file("ro-crate-metadata.json");
    public static final Path STATA = file("demo-data.dta");
    public static final Path R_DATA = file("demo-data.RData");
    public static final Path SPSS = file("demo-data.sav");
    public static final Path EXCEL = file("demo-data.xlsx");

    public static final Path LOGO_IMAGE = file("theme/logo.png");
    public static final Path THUMBNAIL_IMAGE = file("theme/thumbnail.png");
    public static final Path FOOTER_IMAGE = file("theme/footer.png");

    /** "Other" is part of the stock citation block's Subject vocabulary on every install. */
    public static final String DEFAULT_SUBJECT = "Other";
    public static final String DEFAULT_DESCRIPTION = "Created by the Playwright suite; deleted automatically.";

    private TestData() {
    }

    public static Path file(String name) {
        return ROOT.resolve(name);
    }
}
