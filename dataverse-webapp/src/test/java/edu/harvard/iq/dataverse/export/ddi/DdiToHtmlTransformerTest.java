package edu.harvard.iq.dataverse.export.ddi;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class DdiToHtmlTransformerTest {

    private DdiToHtmlTransformer transformer = new DdiToHtmlTransformer();

    @Test
    void transform() throws URISyntaxException, IOException {
        // given & when
        StringWriter output = new StringWriter();
        try (InputStream input = this.getClass().getClassLoader()
                .getResourceAsStream("xml/export/ddi/dataset-forHtml.xml")) {
            transformer.transform(input, output);
        }

        // then
        String transformed = output.toString();
        String expected = IOUtils.toString(this.getClass().getClassLoader()
                .getResource("xml/export/ddi/codebook-result.html").toURI(), StandardCharsets.UTF_8);
        assertThat(transformed).isEqualTo(expected);
    }
}