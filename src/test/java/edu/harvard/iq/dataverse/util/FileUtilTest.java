package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Embargo;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.UtilIT;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.FileUtil.FileCitationExtension;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Ignore;
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

        @Parameter
        public String expectedString;

        @Parameter(1)
        public String originalName;

        @Parameter(2)
        public String newExtension;

        @Parameters
        public static Collection data() {
            return Arrays.asList(new Object[][] {
                // functional approach: what should the method do
                // replace no extension with an empty extension
                { "no-extension.", "no-extension", ""},

                // replace extension x with same extension
                { "extension.x", "extension.x", "x" },

                // replace extension x with another extension y
                { "extension.y", "extension.x", "y" },

                // interface approach: what are possible inputs
                // will not pass as null is not handled
                //{ null, null, null },
                //{ null, null, "" },
                //{ null, null, "y" },

                { ".null", "", null },
                { ".", "", "" },
                { ".y", "", "y" },
            });
        }

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
            License license = new License("CC0", "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.", URI.create("http://creativecommons.org/publicdomain/zero/1.0"), URI.create("/resources/images/cc0.png"), true);
            license.setDefault(true);
            termsOfUseAndAccess.setLicense(license);
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
            License license = new License("CC0", "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.", URI.create("http://creativecommons.org/publicdomain/zero/1.0"), URI.create("/resources/images/cc0.png"), true);
            license.setDefault(true);
            termsOfUseAndAccess.setLicense(license);
            termsOfUseAndAccess.setTermsOfUse("be excellent to each other");
            dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
            assertEquals(false, FileUtil.isDownloadPopupRequired(dsv1));
        }

        @Test
        public void testIsDownloadPopupRequiredHasTermsOfUseAndNoneLicense() {
            DatasetVersion dsv1 = new DatasetVersion();
            dsv1.setVersionState(DatasetVersion.VersionState.RELEASED);
            TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
            termsOfUseAndAccess.setLicense(null);
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
            restrictedFileMetadata.setDataFile(new DataFile());
            assertEquals(false, FileUtil.isPubliclyDownloadable(restrictedFileMetadata));

            FileMetadata nonRestrictedFileMetadata = new FileMetadata();
            nonRestrictedFileMetadata.setDataFile(new DataFile());
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
            nonRestrictedFileMetadata.setDataFile(new DataFile());
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
        public void testIsPubliclyDownloadable3() {

            FileMetadata embargoedFileMetadata = new FileMetadata();
            DataFile df = new DataFile();
            Embargo e = new Embargo();
            e.setDateAvailable(LocalDate.now().plusDays(4) );
            df.setEmbargo(e);
            embargoedFileMetadata.setDataFile(df);
            DatasetVersion dsv = new DatasetVersion();
            dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
            embargoedFileMetadata.setDatasetVersion(dsv);
            Dataset dataset = new Dataset();
            dsv.setDataset(dataset);
            embargoedFileMetadata.setRestricted(false);
            assertEquals(false, FileUtil.isPubliclyDownloadable(embargoedFileMetadata));
        }

        @Test
        public void testgetFileDownloadUrl() {
            Long fileId = 42l;
            Long fileMetadataId = 2L;
            assertEquals("/api/access/datafile/42", FileUtil.getFileDownloadUrlPath(null, fileId, false, null));
            assertEquals("/api/access/datafile/42", FileUtil.getFileDownloadUrlPath("", fileId, false, null));
            assertEquals("/api/access/datafile/bundle/42", FileUtil.getFileDownloadUrlPath("bundle", fileId, false, null));
            assertEquals("/api/access/datafile/bundle/42?fileMetadataId=2", FileUtil.getFileDownloadUrlPath("bundle", fileId, false, fileMetadataId));
            assertEquals("/api/access/datafile/42?format=original", FileUtil.getFileDownloadUrlPath("original", fileId, false, null));
            assertEquals("/api/access/datafile/42?format=RData", FileUtil.getFileDownloadUrlPath("RData", fileId, false, null));
            assertEquals("/api/access/datafile/42/metadata", FileUtil.getFileDownloadUrlPath("var", fileId, false, null));
            assertEquals("/api/access/datafile/42/metadata?fileMetadataId=2", FileUtil.getFileDownloadUrlPath("var", fileId, false, fileMetadataId));
            assertEquals("/api/access/datafile/42?format=tab", FileUtil.getFileDownloadUrlPath("tab", fileId, false, null));
            assertEquals("/api/access/datafile/42?format=tab&gbrecs=true", FileUtil.getFileDownloadUrlPath("tab", fileId, true, null));
            assertEquals("/api/access/datafile/42?gbrecs=true", FileUtil.getFileDownloadUrlPath(null, fileId, true, null));
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
        public void testDetermineFileTypeByExtension() {
            File file = new File("src/main/webapp/resources/images/cc0.png");
            if (file.exists()) {
                try {
                    assertEquals("image/png", FileUtil.determineFileType(file, "cc0.png"));
                } catch (IOException ex) {
                    Logger.getLogger(FileUtilTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                fail("File does not exist: " + file.toPath().toString());
            }
        }
        
        @Test
        public void testDetermineFileTypeFromName() {
            //Verify that name of the local file isn't used in determining the type (as we often use *.tmp when the real name has a different extension)
            try {
                File file = File.createTempFile("empty", "png");
                assertEquals("text/plain", FileUtil.determineFileType(file, "something.txt"));
            } catch (IOException ex) {
                Logger.getLogger(FileUtilTest.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        @Test
        public void testDetermineFileTypeByName() {
            File file = new File("src/test/resources/fileutil/Makefile");
            if (file.exists()) {
                try {
                    assertEquals("text/x-makefile", FileUtil.determineFileType(file, "Makefile"));
                } catch (IOException ex) {
                    Logger.getLogger(FileUtilTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                fail("File does not exist: " + file.toPath().toString());
            }
        }
        
        @Test
        public void testDetermineFileTypeFromNameLocalFile() {
            //Verify that name of the local file isn't used in determining the type (as we often use *.tmp when the real name has a different extension)
            try {
                File file = File.createTempFile("empty", "png");
                assertEquals("text/plain", FileUtil.determineFileType(file, "something.txt"));
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
