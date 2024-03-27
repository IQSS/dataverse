package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.util.bagit.BagValidation.FileValidationResult;
import edu.harvard.iq.dataverse.util.bagit.data.FileDataProvider.InputStreamProvider;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;

/**
 *
 * @author adaybujeda
 */
public class FileChecksumValidationJobTest {

    @Test
    public void should_set_error_when_checksum_do_not_match() throws Exception {
        FileValidationResult result = new FileValidationResult(Path.of(UUID.randomUUID().toString()));
        FileChecksumValidationJob target = createTarget(result, false, false);
        target.run();

        MatcherAssert.assertThat(result.isError(), Matchers.is(true));
        MatcherAssert.assertThat(result.getMessage(), Matchers.containsString("Invalid checksum"));
    }

    @Test
    public void should_set_error_when_inputstream_provider_throws_error() throws Exception {
        FileValidationResult result = new FileValidationResult(Path.of(UUID.randomUUID().toString()));
        FileChecksumValidationJob target = createTarget(result, false, true);
        target.run();

        MatcherAssert.assertThat(result.isError(), Matchers.is(true));
        MatcherAssert.assertThat(result.getMessage(), Matchers.containsString("Error while calculating checksum"));
    }

    @Test
    public void should_set_success_when_checksum_do_match() throws Exception {
        FileValidationResult result = new FileValidationResult(Path.of(UUID.randomUUID().toString()));
        FileChecksumValidationJob target = createTarget(result, true, false);
        target.run();

        MatcherAssert.assertThat(result.isSuccess(), Matchers.is(true));
        MatcherAssert.assertThat(result.getMessage(), Matchers.nullValue());

    }

    private FileChecksumValidationJob createTarget(FileValidationResult result, boolean validChecksum, boolean throwError) throws Exception {
        Path filePath = result.getFilePath();
        BagChecksumType bagChecksumType = BagChecksumType.asList().get(new Random().nextInt(BagChecksumType.asList().size()));
        String checksum = validChecksum ? bagChecksumType.getInputStreamDigester().digest(IOUtils.toInputStream(filePath.toString(), "UTF-8")) : "invalid";
        InputStreamProvider provider = throwError ? new ExceptionStreamProvider() : () -> IOUtils.toInputStream(filePath.toString(), "UTF-8");
        return new FileChecksumValidationJob(provider, filePath, checksum, bagChecksumType, result);
    }

    private static class ExceptionStreamProvider implements InputStreamProvider {
        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException("Error");
        }
    }

}