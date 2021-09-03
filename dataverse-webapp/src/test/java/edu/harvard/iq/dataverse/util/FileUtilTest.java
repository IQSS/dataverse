package edu.harvard.iq.dataverse.util;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.RestrictType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.guestbook.Guestbook;
import edu.harvard.iq.dataverse.util.FileUtil.ApiBatchDownloadType;
import edu.harvard.iq.dataverse.util.FileUtil.ApiDownloadType;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilTest {

    @Test
    public void testGetCiteDataFileFilename() {

        assertEquals(null, FileUtil.getCiteDataFileFilename(null, null));

        String fileName = "trees.png";
        assertEquals("trees.png-endnote.xml", FileUtil.getCiteDataFileFilename(fileName, FileUtil.FileCitationExtension.ENDNOTE));
        assertEquals("trees.png.ris", FileUtil.getCiteDataFileFilename(fileName, FileUtil.FileCitationExtension.RIS));
        assertEquals("trees.png.bib", FileUtil.getCiteDataFileFilename(fileName, FileUtil.FileCitationExtension.BIBTEX));
        assertEquals(null, FileUtil.getCiteDataFileFilename(fileName, null));


        String tabFileName = "50by1000.tab";
        assertEquals("50by1000-endnote.xml", FileUtil.getCiteDataFileFilename(tabFileName, FileUtil.FileCitationExtension.ENDNOTE));
        assertEquals("50by1000.ris", FileUtil.getCiteDataFileFilename(tabFileName, FileUtil.FileCitationExtension.RIS));
        assertEquals("50by1000.bib", FileUtil.getCiteDataFileFilename(tabFileName, FileUtil.FileCitationExtension.BIBTEX));
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
        assertFalse(FileUtil.isPubliclyDownloadable(null));

        FileMetadata restrictedFileMetadata = MocksFactory.makeFileMetadata(123l, "file.txt", 0);
        restrictedFileMetadata.getTermsOfUse().setLicense(null);
        restrictedFileMetadata.getTermsOfUse().setRestrictType(RestrictType.ACADEMIC_PURPOSE);
        assertFalse(FileUtil.isPubliclyDownloadable(restrictedFileMetadata));

        FileMetadata nonRestrictedFileMetadata = MocksFactory.makeFileMetadata(123l, "file.txt", 0);
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
        nonRestrictedFileMetadata.setDatasetVersion(dsv);
        Dataset dataset = new Dataset();
        dsv.setDataset(dataset);
        assertTrue(FileUtil.isPubliclyDownloadable(nonRestrictedFileMetadata));
    }

    @Test
    public void testgetFileDownloadUrl() {
        Long fileId = 42l;
        assertEquals("/api/access/datafile/42", FileUtil.getFileDownloadUrlPath(ApiDownloadType.DEFAULT, fileId, false));
        assertThrows(NullPointerException.class, () -> FileUtil.getFileDownloadUrlPath(null, fileId, false));
        assertEquals("/api/access/datafile/bundle/42", FileUtil.getFileDownloadUrlPath(ApiDownloadType.BUNDLE, fileId, false));
        assertEquals("/api/access/datafile/42?format=original", FileUtil.getFileDownloadUrlPath(ApiDownloadType.ORIGINAL, fileId, false));
        assertEquals("/api/access/datafile/42?format=RData", FileUtil.getFileDownloadUrlPath(ApiDownloadType.RDATA, fileId, false));
        assertEquals("/api/access/datafile/42/metadata", FileUtil.getFileDownloadUrlPath(ApiDownloadType.VAR, fileId, false));
        assertEquals("/api/access/datafile/42?format=tab", FileUtil.getFileDownloadUrlPath(ApiDownloadType.TAB, fileId, false));
        assertEquals("/api/access/datafile/42?format=tab&gbrecs=true", FileUtil.getFileDownloadUrlPath(ApiDownloadType.TAB, fileId, true));
        assertEquals("/api/access/datafile/42?gbrecs=true", FileUtil.getFileDownloadUrlPath(ApiDownloadType.DEFAULT, fileId, true));
    }
    
    @Test
    public void testgetBatchFilesDownloadUrlPath() {
        // given
        List<Long> fileIds = Lists.newArrayList(11L, 12L);
        // when & then
        assertEquals("/api/access/datafiles/11,12", FileUtil.getBatchFilesDownloadUrlPath(fileIds, false, ApiBatchDownloadType.DEFAULT));
        assertEquals("/api/access/datafiles/11,12?gbrecs=true", FileUtil.getBatchFilesDownloadUrlPath(fileIds, true, ApiBatchDownloadType.DEFAULT));
        assertEquals("/api/access/datafiles/11,12?format=original", FileUtil.getBatchFilesDownloadUrlPath(fileIds, false, ApiBatchDownloadType.ORIGINAL));
        assertEquals("/api/access/datafiles/11,12?gbrecs=true&format=original", FileUtil.getBatchFilesDownloadUrlPath(fileIds, true, ApiBatchDownloadType.ORIGINAL));
    }

    @Test
    public void testGetDownloadWholeDatasetUrlPath() {
        //given
        DatasetVersion dsv = new DatasetVersion();
        dsv.setId(11L);
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        dsv.setDataset(dataset);

        // when & then
        assertEquals("/api/datasets/1/versions/11/files/download?format=original", FileUtil.getDownloadWholeDatasetUrlPath(dsv, false, ApiBatchDownloadType.ORIGINAL));
        assertEquals("/api/datasets/1/versions/11/files/download", FileUtil.getDownloadWholeDatasetUrlPath(dsv, false, ApiBatchDownloadType.DEFAULT));
        assertEquals("/api/datasets/1/versions/11/files/download?gbrecs=true&format=original", FileUtil.getDownloadWholeDatasetUrlPath(dsv, true, ApiBatchDownloadType.ORIGINAL));
        assertEquals("/api/datasets/1/versions/11/files/download?gbrecs=true", FileUtil.getDownloadWholeDatasetUrlPath(dsv, true, ApiBatchDownloadType.DEFAULT));
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

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not files that thumbnails can be created for.
     *
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
