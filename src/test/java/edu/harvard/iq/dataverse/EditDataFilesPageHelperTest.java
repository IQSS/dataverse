package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.file.CreateDataFileResult;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author adaybujeda
 */
@RunWith(MockitoJUnitRunner.class)
public class EditDataFilesPageHelperTest {

    private static final String FILENAME = UUID.randomUUID().toString();
    private static final String TYPE = UUID.randomUUID().toString();

    @Mock
    private SettingsWrapper settingsWrapper;

    @InjectMocks
    private EditDataFilesPageHelper target;

    @Test
    public void getHtmlErrorMessage_should_return_null_when_no_error_messages() {
        CreateDataFileResult createDataFileResult = new CreateDataFileResult(FILENAME, TYPE, Collections.emptyList(), Collections.emptyList());

        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.nullValue());
    }

    @Test
    public void getHtmlErrorMessage_should_return_null_when_max_errors_is_0() {
        Mockito.when(settingsWrapper.getInteger(EditDataFilesPageHelper.MAX_ERRORS_TO_DISPLAY_SETTING, EditDataFilesPageHelper.MAX_ERRORS_TO_DISPLAY)).thenReturn(0);
        CreateDataFileResult createDataFileResult = CreateDataFileResult.error(FILENAME, TYPE, Arrays.asList("error1"));

        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.nullValue());
    }

    @Test
    public void getHtmlErrorMessage_should_return_message_when_there_are_errors() {
        Mockito.when(settingsWrapper.getInteger(EditDataFilesPageHelper.MAX_ERRORS_TO_DISPLAY_SETTING, EditDataFilesPageHelper.MAX_ERRORS_TO_DISPLAY)).thenReturn(10);
        CreateDataFileResult createDataFileResult = CreateDataFileResult.error(FILENAME, TYPE, Arrays.asList("error1"));

        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.notNullValue());
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.containsString("error1"));
    }

    @Test
    public void getHtmlErrorMessage_should_return_message_with_MAX_ERRORS_TO_DISPLAY_when_there_are_more_errors() {
        Mockito.when(settingsWrapper.getInteger(EditDataFilesPageHelper.MAX_ERRORS_TO_DISPLAY_SETTING, EditDataFilesPageHelper.MAX_ERRORS_TO_DISPLAY)).thenReturn(2);
        CreateDataFileResult createDataFileResult = CreateDataFileResult.error(FILENAME, TYPE, Arrays.asList("error1", "error2", "error3", "error4"));

        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.notNullValue());
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.containsString("error1"));
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.containsString("error2"));
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.not(Matchers.containsString("error3")));
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.not(Matchers.containsString("error4")));
    }

    @Test
    public void getHtmlErrorMessage_should_escape_html_from_error_messages() {
        Mockito.when(settingsWrapper.getInteger(EditDataFilesPageHelper.MAX_ERRORS_TO_DISPLAY_SETTING, EditDataFilesPageHelper.MAX_ERRORS_TO_DISPLAY)).thenReturn(5);
        CreateDataFileResult createDataFileResult = CreateDataFileResult.error(FILENAME, TYPE, Arrays.asList("<script>alert('hello');</script>"));

        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.notNullValue());
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.not(Matchers.containsString("<script>")));
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.not(Matchers.containsString("</script>")));
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.containsString("&lt;script&gt;"));
        MatcherAssert.assertThat(target.getHtmlErrorMessage(createDataFileResult), Matchers.containsString("&lt;/script&gt;"));
    }

    @Test
    public void consolidateHtmlErrorMessages_should_return_null_when_null_or_no_errors() {
        MatcherAssert.assertThat(target.consolidateHtmlErrorMessages(null), Matchers.nullValue());
        MatcherAssert.assertThat(target.consolidateHtmlErrorMessages(Collections.emptyList()), Matchers.nullValue());
    }

    @Test
    public void consolidateHtmlErrorMessages_should_not_add_anything_when_only_one_error() {
        List<String> errorMessages = Arrays.asList("error1");

        String result = target.consolidateHtmlErrorMessages(errorMessages);

        MatcherAssert.assertThat(result, Matchers.notNullValue());
        MatcherAssert.assertThat(result, Matchers.containsString("error1"));
    }

    @Test
    public void consolidateHtmlErrorMessages_should_add_HTML_break_line_between_errors() {
        List<String> errorMessages = Arrays.asList("error1", "error2");

        String result = target.consolidateHtmlErrorMessages(errorMessages);

        MatcherAssert.assertThat(result, Matchers.notNullValue());
        MatcherAssert.assertThat(result, Matchers.containsString("error1</br>error2"));
    }

}