package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIOConstants;
import edu.harvard.iq.dataverse.datafile.pojo.FileIntegrityCheckResult;
import edu.harvard.iq.dataverse.datafile.pojo.FileIntegrityFail;
import edu.harvard.iq.dataverse.datafile.pojo.FilesIntegrityReport;
import edu.harvard.iq.dataverse.mail.EmailContent;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@Stateless
public class FileIntegrityChecker {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private static final String NEW_LINE = "\n";

    private DataFileServiceBean dataFileService;
    private AuthenticationServiceBean authSvc;
    private MailService mailService;
    private DataverseDao dataverseDao;
    private SystemConfig systemConfig;
    private DataAccess dataAccess;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    FileIntegrityChecker() {
        // JEE requirement
    }

    @Inject
    public FileIntegrityChecker(DataFileServiceBean dataFileServiceBean,
                                AuthenticationServiceBean authSvc,
                                MailService mailService,
                                SystemConfig systemConfig,
                                DataverseDao dataverseDao) {
        this(dataFileServiceBean, authSvc, mailService, systemConfig, dataverseDao, DataAccess.dataAccess());
    }

    FileIntegrityChecker(DataFileServiceBean dataFileServiceBean,
            AuthenticationServiceBean authSvc,
            MailService mailService,
            SystemConfig systemConfig,
            DataverseDao dataverseDao,
            DataAccess dataAccess) {
        this.dataFileService = dataFileServiceBean;
        this.authSvc = authSvc;
        this.mailService = mailService;
        this.systemConfig = systemConfig;
        this.dataverseDao = dataverseDao;
        this.dataAccess = dataAccess;
    }


    // -------------------- LOGIC --------------------

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public FilesIntegrityReport checkFilesIntegrity() {
        List<DataFile> dataFiles = dataFileService.findAll();

        FilesIntegrityReport report = new FilesIntegrityReport();

        for (DataFile dataFile:dataFiles) {
            if (dataFile.isHarvested()) {
                continue;
            }
            FileIntegrityCheckResult checkResult = checkFileIntegrity(dataFile);

            if (!checkResult.isOK()) {
                report.addSuspicious(dataFile, checkResult);
                report.incrementFailCount(checkResult);
            } else if (checkResult == FileIntegrityCheckResult.OK_SKIPPED_CHECKSUM_VERIFICATION) {
                report.incrementSkippedChecksumVerification();
            }

            report.incrementCheckedCount();
        }

        EmailContent reportEmailContent = buildReportEmailContent(report);
        authSvc.findSuperUsers()
            .stream()
            .forEach(user -> mailService.sendMailAsync(user.getEmail(), reportEmailContent));


        return report;

    }

    // -------------------- PRIVATE --------------------

    private FileIntegrityCheckResult checkFileIntegrity(DataFile dataFile) {
        try {
            StorageIO<DataFile> storageIO = dataAccess.getStorageIO(dataFile);

            if (!existsInStorage(dataFile, storageIO)) {
                return FileIntegrityCheckResult.NOT_EXIST;
            }
            if (!haveSameFilesize(dataFile, storageIO)) {
                return FileIntegrityCheckResult.DIFFERENT_SIZE;
            }

            boolean withMd5Compare = storageIO.isMD5CheckSupported() && dataFile.getChecksumType() == ChecksumType.MD5;

            if (withMd5Compare && !haveSameMd5(dataFile, storageIO)) {
                return FileIntegrityCheckResult.DIFFERENT_CHECKSUM;
            }

            return withMd5Compare ? FileIntegrityCheckResult.OK : FileIntegrityCheckResult.OK_SKIPPED_CHECKSUM_VERIFICATION;

        } catch (IOException e) {
            logger.info(e.getMessage());
            return FileIntegrityCheckResult.STORAGE_ERROR;
        }
    }

    private boolean existsInStorage(DataFile dataFile, StorageIO<DataFile> storageIO) throws IOException {
        return dataFile.isTabularData()
                ? storageIO.isAuxObjectCached(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION)
                : storageIO.exists();
    }

    private boolean haveSameFilesize(DataFile dataFile, StorageIO<DataFile> storageIO) throws IOException {
        long databaseFilesize = dataFile.isTabularData() ? dataFile.getOriginalFileSize() : dataFile.getFilesize();
        long storageFilesize = dataFile.isTabularData()
                ? storageIO.getAuxObjectSize(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION)
                : storageIO.getSize();

        return databaseFilesize == storageFilesize;
    }

    private boolean haveSameMd5(DataFile dataFile, StorageIO<DataFile> storageIO) throws IOException {
        String databaseChecksum = dataFile.getChecksumValue();
        String storageChecksum = dataFile.isTabularData()
                ? storageIO.getAuxObjectMD5(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION)
                : storageIO.getMD5();

        return StringUtils.equals(databaseChecksum, storageChecksum);
    }

    private EmailContent buildReportEmailContent(FilesIntegrityReport report) {
        StringBuilder messageBodyBuilder = new StringBuilder();

        messageBodyBuilder.append("Datafiles integrity check summary: ")
                          .append(NEW_LINE)
                          .append("Files checked: ")
                          .append(report.getCheckedCount())
                          .append(NEW_LINE)
                          .append("Skipped checksum verification: ")
                          .append(report.getSkippedChecksumVerification())
                          .append(NEW_LINE)
                          .append("Number of files with failures: ")
                          .append(report.getSuspicious().size())
                          .append(NEW_LINE)
                          .append(NEW_LINE)
                          .append("** Number of files with \"").append(FileIntegrityCheckResult.DIFFERENT_CHECKSUM).append("\" discrepancy: ")
                          .append(report.getFailCountFor(FileIntegrityCheckResult.DIFFERENT_CHECKSUM))
                          .append(NEW_LINE)
                          .append("** Number of files with \"").append(FileIntegrityCheckResult.NOT_EXIST).append("\" discrepancy: ")
                          .append(report.getFailCountFor(FileIntegrityCheckResult.NOT_EXIST))
                          .append(NEW_LINE)
                          .append("** Number of files with \"").append(FileIntegrityCheckResult.STORAGE_ERROR).append("\" discrepancy: ")
                          .append(report.getFailCountFor(FileIntegrityCheckResult.STORAGE_ERROR))
                          .append(NEW_LINE)
                          .append("** Number of files with \"").append(FileIntegrityCheckResult.DIFFERENT_SIZE).append("\" discrepancy: ")
                          .append(report.getFailCountFor(FileIntegrityCheckResult.DIFFERENT_SIZE))
                          .append(NEW_LINE)
                          .append(NEW_LINE)
                          .append("List of files with failures:")
                          .append(NEW_LINE);

        report.getSuspicious().stream()
            .forEach(integrityFail -> messageBodyBuilder
                                        .append("File id: ")
                                        .append(integrityFail.getIntegrityFailFile().getId())
                                        .append(", file label: ")
                                        .append(integrityFail.getIntegrityFailFile().getLatestFileMetadata().getLabel())
                                        .append(" (")
                                        .append(integrityFail.getCheckResult())
                                        .append(")")
                                        .append(", file link: ")
                                        .append(getFailedFileUrl(integrityFail))
                                        .append(NEW_LINE));

        String messageSubject = BrandingUtil.getInstallationBrandName(dataverseDao.findRootDataverse().getName()) + " files integrity check report";

        return new EmailContent(messageSubject, messageBodyBuilder.toString(), "");
    }

    private String getFailedFileUrl(FileIntegrityFail fail) {
        return systemConfig.getDataverseSiteUrl() + fail.getFailedFileUrl();
    }

}
