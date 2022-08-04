package edu.harvard.iq.dataverse.util.file;

import edu.harvard.iq.dataverse.DataFile;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author adaybujeda
 */
public class CreateDataFileResultTest {

    @Test
    public void error_static_initializer_should_return_error_result() {
        CreateDataFileResult target = CreateDataFileResult.error("filename", "test_type");

        MatcherAssert.assertThat(target.success(), Matchers.is(false));
        MatcherAssert.assertThat(target.getFilename(), Matchers.is("filename"));
        MatcherAssert.assertThat(target.getType(), Matchers.is("test_type"));
        MatcherAssert.assertThat(target.getErrors(), Matchers.is(Collections.emptyList()));
        MatcherAssert.assertThat(target.getDataFiles(), Matchers.nullValue());
    }

    @Test
    public void error_static_initializer_with_messages_should_return_error_result() {
        CreateDataFileResult target = CreateDataFileResult.error("filename", "test_type", Arrays.asList("error1", "error2"));

        MatcherAssert.assertThat(target.success(), Matchers.is(false));
        MatcherAssert.assertThat(target.getFilename(), Matchers.is("filename"));
        MatcherAssert.assertThat(target.getType(), Matchers.is("test_type"));
        MatcherAssert.assertThat(target.getErrors(), Matchers.is(Arrays.asList("error1", "error2")));
        MatcherAssert.assertThat(target.getDataFiles(), Matchers.nullValue());
    }

    @Test
    public void success_static_initializer_should_return_success_result() {
        List<DataFile> dataFiles = Arrays.asList(new DataFile(), new DataFile());
        CreateDataFileResult target = CreateDataFileResult.success("filename", "test_type", dataFiles);

        MatcherAssert.assertThat(target.success(), Matchers.is(true));
        MatcherAssert.assertThat(target.getFilename(), Matchers.is("filename"));
        MatcherAssert.assertThat(target.getType(), Matchers.is("test_type"));
        MatcherAssert.assertThat(target.getErrors(), Matchers.is(Collections.emptyList()));
        MatcherAssert.assertThat(target.getDataFiles(), Matchers.is(dataFiles));
    }

    @Test
    public void getBundleKey_should_return_string_based_on_type() {
        CreateDataFileResult target = new CreateDataFileResult("filename", "test_type", Collections.emptyList(), Collections.emptyList());

        MatcherAssert.assertThat(target.getBundleKey(), Matchers.is("dataset.file.error.test_type"));
    }
}