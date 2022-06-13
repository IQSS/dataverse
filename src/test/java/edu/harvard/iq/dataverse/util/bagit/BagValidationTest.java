package edu.harvard.iq.dataverse.util.bagit;

import edu.harvard.iq.dataverse.util.bagit.BagValidation.FileValidationResult;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author adaybujeda
 */
public class BagValidationTest {

    private static final Path FILE_PATH = Path.of(UUID.randomUUID().toString());

    @Test
    public void success_should_be_true_when_no_error_message() {
        BagValidation target = new BagValidation(Optional.empty());

        MatcherAssert.assertThat(target.success(), Matchers.is(true));
        MatcherAssert.assertThat(target.getErrorMessage().isPresent(), Matchers.is(false));
        MatcherAssert.assertThat(target.getFileResults().isEmpty(), Matchers.is(true));
    }

    @Test
    public void success_should_be_true_when_no_error_message_has_file_validations_without_errors() {
        BagValidation target = new BagValidation(Optional.empty());
        FileValidationResult result = target.addFileResult(FILE_PATH);
        result.setSuccess();

        MatcherAssert.assertThat(target.success(), Matchers.is(true));
        MatcherAssert.assertThat(target.getErrorMessage().isPresent(), Matchers.is(false));
        MatcherAssert.assertThat(target.getFileResults().size(), Matchers.is(1));
    }

    @Test
    public void success_should_be_false_when_error_message() {
        BagValidation target = new BagValidation(Optional.of("Error message"));

        MatcherAssert.assertThat(target.success(), Matchers.is(false));
        MatcherAssert.assertThat(target.getErrorMessage().isPresent(), Matchers.is(true));
        MatcherAssert.assertThat(target.getFileResults().isEmpty(), Matchers.is(true));
    }

    @Test
    public void success_should_be_false_when_no_error_message_but_has_file_validation_errors() {
        BagValidation target = new BagValidation(Optional.empty());
        FileValidationResult result = target.addFileResult(FILE_PATH);
        result.setError();

        MatcherAssert.assertThat(target.success(), Matchers.is(false));
        MatcherAssert.assertThat(target.getErrorMessage().isPresent(), Matchers.is(false));
        MatcherAssert.assertThat(target.getFileResults().size(), Matchers.is(1));
    }

    @Test
    public void report_should_return_total_file_validation_and_total_success_validations() {
        BagValidation target = new BagValidation(Optional.empty());
        target.addFileResult(Path.of(UUID.randomUUID().toString())).setSuccess();
        target.addFileResult(Path.of(UUID.randomUUID().toString())).setError();

        MatcherAssert.assertThat(target.report(), Matchers.containsString("success=false"));
        MatcherAssert.assertThat(target.report(), Matchers.containsString("fileResultsItems=2"));
        MatcherAssert.assertThat(target.report(), Matchers.containsString("fileResultsSuccess=1"));
    }

}