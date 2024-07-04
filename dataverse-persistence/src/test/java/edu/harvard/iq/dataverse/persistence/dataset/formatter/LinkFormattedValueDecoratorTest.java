package edu.harvard.iq.dataverse.persistence.dataset.formatter;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import io.vavr.control.Option;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinkFormattedValueDecoratorTest {

    @Mock
    private LinkFormattedValueDecorator.UrlProvider urlProvider;

    @InjectMocks
    private LinkFormattedValueDecorator decorator;

    @Test
    public void decorate() {
        // given
        DatasetField field = new DatasetField();
        when(urlProvider.getUrl(field)).thenReturn(Option.some("http://url.com"));
        // when
        Option<String> decoratedValue = decorator.decorate(field, "value");
        // then
        assertThat(decoratedValue.toJavaOptional())
                .isNotEmpty()
                .contains("<a href=\"http://url.com\" target=\"_blank\">value</a>");

    }

    @Test
    public void decorate__no_url() {
        // given
        DatasetField field = new DatasetField();
        when(urlProvider.getUrl(field)).thenReturn(Option.none());
        // when
        Option<String> decoratedValue = decorator.decorate(field, "value");
        // then
        assertThat(decoratedValue.toJavaOptional()).isEmpty();
    }
}
