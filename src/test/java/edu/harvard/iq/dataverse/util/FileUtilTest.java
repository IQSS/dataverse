package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.util.FileUtil.FileCitationExtension;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class FileUtilTest {

    @RunWith(Parameterized.class)
    public static class FileUtilParamTest {

        @Parameters
        public static Collection data() {
            return Arrays.asList(new Object[][] {
                { null, null, null },

                { "trees.png-endnote.xml", "trees.png", FileUtil.FileCitationExtension.ENDNOTE },
                { "trees.png.ris", "trees.png", FileUtil.FileCitationExtension.RIS },
                { "trees.png.bib", "trees.png", FileUtil.FileCitationExtension.BIBTEX },
                { null, "trees.png", null },

                { "50by1000-endnote.xml", "50by1000.tab", FileUtil.FileCitationExtension.ENDNOTE },
                { "50by1000.ris", "50by1000.tab", FileUtil.FileCitationExtension.RIS },
                { "50by1000.bib", "50by1000.tab", FileUtil.FileCitationExtension.BIBTEX }
            });
        }

        @Parameter
        public String expectedFileName;

        @Parameter(1)
        public String actualFileName;

        @Parameter(2)
        public FileCitationExtension citationExtension;

        @Test
        public void testGetCiteDataFileFilename() {
            assertEquals(expectedFileName, FileUtil.getCiteDataFileFilename(actualFileName, citationExtension));
        }
    }

    @RunWith(Parameterized.class)
    public static class FileUtilParamTest2 {

        @Parameters
        public static Collection data() {
            return Arrays.asList(new Object[][] {
                // functional approach: what should the method do
                // replace no extension with an empty extension
                //{ "no-extension", "no-extension", ""}, // will not pass as empty extensions are not handled

                // replace extension x with same extension
                { "extension.x", "extension.x", "x" },

                // replace extension x with another extension y
                { "extension.y", "extension.x", "y" },

                // interface approach: what are possible inputs
                // will not pass as empty or null originalNames are not handled
                //{ null, null, null },
                //{ null, null, "" },
                //{ null, null, "y" },

                //{ "", "", null },
                //{ "", "", "" },
                //{ "", "", "y" },
            });
        }

        @Parameter
        public String expectedString;

        @Parameter(1)
        public String originalName;

        @Parameter(2)
        public String newExtension;

        @Test
        public void testReplaceExtension() {
            assertEquals(expectedString, FileUtil.replaceExtension(originalName, newExtension));
        }

    }

    public static class FileUtilNoParamTest {
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
            assertEquals("/api/access/datafile/42/metadata", FileUtil.getFileDownloadUrlPath("var", fileId, false));
            assertEquals("/api/access/datafile/42?format=tab", FileUtil.getFileDownloadUrlPath("tab", fileId, false));
            assertEquals("/api/access/datafile/42?format=tab&gbrecs=true", FileUtil.getFileDownloadUrlPath("tab", fileId, true));
            assertEquals("/api/access/datafile/42?gbrecs=true", FileUtil.getFileDownloadUrlPath(null, fileId, true));
        }

        @Test
        public void testGetPublicDownloadUrl() {
            assertEquals(null, FileUtil.getPublicDownloadUrl(null, null, null));
            assertEquals("https://demo.dataverse.org/api/access/datafile/:persistentId?persistentId=doi:10.5072/FK2/TLU3EP", FileUtil.getPublicDownloadUrl("https://demo.dataverse.org", "doi:10.5072/FK2/TLU3EP", 33L)); //pid before fileId
            assertEquals("https://demo.dataverse.org/api/access/datafile/:persistentId?persistentId=doi:10.5072/FK2/TLU3EP", FileUtil.getPublicDownloadUrl("https://demo.dataverse.org", "doi:10.5072/FK2/TLU3EP", null));
            assertEquals("https://demo.dataverse.org/api/access/datafile/33", FileUtil.getPublicDownloadUrl("https://demo.dataverse.org", null, 33L)); //pid before fileId
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

        /*
         * The method below has been removed from FileUtil
        @Test
        public void testRescaleImage() throws IOException {
            assertEquals(null, FileUtil.rescaleImage(null));
            File file = new File("src/main/webapp/resources/images/cc0.png");
            String imageAsBase64actual = FileUtil.rescaleImage(file);
            String imageAsBase64expected = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAQCAYAAABQrvyxAAABxUlEQVR42tVWu42DQBR0By6BBpBcAqIARAmuwCInIXFEgOnAJbgEl+DAEYlJQPgiRHJ3nITeaWzNagEjnUEnm5WeePt/M/N2l8XiXmSmdg9+roUgeh2e54lhGC20QRBI0zRS17WYpvlWKqjAy7JUHa7ryul0urXv93vV7jiOZFkmq9Xq/QDAX6/Xqg4Ax+NR1QEKYzabjZzP555CHGtZljLWSQb34zy9DQTqfU8BQMosl0sVbJdhljRNb/WqqsT3/RYA2BAAnaQuAPQjbScpgC8ZINMICIvDxwZ6EFEUyeVyaQGgD/BQ43A4tAJEG/pQdMDcm3uMBtA53S2fGxMggiqKYnBhqAl7JpiR52oYANIKLMLX0+ARAJwdMEwlMIYqoA6fSu52O7U+farDcfxOSiG971EK5XneSyEETLUIhKnClGJwOE86eSRJP/D/dojxHmy32x4AMMpAqBwBEAQB8ObB3joA3majrlEwNnSNEhTUwC2kL8QU4pvBoLEGUwLzQBJB8oLAnMkp1H3IuBjamKcMFOzbtv1+DxkL2Ov+SgAECn4nkiSROI4lDMOXmgLwU3/92ervz5sqH9erXIv8paYAzNl+AXxcLdKhqOWiAAAAAElFTkSuQmCC";
            assertEquals(imageAsBase64expected, imageAsBase64actual);
        }*/

        @Test
        public void testDetermineFileType() {
            File file = new File("src/main/webapp/resources/images/cc0.png");
            try {
                assertEquals("image/png", FileUtil.determineFileType(file, "cc0.png"));
            } catch (IOException ex) {
                Logger.getLogger(FileUtilTest.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        // isThumbnailSuppported() has been moved from DataFileService to FileUtil:
        /**
         * Expect that {@code null}, a DataFile without content type and a DataFile
         * with bogus content type are not files that thumbnails can be created for.
         * @throws Exception when the test is in error.
         */
        @Test
        public void testIsThumbnailSupported() throws Exception {
            // null file:
            assertFalse(FileUtil.isThumbnailSupported(null));
            // file with no content type:
            DataFile filewNoContentType = new DataFile("");
            filewNoContentType.setStorageIdentifier("");
            assertFalse(FileUtil.isThumbnailSupported(filewNoContentType));
            DataFile filewBogusContentType = new DataFile("");
            filewBogusContentType.setStorageIdentifier("");
            assertFalse(FileUtil.isThumbnailSupported(filewBogusContentType));
        }
    }
}
