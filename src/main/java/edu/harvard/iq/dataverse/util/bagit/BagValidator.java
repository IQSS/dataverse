package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.bagit.BagValidation.FileValidationResult;
import edu.harvard.iq.dataverse.util.bagit.ManifestReader.ManifestChecksum;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider.InputStreamProvider;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author adaybujeda
 */
public class BagValidator {

    private static final Logger logger = Logger.getLogger(BagValidator.class.getCanonicalName());

    public static enum BagValidatorSettings {
        JOB_POOL_SIZE(":BagValidatorJobPoolSize", 4),
        MAX_ERRORS(":BagValidatorMaxErrors", 5),
        JOB_WAIT_INTERVAL(":BagValidatorJobWaitInterval", 10);

        private String settingsKey;
        private Integer defaultValue;

        private BagValidatorSettings(String settingsKey, Integer defaultValue) {
            this.settingsKey = settingsKey;
            this.defaultValue = defaultValue;
        }

        public String getSettingsKey() {
            return settingsKey;
        }

        public Integer getDefaultValue() {
            return defaultValue;
        }
    }

    private static final String BAGIT_FILE_MARKER = "bagit.txt";

    private final Integer validatorJobPoolSize;
    private final Integer maxErrors;
    private final Integer jobWaitIntervalInSeconds;
    private final ManifestReader manifestReader;

    public BagValidator(ManifestReader manifestReader) {
        this(BagValidatorSettings.JOB_POOL_SIZE.getDefaultValue(), BagValidatorSettings.MAX_ERRORS.getDefaultValue(), BagValidatorSettings.JOB_WAIT_INTERVAL.getDefaultValue(), manifestReader);
    }

    public BagValidator(Integer validatorJobPoolSize, Integer maxErrors, Integer jobWaitIntervalInSeconds, ManifestReader manifestReader) {
        this.validatorJobPoolSize = validatorJobPoolSize == null ? BagValidatorSettings.JOB_POOL_SIZE.getDefaultValue() : validatorJobPoolSize;
        this.maxErrors = maxErrors == null ? BagValidatorSettings.MAX_ERRORS.getDefaultValue() : maxErrors;
        this.jobWaitIntervalInSeconds = jobWaitIntervalInSeconds == null ? BagValidatorSettings.JOB_WAIT_INTERVAL.getDefaultValue() : jobWaitIntervalInSeconds;
        this.manifestReader = manifestReader;
    }

    public boolean hasBagItPackage(FileDataProvider fileDataProvider) {
        Optional<Path> bagItFile = getBagItFile(fileDataProvider.getFilePaths());
        if(bagItFile.isEmpty()) {
            return false;
        }

        Path bagRoot = getBagItRoot(bagItFile.get());
        Optional<Path> supportedManifest = manifestReader.getSupportedManifest(fileDataProvider, bagRoot);
        return supportedManifest.isPresent();
    }

    public BagValidation validateChecksums(FileDataProvider fileDataProvider) {
        Optional<Path> bagItFile = getBagItFile(fileDataProvider.getFilePaths());
        if (bagItFile.isEmpty()) {
            logger.warning(String.format("action=validateBag result=bag-marker-file-not-found fileDataProvider=%s", fileDataProvider.getName()));
            return new BagValidation(Optional.of(getMessage("bagit.validation.bag.file.not.found", fileDataProvider.getName())));
        }

        Path bagRoot = getBagItRoot(bagItFile.get());
        Optional<ManifestChecksum> manifestChecksum = manifestReader.getManifestChecksums(fileDataProvider, bagRoot);
        if (manifestChecksum.isEmpty()) {
            logger.warning(String.format("action=validateBag result=no-supported-manifest-found fileDataProvider=%s", fileDataProvider.getName()));
            return new BagValidation(Optional.of(getMessage("bagit.validation.manifest.not.supported", fileDataProvider.getName(), BagChecksumType.asList())));
        }

        BagValidation bagValidation = validateChecksums(fileDataProvider, manifestChecksum.get());
        logger.fine(String.format("action=validateBag completed fileDataProvider=%s bagValidation=%s", fileDataProvider.getName(), bagValidation));
        return bagValidation;
    }

    private Optional<Path> getBagItFile(List<Path> filePaths) {
        return filePaths.stream().filter(path -> path.endsWith(BAGIT_FILE_MARKER)).findFirst();
    }

    private Path getBagItRoot(Path bagItFile) {
        Path bagRoot = Optional.ofNullable(bagItFile.getParent()).filter(path -> path != null).orElse(Path.of(""));
        return bagRoot;
    }

    private BagValidation validateChecksums(FileDataProvider fileDataProvider, ManifestChecksum manifestChecksums) {
        ExecutorService executor = getExecutorService();
        BagValidation bagValidationResults = new BagValidation(Optional.empty());
        logger.fine(String.format("action=validateChecksums start name=%s type=%s files=%s", fileDataProvider.getName(), manifestChecksums.getType(), manifestChecksums.getFileChecksums().size()));
        for(Map.Entry<Path, String> checksumEntry:  manifestChecksums.getFileChecksums().entrySet()) {
            Path filePath = checksumEntry.getKey();
            String fileChecksum = checksumEntry.getValue();
            FileValidationResult fileValidationResult = bagValidationResults.addFileResult(filePath);
            Optional<InputStreamProvider> inputStreamProvider = fileDataProvider.getInputStreamProvider(filePath);
            if(inputStreamProvider.isPresent()) {
                FileChecksumValidationJob validationJob = new FileChecksumValidationJob(inputStreamProvider.get(), filePath, fileChecksum, manifestChecksums.getType(), fileValidationResult);
                executor.execute(validationJob);
            } else {
                fileValidationResult.setError(getMessage("bagit.validation.file.not.found", filePath, fileDataProvider.getName()));
            }

        }

        executor.shutdown();
        try {
            while (!executor.awaitTermination(jobWaitIntervalInSeconds, TimeUnit.SECONDS)) {
                logger.fine(String.format("action=validateChecksums result=waiting-completion name=%s type=%s files=%s", fileDataProvider.getName(), manifestChecksums.getType(), manifestChecksums.getFileChecksums().size()));
                if(bagValidationResults.errors() > maxErrors) {
                    logger.info(String.format("action=validateChecksums result=max-errors-reached name=%s type=%s files=%s bagValidationResults=%s", fileDataProvider.getName(), manifestChecksums.getType(), manifestChecksums.getFileChecksums().size(), bagValidationResults.report()));
                    executor.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, String.format("action=validateChecksums result=error message=unable-to-complete-checksums name=%s type=%s files=%s", fileDataProvider.getName(), manifestChecksums.getType(), manifestChecksums.getFileChecksums().size()), e);
            executor.shutdownNow();
            return new BagValidation(Optional.of(getMessage("bagit.validation.exception", fileDataProvider.getName())));
        }

        logger.fine(String.format("action=validateChecksums completed file=%s name=%s files=%s", fileDataProvider.getName(), manifestChecksums.getType(), manifestChecksums.getFileChecksums().size()));
        return bagValidationResults;
    }

    // Visible for testing
    ExecutorService getExecutorService() {
        return Executors.newFixedThreadPool(validatorJobPoolSize);
    }

    // Visible for testing
    String getMessage(String propertyKey, Object... parameters){
        List<String> parameterList = Arrays.stream(parameters).map(param -> param.toString()).collect(Collectors.toList());
        return BundleUtil.getStringFromBundle(propertyKey, parameterList);
    }
}
