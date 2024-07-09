package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.FileUtil.FileCitationExtension;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class FileUtilTest {
    
    static Stream<Arguments> dataFilenames() {
        return Stream.of(
            Arguments.of(null, null, null),
            Arguments.of("trees.png-endnote.xml", "trees.png", FileUtil.FileCitationExtension.ENDNOTE),
            Arguments.of("trees.png.ris", "trees.png", FileUtil.FileCitationExtension.RIS),
            Arguments.of("trees.png.bib", "trees.png", FileUtil.FileCitationExtension.BIBTEX),
            Arguments.of(null, "trees.png", null),
            Arguments.of("50by1000-endnote.xml", "50by1000.tab", FileUtil.FileCitationExtension.ENDNOTE),
            Arguments.of("50by1000.ris", "50by1000.tab", FileUtil.FileCitationExtension.RIS),
            Arguments.of("50by1000.bib", "50by1000.tab", FileUtil.FileCitationExtension.BIBTEX)
        );
    }
    
    @ParameterizedTest
    @MethodSource("dataFilenames")
    void testGetCiteDataFileFilename(String expectedFileName, String actualFileName, FileCitationExtension citationExtension) {
        assertEquals(expectedFileName, FileUtil.getCiteDataFileFilename(actualFileName, citationExtension));
    }
    
    static Stream<Arguments> dataReplaceNames() {
        return Stream.of(
            // functional approach: what should the method do
            // replace no extension with an empty extension
            Arguments.of("no-extension.", "no-extension", ""),
        
            // replace extension x with same extension
            Arguments.of("extension.x", "extension.x", "x"),
        
            // replace extension x with another extension y
            Arguments.of("extension.y", "extension.x", "y"),
        
            // interface approach: what are possible inputs
            // will not pass as null is not handled
            //Arguments.of(null, null, null),
            //Arguments.of(null, null, ""),
            //Arguments.of(null, null, "y"),
            
            Arguments.of(".null", "", null),
            Arguments.of(".", "", ""),
            Arguments.of(".y", "", "y")
        );
    }
    
    @ParameterizedTest
    @MethodSource("dataReplaceNames")
    void testReplaceExtension(String expectedString, String originalName, String newExtension) {
        assertEquals(expectedString, FileUtil.replaceExtension(originalName, newExtension));
    }

    static class FileUtilNoParamTest {
        @Test
        public void testIsDownloadPopupRequiredNull() {
            assertFalse(FileUtil.isDownloadPopupRequired(null));
        }

        @Test
        public void testIsDownloadPopupRequiredDraft() {
            Dataset dataset = new Dataset();
            DatasetVersion dsv1 = dataset.getOrCreateEditVersion();
            assertEquals(DatasetVersion.VersionState.DRAFT, dsv1.getVersionState());
            assertFalse(FileUtil.isDownloadPopupRequired(dsv1));
        }

        @Test
        public void testIsDownloadPopupRequiredLicenseCC0() {
            DatasetVersion dsv1 = new DatasetVersion();
            dsv1.setVersionState(DatasetVersion.VersionState.RELEASED);
            TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
            License license = new License("CC0", "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.", URI.create("http://creativecommons.org/publicdomain/zero/1.0"), URI.create("/resources/images/cc0.png"), true, 1l);
            license.setDefault(true);
            termsOfUseAndAccess.setLicense(license);
            dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
            assertFalse(FileUtil.isDownloadPopupRequired(dsv1));
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
            License license = new License("CC0", "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.", URI.create("http://creativecommons.org/publicdomain/zero/1.0"), URI.create("/resources/images/cc0.png"), true, 2l);
            license.setDefault(true);
            termsOfUseAndAccess.setLicense(license);
            termsOfUseAndAccess.setTermsOfUse("be excellent to each other");
            dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
            assertFalse(FileUtil.isDownloadPopupRequired(dsv1));
        }

        @Test
        public void testIsDownloadPopupRequiredHasTermsOfUseAndNoneLicense() {
            DatasetVersion dsv1 = new DatasetVersion();
            dsv1.setVersionState(DatasetVersion.VersionState.RELEASED);
            TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
            termsOfUseAndAccess.setLicense(null);
            termsOfUseAndAccess.setTermsOfUse("be excellent to each other");
            dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
            assertTrue(FileUtil.isDownloadPopupRequired(dsv1));
        }

        @Test
        public void testIsDownloadPopupRequiredHasTermsOfAccess() {
            DatasetVersion dsv1 = new DatasetVersion();
            dsv1.setVersionState(DatasetVersion.VersionState.RELEASED);
            TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
            termsOfUseAndAccess.setTermsOfAccess("Terms of *Access* is different than Terms of Use");
            dsv1.setTermsOfUseAndAccess(termsOfUseAndAccess);
            assertTrue(FileUtil.isDownloadPopupRequired(dsv1));
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
            assertTrue(FileUtil.isDownloadPopupRequired(datasetVersion));
        }

        @Test
        public void testIsPubliclyDownloadable() {
            assertFalse(FileUtil.isPubliclyDownloadable(null));

            FileMetadata restrictedFileMetadata = new FileMetadata();
            restrictedFileMetadata.setRestricted(true);
            restrictedFileMetadata.setDataFile(new DataFile());
            assertFalse(FileUtil.isPubliclyDownloadable(restrictedFileMetadata));

            FileMetadata nonRestrictedFileMetadata = new FileMetadata();
            nonRestrictedFileMetadata.setDataFile(new DataFile());
            DatasetVersion dsv = new DatasetVersion();
            dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
            nonRestrictedFileMetadata.setDatasetVersion(dsv);
            Dataset dataset = new Dataset();
            dsv.setDataset(dataset);
            nonRestrictedFileMetadata.setRestricted(false);
            assertTrue(FileUtil.isPubliclyDownloadable(nonRestrictedFileMetadata));
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
            assertFalse(FileUtil.isPubliclyDownloadable(nonRestrictedFileMetadata));
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
            assertFalse(FileUtil.isPubliclyDownloadable(embargoedFileMetadata));
        }

        @Test
        public void testIsPubliclyDownloadable4() {

            FileMetadata retentionFileMetadata = new FileMetadata();
            DataFile df = new DataFile();
            Retention r = new Retention();
            r.setDateUnavailable(LocalDate.now().minusDays(1) );
            df.setRetention(r);
            retentionFileMetadata.setDataFile(df);
            DatasetVersion dsv = new DatasetVersion();
            dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
            retentionFileMetadata.setDatasetVersion(dsv);
            Dataset dataset = new Dataset();
            dsv.setDataset(dataset);
            retentionFileMetadata.setRestricted(false);
            assertFalse(FileUtil.isPubliclyDownloadable(retentionFileMetadata));
        }

        @Test
        public void testIsPubliclyDownloadable5() {

            FileMetadata retentionFileMetadata = new FileMetadata();
            DataFile df = new DataFile();
            Retention r = new Retention();
            r.setDateUnavailable(LocalDate.now());
            df.setRetention(r);
            retentionFileMetadata.setDataFile(df);
            DatasetVersion dsv = new DatasetVersion();
            dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
            retentionFileMetadata.setDatasetVersion(dsv);
            Dataset dataset = new Dataset();
            dsv.setDataset(dataset);
            retentionFileMetadata.setRestricted(false);
            assertTrue(FileUtil.isPubliclyDownloadable(retentionFileMetadata));
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
            assertNull(FileUtil.getPublicDownloadUrl(null, null, null));
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

    @Test
    public void testNetcdfFile() throws IOException {
        // We got madis-raob.nc from https://www.unidata.ucar.edu/software/netcdf/examples/files.html
        // and named it "madis-raob" with no file extension for this test.
        String path = "src/test/resources/netcdf/";
        String pathAndFile = path + "madis-raob";
        File file = new File(pathAndFile);
        String contentType = FileUtil.determineFileType(file, pathAndFile);
        assertEquals("application/netcdf", contentType);
    }

    @Test
    public void testHdf5File() throws IOException {
        // We got vlen_string_dset.h5 from https://github.com/h5py/h5py/blob/3.7.0/h5py/tests/data_files/vlen_string_dset.h5
        // and named in "vlen_string_dset" with no file extension for this test.
        String path = "src/test/resources/hdf/hdf5/";
        String pathAndFile = path + "vlen_string_dset";
        File file = new File(pathAndFile);
        String contentType = FileUtil.determineFileType(file, pathAndFile);
        assertEquals("application/x-hdf5", contentType);
    }

    @Test
    public void testHdf4File() throws IOException {
        // We got test.hdf from https://people.sc.fsu.edu/~jburkardt/data/hdf/hdf.html
        // and named in "hdf4test" with no file extension for this test.
        // HDF4 is the old format, the previous generation before HDF5.
        // We can't detect it based on its content.
        String path = "src/test/resources/hdf/hdf4/";
        String pathAndFile = path + "hdf4test";
        File file = new File(pathAndFile);
        String contentType = FileUtil.determineFileType(file, pathAndFile);
        assertEquals("application/octet-stream", contentType);
    }

    @Test
    public void testGZipFile() throws IOException {
        String path = "src/test/resources/fits/";
        String pathAndFile = path + "FOSy19g0309t_c2f.fits.gz";
        File file = new File(pathAndFile);
        String contentType = FileUtil.determineFileType(file, pathAndFile);
        assertEquals("application/fits-gzipped", contentType);
    }

    @Test
    public void testDetermineFileTypeROCrate() {
        final String roCrateContentType = "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#flattened http://www.w3.org/ns/json-ld#compacted https://w3id.org/ro/crate\"";
        final DataFile rocrate = new DataFile(roCrateContentType);
        
        assertEquals(roCrateContentType, rocrate.getContentType());
        assertEquals("RO-Crate metadata", FileUtil.getUserFriendlyFileType(rocrate));
        assertEquals("Metadata", FileUtil.getIndexableFacetFileType(rocrate));

        final File roCrateFile = new File("src/test/resources/fileutil/ro-crate-metadata.json");
        try {
            assertEquals(roCrateContentType, FileUtil.determineFileType(roCrateFile, "ro-crate-metadata.json"));
        } catch (IOException ex) {
            fail(ex);
        }

        // test ";" removal
        final String dockerFileWithProfile = "application/x-docker-file; profile=\"http://www.w3.org/ns/json-ld#flattened http://www.w3.org/ns/json-ld#compacted https://w3id.org/ro/crate\"";
        final DataFile dockerDataFile = new DataFile(dockerFileWithProfile);
        
        assertEquals(dockerFileWithProfile, dockerDataFile.getContentType());
        assertEquals("Docker Image File", FileUtil.getUserFriendlyFileType(dockerDataFile));
        assertEquals("Code", FileUtil.getIndexableFacetFileType(dockerDataFile));
    }

    @Test
    public void testSanitizeFileName() {
        assertEquals(null, FileUtil.sanitizeFileName(null));
        assertEquals("with_space", FileUtil.sanitizeFileName("with space"));
        assertEquals("withcomma", FileUtil.sanitizeFileName("with,comma"));
        assertEquals("with.txt", FileUtil.sanitizeFileName("with,\\?:;,.txt"));
    }
}
