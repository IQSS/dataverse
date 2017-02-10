package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FileUtilTest {

    @Test
    public void testGetCiteDataFileFilename() {

        assertEquals(null, FileUtil.getCiteDataFileFilename(null, null));

        FileMetadata png = new FileMetadata();
        png.setLabel("trees.png");
        assertEquals("trees.png-endnote.xml", FileUtil.getCiteDataFileFilename(png, FileUtil.FileCitationExtension.ENDNOTE));
        assertEquals("trees.png.ris", FileUtil.getCiteDataFileFilename(png, FileUtil.FileCitationExtension.RIS));
        assertEquals("trees.png.bib", FileUtil.getCiteDataFileFilename(png, FileUtil.FileCitationExtension.BIBTEX));
        assertEquals(null, FileUtil.getCiteDataFileFilename(png, null));

        FileMetadata tabular = new FileMetadata();
        tabular.setLabel("50by1000.tab");
        assertEquals("50by1000-endnote.xml", FileUtil.getCiteDataFileFilename(tabular, FileUtil.FileCitationExtension.ENDNOTE));
        assertEquals("50by1000.ris", FileUtil.getCiteDataFileFilename(tabular, FileUtil.FileCitationExtension.RIS));
        assertEquals("50by1000.bib", FileUtil.getCiteDataFileFilename(tabular, FileUtil.FileCitationExtension.BIBTEX));
    }

    @Test
    public void testIsDownloadPopupRequiredNull() {
        assertEquals(false, FileUtil.isDownloadPopupRequired(null));
    }

    @Test
    public void testIsDownloadPopupRequiredDraft() {
        Dataset dataset = new Dataset();
        DatasetVersion dsv1 = dataset.getEditVersion();
        assertEquals(DatasetVersion.VersionState.DRAFT, dsv1.getVersionState());
        assertEquals(false, FileUtil.isDownloadPopupRequired(dsv1));
    }

    @Test
    public void testIsDownloadPopupRequiredLicenseCC0() {
        DatasetVersion dsv1 = new DatasetVersion();
        dsv1.setVersionState(DatasetVersion.VersionState.RELEASED);
        TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
        termsOfUseAndAccess.setLicense(TermsOfUseAndAccess.License.CC0);
        dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
        assertEquals(false, FileUtil.isDownloadPopupRequired(dsv1));
    }

    @Test
    public void testIsDownloadPopupRequiredHasTermsOfUseAndCc0License() {
        DatasetVersion dsv1 = new DatasetVersion();
        dsv1.setVersionState(DatasetVersion.VersionState.RELEASED);
        TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
        /**
         * @todo Ask if setting the license to CC0 should be enough to not show
         * the popup when the are Terms of Use. This feels like a bug since the
         * Terms of Use should probably be shown.
         */
        termsOfUseAndAccess.setLicense(TermsOfUseAndAccess.License.CC0);
        termsOfUseAndAccess.setTermsOfUse("be excellent to each other");
        dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
        assertEquals(false, FileUtil.isDownloadPopupRequired(dsv1));
    }

    @Test
    public void testIsDownloadPopupRequiredHasTermsOfUseAndNoneLicense() {
        DatasetVersion dsv1 = new DatasetVersion();
        dsv1.setVersionState(DatasetVersion.VersionState.RELEASED);
        TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
        termsOfUseAndAccess.setLicense(TermsOfUseAndAccess.License.NONE);
        termsOfUseAndAccess.setTermsOfUse("be excellent to each other");
        dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
        assertEquals(true, FileUtil.isDownloadPopupRequired(dsv1));
    }

    @Test
    public void testIsDownloadPopupRequiredHasTermsOfAccess() {
        DatasetVersion dsv1 = new DatasetVersion();
        dsv1.setVersionState(DatasetVersion.VersionState.RELEASED);
        TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
        termsOfUseAndAccess.setTermsOfAccess("Terms of *Access* is different than Terms of Use");
        dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
        assertEquals(true, FileUtil.isDownloadPopupRequired(dsv1));
    }

    @Test
    public void testIsDownloadPopupRequiredHasGuestBook() {
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        Dataset dataset = new Dataset();
        datasetVersion.setDataset(dataset);
        Guestbook guestbook = new Guestbook();
        guestbook.setEnabled(true);
        dataset.setGuestbook(guestbook);
        Dataverse dataverse = new Dataverse();
        guestbook.setDataverse(dataverse);
        assertEquals(true, FileUtil.isDownloadPopupRequired(datasetVersion));
    }

    @Test
    public void testIsPubliclyDownloadable() {
        assertEquals(false, FileUtil.isPubliclyDownloadable(null));

        FileMetadata restrictedFileMetadata = new FileMetadata();
        restrictedFileMetadata.setRestricted(true);
        assertEquals(false, FileUtil.isPubliclyDownloadable(restrictedFileMetadata));

        FileMetadata nonRestrictedFileMetadata = new FileMetadata();
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
        nonRestrictedFileMetadata.setDatasetVersion(dsv);
        Dataset dataset = new Dataset();
        dsv.setDataset(dataset);
        nonRestrictedFileMetadata.setRestricted(false);
        assertEquals(true, FileUtil.isPubliclyDownloadable(nonRestrictedFileMetadata));
    }

    @Test
    public void testIsPubliclyDownloadable2() {

        FileMetadata nonRestrictedFileMetadata = new FileMetadata();
        DatasetVersion dsv = new DatasetVersion();
        TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
        termsOfUseAndAccess.setTermsOfUse("be excellent to each other");
        dsv.setTermsOfUseAndAccess(termsOfUseAndAccess);
        dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
        nonRestrictedFileMetadata.setDatasetVersion(dsv);
        Dataset dataset = new Dataset();
        dsv.setDataset(dataset);
        nonRestrictedFileMetadata.setRestricted(false);
        assertEquals(false, FileUtil.isPubliclyDownloadable(nonRestrictedFileMetadata));
    }

    @Test
    public void testgetFileDownloadUrl() {
        Long fileId = 42l;
        assertEquals("/api/access/datafile/42", FileUtil.getFileDownloadUrlPath(null, fileId, false));
        assertEquals("/api/access/datafile/42", FileUtil.getFileDownloadUrlPath("", fileId, false));
        assertEquals("/api/access/datafile/bundle/42", FileUtil.getFileDownloadUrlPath("bundle", fileId, false));
        assertEquals("/api/access/datafile/42?format=original", FileUtil.getFileDownloadUrlPath("original", fileId, false));
        assertEquals("/api/access/datafile/42?format=RData", FileUtil.getFileDownloadUrlPath("RData", fileId, false));
        assertEquals("/api/meta/datafile/42", FileUtil.getFileDownloadUrlPath("var", fileId, false));
        assertEquals("/api/access/datafile/42?format=tab", FileUtil.getFileDownloadUrlPath("tab", fileId, false));
        assertEquals("/api/access/datafile/42?format=tab&gbrecs=true", FileUtil.getFileDownloadUrlPath("tab", fileId, true));
        assertEquals("/api/access/datafile/42?gbrecs=true", FileUtil.getFileDownloadUrlPath(null, fileId, true));
    }

    @Test
    public void testGetPublicDownloadUrl() {
        assertEquals(null, FileUtil.getPublicDownloadUrl(null, null));
        assertEquals("https://demo.dataverse.org/api/access/datafile/42", FileUtil.getPublicDownloadUrl("https://demo.dataverse.org", 42l));
    }

    @Test
    public void testGenerateOriginalExtension() {
        assertEquals("", FileUtil.generateOriginalExtension("foo"));
        // uh-oh, NullPointerException
//        assertEquals("", FileUtil.generateOriginalExtension(null));
        assertEquals(".sav", FileUtil.generateOriginalExtension("application/x-spss-sav"));
        assertEquals(".por", FileUtil.generateOriginalExtension("application/x-spss-por"));
        assertEquals(".dta", FileUtil.generateOriginalExtension("application/x-stata"));
        assertEquals(".RData", FileUtil.generateOriginalExtension("application/x-rlang-transport"));
        assertEquals(".csv", FileUtil.generateOriginalExtension("text/csv"));
        assertEquals(".xlsx", FileUtil.generateOriginalExtension("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
