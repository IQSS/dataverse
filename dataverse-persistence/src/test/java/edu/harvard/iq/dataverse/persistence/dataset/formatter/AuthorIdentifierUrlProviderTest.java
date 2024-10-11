package edu.harvard.iq.dataverse.persistence.dataset.formatter;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorIdentifierUrlProviderTest {

    private AuthorIdentifierUrlProvider provider = new AuthorIdentifierUrlProvider();

    @Test
    public void getUrl() {
        // given
        DatasetField identifier = buildDatasetField("0000-0002-1825-0097", "ORCID");

        // when
        Option<String> url = provider.getUrl(identifier);

        // then
        assertThat(url.toJavaOptional())
                .isNotEmpty()
                .contains("https://orcid.org/0000-0002-1825-0097");
    }

    @Test
    public void getUrl__invalid() {
        // given
        DatasetField identifier = buildDatasetField("XX00-0002-1825-0097", "ORCID");

        // when
        Option<String> url = provider.getUrl(identifier);

        // then
        assertThat(url.toJavaOptional()).isEmpty();
    }

    @Test
    public void getUrl__valid_isni() {
        // given
        DatasetField identifier = buildDatasetField("0000000109010190", "ISNI");

        // when
        Option<String> url = provider.getUrl(identifier);

        // then
        assertThat(url.toJavaOptional())
                .isNotEmpty()
                .contains("http://www.isni.org/isni/0000000109010190");
    }

    @Test
    public void getUrl__invalid_field() {
        // given
        DatasetField invalidField = new DatasetField();
        invalidField.setDatasetFieldType(new DatasetFieldType());
        invalidField.getDatasetFieldType().setName("unknown");

        // when
        Option<String> url = provider.getUrl(invalidField);

        // then
        assertThat(url.toJavaOptional())
                .isEmpty();
    }

    // -------------------- PRIVATE ---------------------

    private static DatasetField buildDatasetField(String identifierValue, String identifierType) {
        DatasetField authorField = new DatasetField();
        authorField.setDatasetFieldType(new DatasetFieldType());
        authorField.getDatasetFieldType().setName("author");

        DatasetField identifierScheme = new DatasetField();
        identifierScheme.setValue(identifierType);
        identifierScheme.setDatasetFieldType(new DatasetFieldType());
        identifierScheme.getDatasetFieldType().setName("authorIdentifierScheme");
        identifierScheme.setDatasetFieldParent(authorField);

        DatasetField identifier = new DatasetField();
        identifier.setValue(identifierValue);
        identifier.setDatasetFieldType(new DatasetFieldType());
        identifier.getDatasetFieldType().setName("authorIdentifier");
        identifier.setDatasetFieldParent(authorField);

        authorField.setDatasetFieldsChildren(List.of(identifierScheme, identifier).asJava());

        return identifier;
    }
}
