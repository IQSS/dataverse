package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.datafile.pojo.FileIntegrityCheckResult;
import edu.harvard.iq.dataverse.datafile.pojo.FilesIntegrityReport;
import edu.harvard.iq.dataverse.mail.EmailContent;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile.ChecksumType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
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
    private DataAccess dataAccess;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    FileIntegrityChecker() {
        // JEE requirement
    }

    @Inject
    public FileIntegrityChecker(DataFileServiceBean dataFileServiceBean,
            AuthenticationServiceBean authSvc,
            MailService mailService) {
        this(dataFileServiceBean, authSvc, mailService, DataAccess.dataAccess());
    }

    FileIntegrityChecker(DataFileServiceBean dataFileServiceBean,
            AuthenticationServiceBean authSvc,
            MailService mailService,
            DataAccess dataAccess) {
        this.dataFileService = dataFileServiceBean;
        this.authSvc = authSvc;
        this.mailService = mailService;
        this.dataAccess = dataAccess;
    }
    
    
    // -------------------- LOGIC --------------------

    public FilesIntegrityReport checkFilesIntegrity() {
        List<DataFile> dataFiles = dataFileService.findAll();

        FilesIntegrityReport report = new FilesIntegrityReport();
        report.setCheckedCount(dataFiles.size());

        for (DataFile dataFile:dataFiles) {
            FileIntegrityCheckResult checkResult = checkFileIntegrity(dataFile);

            if (!checkResult.isOK()) {
                report.addSuspicious(dataFile, checkResult);
            } else if (checkResult == FileIntegrityCheckResult.OK_SKIPPED_CHECKSUM_VERIFICATION) {
                report.incrementSkippedChecksumVerification();
            }
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
            
            if (!storageIO.exists() || storageIO.getSize() == 0) {
                return FileIntegrityCheckResult.NOT_EXIST;
            }
            if (storageIO.getSize() != dataFile.getFilesize()) {
                return FileIntegrityCheckResult.DIFFERENT_SIZE;
            }
            if (storageIO.isMD5CheckSupported() && dataFile.getChecksumType() == ChecksumType.MD5) {
                if (!StringUtils.equals(storageIO.getMD5(), dataFile.getChecksumValue())) {
                    return FileIntegrityCheckResult.DIFFERENT_CHECKSUM;
                }
            } else {
                return FileIntegrityCheckResult.OK_SKIPPED_CHECKSUM_VERIFICATION;
            }
            return FileIntegrityCheckResult.OK;
            
        } catch (IOException e) {
            logger.info(e.getMessage());
            return FileIntegrityCheckResult.STORAGE_ERROR;
        }
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
                                        .append(NEW_LINE));

        String messageSubject = "Dataverse files integrity check report";

        return new EmailContent(messageSubject, messageBodyBuilder.toString(), "");
    }
}
