package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.bagit.BagValidation.FileValidationResult;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider.InputStreamProvider;
import org.apache.commons.compress.utils.IOUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author adaybujeda
 */
public class FileChecksumValidationJob implements Runnable {

    private static final Logger logger = Logger.getLogger(FileChecksumValidationJob.class.getCanonicalName());

    private final InputStreamProvider inputStreamProvider;
    private final Path filePath;
    private final String fileChecksum;
    private final BagChecksumType bagChecksumType;
    private final FileValidationResult result;

    public FileChecksumValidationJob(InputStreamProvider inputStreamProvider, Path filePath, String fileChecksum, BagChecksumType bagChecksumType, FileValidationResult result) {
        this.inputStreamProvider = inputStreamProvider;
        this.filePath = filePath;
        this.fileChecksum = fileChecksum;
        this.bagChecksumType = bagChecksumType;
        this.result = result;
    }

    public void run() {
        InputStream inputStream = null;
        try {
            inputStream = inputStreamProvider.getInputStream();
            String calculatedChecksum = bagChecksumType.getInputStreamDigester().digest(inputStream);
            if (fileChecksum.equals(calculatedChecksum)) {
                result.setSuccess();
            } else {
                result.setError(getMessage("bagit.checksum.validation.error", filePath, bagChecksumType, fileChecksum, calculatedChecksum));
            }
        } catch (Exception e) {
            result.setError(getMessage("bagit.checksum.validation.exception", filePath, bagChecksumType, e.getMessage()));
            logger.log(Level.WARNING, String.format("action=validate-checksum result=error filePath=%s type=%s", filePath, bagChecksumType), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private String getMessage(String propertyKey, Object... parameters){
        List<String> parameterList = Arrays.stream(parameters).map(param -> param.toString()).collect(Collectors.toList());
        return BundleUtil.getStringFromBundle(propertyKey, parameterList);
    }

}
