package edu.harvard.iq.dataverse.datafile;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIOConstants;
import edu.harvard.iq.dataverse.datafile.pojo.FileIntegrityCheckResult;
import edu.harvard.iq.dataverse.datafile.pojo.FilesIntegrityReport;
import edu.harvard.iq.dataverse.mail.EmailContent;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FileIntegrityCheckerTest {

    @InjectMocks
    private FileIntegrityChecker fileIntegrityChecker;

    @Mock
    private DataFileServiceBean dataFileService;

    @Mock
    private AuthenticationServiceBean authSvc;

    @Mock
    private MailService mailService;

    @Mock
    private DataAccess dataAccess;

    @Mock
    private SystemConfig systemConfig;

    @Mock
    private DataverseDao dataverseDao;

    @Captor
    private ArgumentCaptor<EmailContent> emailContentCaptor;

    // -------------------- TESTS --------------------

    @BeforeEach
    public void beforeEach() {
        Dataverse dv = new Dataverse();
        dv.setName("RepOD");

        when(dataverseDao.findRootDataverse()).thenReturn(dv);
        when(systemConfig.getSiteFullName(Locale.ENGLISH)).thenReturn("Repository for Open Data");
        when(systemConfig.getDataverseSiteUrl()).thenReturn("http://localhost:8080");
    }

    @Test
    public void checkFilesIntegrity() throws IOException {
        // given
        DataFile datafile1 = makeDataFile(102L, ChecksumType.MD5, "md5hash1");
        DataFile datafile2 = makeDataFile(103L, ChecksumType.MD5, "md5hash2");
        DataFile datafile3 = makeDataFile(104L, ChecksumType.MD5, "md5hash3");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIO(true, 102L, true, "md5hash1");
        StorageIO<DataFile> datafileStorage2 = mockDataFileStorageIO(true, 103L, true, "md5hash2");
        StorageIO<DataFile> datafileStorage3 = mockDataFileStorageIO(true, 104L, true, "md5hash3");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1, datafile2, datafile3));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);
        when(dataAccess.getStorageIO(datafile2)).thenReturn(datafileStorage2);
        when(dataAccess.getStorageIO(datafile3)).thenReturn(datafileStorage3);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(3, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(0, integrityReport.getSuspicious().size());
    }

    @Test
    public void checkFilesIntegrity__with_storage_that_not_supports_md5() throws IOException {
        // given
        DataFile datafile1 = makeDataFile(102L, ChecksumType.MD5, "md5hash1");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIO(true, 102L, false, "md5hash1");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(1, integrityReport.getSkippedChecksumVerification());
        assertEquals(0, integrityReport.getSuspicious().size());
    }

    @Test
    public void checkFilesIntegrity__with_datafile_checksum_other_than_md5() throws IOException {
        // given
        DataFile datafile1 = makeDataFile(102L, ChecksumType.SHA512, "sha512hash1");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIO(true, 102L, true, "md5hash1");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(1, integrityReport.getSkippedChecksumVerification());
        assertEquals(0, integrityReport.getSuspicious().size());
    }

    @Test
    public void checkFilesIntegrity__with_storage_md5_different_than_datafile_md5() throws IOException {
        // given
        DataFile datafile1 = makeDataFile(102L, ChecksumType.MD5, "md5hash1");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIO(true, 102L, true, "md5hash_other");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(1, integrityReport.getSuspicious().size());
        assertEquals(FileIntegrityCheckResult.DIFFERENT_CHECKSUM, integrityReport.getSuspicious().get(0).getCheckResult());
        assertEquals(datafile1, integrityReport.getSuspicious().get(0).getIntegrityFailFile());
    }

    @Test
    public void checkFilesIntegrity__with_storage_aux_md5_different_than_datafile_md5() throws IOException {
        // given
        DataFile datafile1 = makeTabularDataFile(102L, ChecksumType.MD5, "md5hash1");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIOWithAuxObject(true, 102L, true, "md5hash_other");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(1, integrityReport.getSuspicious().size());
        assertEquals(FileIntegrityCheckResult.DIFFERENT_CHECKSUM, integrityReport.getSuspicious().get(0).getCheckResult());
        assertEquals(datafile1, integrityReport.getSuspicious().get(0).getIntegrityFailFile());
    }

    @Test
    public void checkFilesIntegrity__with_storage_size_different_than_datafile_size() throws IOException {
        // given
        DataFile datafile1 = makeDataFile(102L, ChecksumType.MD5, "md5hash1");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIO(true, 10200L, false, "md5hash1");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(1, integrityReport.getSuspicious().size());
        assertEquals(FileIntegrityCheckResult.DIFFERENT_SIZE, integrityReport.getSuspicious().get(0).getCheckResult());
        assertEquals(datafile1, integrityReport.getSuspicious().get(0).getIntegrityFailFile());
    }

    @Test
    public void checkFilesIntegrity__with_storage_aux_size_different_than_datafile_tabular_size() throws IOException {
        // given
        DataFile datafile1 = makeTabularDataFile(102L, ChecksumType.MD5, "md5hash1");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIOWithAuxObject(true, 10200L, false, "md5hash1");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(1, integrityReport.getSuspicious().size());
        assertEquals(FileIntegrityCheckResult.DIFFERENT_SIZE, integrityReport.getSuspicious().get(0).getCheckResult());
        assertEquals(datafile1, integrityReport.getSuspicious().get(0).getIntegrityFailFile());
    }

    @Test
    public void checkFilesIntegrity__with_not_existing_storage_file() throws IOException {
        // given
        DataFile datafile1 = makeDataFile(102L, ChecksumType.MD5, "md5hash1");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIO(false, 0L, false, null);

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(1, integrityReport.getSuspicious().size());
        assertEquals(FileIntegrityCheckResult.NOT_EXIST, integrityReport.getSuspicious().get(0).getCheckResult());
        assertEquals(datafile1, integrityReport.getSuspicious().get(0).getIntegrityFailFile());
    }

    @Test
    public void checkFilesIntegrity__with_not_existing_storage_tabular_file() throws IOException {
        // given
        DataFile datafile1 = makeTabularDataFile(102L, ChecksumType.MD5, "md5hash1");

        StorageIO<DataFile> datafileStorage1 = mockDataFileStorageIOWithAuxObject(false, 0L, false, null);

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenReturn(datafileStorage1);

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(1, integrityReport.getSuspicious().size());
        assertEquals(FileIntegrityCheckResult.NOT_EXIST, integrityReport.getSuspicious().get(0).getCheckResult());
        assertEquals(datafile1, integrityReport.getSuspicious().get(0).getIntegrityFailFile());
    }

    @Test
    public void checkFilesIntegrity__with_error_obtaining_storage() throws IOException {
        // given
        DataFile datafile1 = makeDataFile(102L, ChecksumType.MD5, "md5hash1");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenThrow(new IOException("Problem getting storageIO"));

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(1, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(1, integrityReport.getSuspicious().size());
        assertEquals(FileIntegrityCheckResult.STORAGE_ERROR, integrityReport.getSuspicious().get(0).getCheckResult());
        assertEquals(datafile1, integrityReport.getSuspicious().get(0).getIntegrityFailFile());
    }

    @Test
    public void checkFilesIntegrity__with_harvested_data_file() {
        // given
        DataFile datafile1 = makeDataFile(102L, ChecksumType.MD5, "md5hash1");
        datafile1.getOwner().setHarvestedFrom(new HarvestingClient());

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));

        // when
        FilesIntegrityReport integrityReport = fileIntegrityChecker.checkFilesIntegrity();

        // then
        assertEquals(0, integrityReport.getCheckedCount());
        assertEquals(0, integrityReport.getSkippedChecksumVerification());
        assertEquals(0, integrityReport.getSuspicious().size());
    }


    @Test
    public void checkFilesIntegrity__should_send_report_to_superusers() throws IOException {
        // given
        AuthenticatedUser superuser1 = MocksFactory.makeAuthenticatedUser("super1", "user");
        superuser1.setEmail("super1@example.com");
        AuthenticatedUser superuser2 = MocksFactory.makeAuthenticatedUser("super2", "user");
        superuser2.setEmail("super2@example.com");

        DataFile datafile1 = makeDataFile(102L, ChecksumType.MD5, "md5hash1");

        when(dataFileService.findAll()).thenReturn(Lists.newArrayList(datafile1));
        when(dataAccess.getStorageIO(datafile1)).thenThrow(new IOException("Problem getting storageIO"));
        when(authSvc.findSuperUsers()).thenReturn(Lists.newArrayList(superuser1, superuser2));

        // when
        fileIntegrityChecker.checkFilesIntegrity();

        // then
        verify(mailService).sendMailAsync(eq("super1@example.com"), Mockito.any(), emailContentCaptor.capture());

        String emailSubject1 = emailContentCaptor.getValue().getSubject();
        String emailText1 = emailContentCaptor.getValue().getMessageText();
        String emailFooter1 = emailContentCaptor.getValue().getFooter();

        assertEquals("RepOD files integrity check report", emailSubject1);
        assertThat(emailText1, containsString("Files checked: 1"));
        assertThat(emailText1, containsString("Number of files with failures: 1"));
        assertThat(emailText1, containsString("** Number of files with \"STORAGE_ERROR\" discrepancy: 1"));
        assertThat(emailText1, containsString("** Number of files with \"DIFFERENT_SIZE\" discrepancy: 0"));
        assertThat(emailText1, containsString("** Number of files with \"NOT_EXIST\" discrepancy: 0"));
        assertThat(emailText1, containsString("** Number of files with \"DIFFERENT_CHECKSUM\" discrepancy: 0"));
        assertThat(emailText1, containsString("File id: " + datafile1.getId() +
                ", file label: Metadata for DataFile " + datafile1.getId() + " (STORAGE_ERROR)" +
                ", file link: http://localhost:8080/file.xhtml?fileId=" + datafile1.getId()));
        assertEquals("", emailFooter1);


        verify(mailService).sendMailAsync(eq("super2@example.com"), Mockito.any(), emailContentCaptor.capture());

        String emailSubject2 = emailContentCaptor.getValue().getSubject();
        String emailText2 = emailContentCaptor.getValue().getMessageText();
        String emailFooter2 = emailContentCaptor.getValue().getFooter();

        assertEquals("RepOD files integrity check report", emailSubject2);
        assertEquals(emailText1, emailText2);
        assertEquals("", emailFooter2);

    }

    // -------------------- PRIVATE --------------------

    private DataFile makeDataFile(long size, ChecksumType checksumType, String checksumValue) {
        DataFile datafile1 = MocksFactory.makeDataFile();
        datafile1.setFilesize(size);
        datafile1.setChecksumType(checksumType);
        datafile1.setChecksumValue(checksumValue);
        datafile1.setStorageIdentifier("");
        datafile1.setOwner(new Dataset());
        return datafile1;
    }

    private DataFile makeTabularDataFile(long originalSize, ChecksumType checksumType, String checksumValue) {
        DataFile datafile1 = MocksFactory.makeDataFile();
        datafile1.setDataTable(new DataTable());
        datafile1.getDataTable().setOriginalFileSize(originalSize);

        datafile1.setChecksumType(checksumType);
        datafile1.setChecksumValue(checksumValue);
        datafile1.setStorageIdentifier("");
        datafile1.setOwner(new Dataset());
        return datafile1;
    }

    private StorageIO<DataFile> mockDataFileStorageIO(boolean exists, long size, boolean isMD5Supported, String md5) throws IOException {
        StorageIO<DataFile> storageIO = mock(StorageIO.class);
        when(storageIO.exists()).thenReturn(exists);
        when(storageIO.getSize()).thenReturn(size);
        when(storageIO.isMD5CheckSupported()).thenReturn(isMD5Supported);
        when(storageIO.getMD5()).thenReturn(md5);
        return storageIO;
    }

    private StorageIO<DataFile> mockDataFileStorageIOWithAuxObject(boolean exists, long size, boolean isMD5Supported, String md5) throws IOException {
        StorageIO<DataFile> storageIO = mock(StorageIO.class);
        when(storageIO.isAuxObjectCached(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION)).thenReturn(exists);
        when(storageIO.getAuxObjectSize(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION)).thenReturn(size);
        when(storageIO.isMD5CheckSupported()).thenReturn(isMD5Supported);
        when(storageIO.getAuxObjectMD5(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION)).thenReturn(md5);
        return storageIO;
    }
}
