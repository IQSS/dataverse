package edu.harvard.iq.dataverse.api.imports;

import com.google.common.collect.ImmutableList;
import edu.harvard.iq.dataverse.export.ExporterType;
import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class HarvestImporterTypeResolverTest {

    private final HarvestImporterTypeResolver resolver = new HarvestImporterTypeResolver();

    @Test
    public void filterSupportedFormats() {
        // given
        List<MetadataFormat> allFormats = Arrays.stream(ExporterType.values())
                .map(ExporterType::getPrefix)
                .map(prefix -> new MetadataFormat()
                        .withMetadataPrefix(prefix)
                        .withSchema(prefix + "_schema.xsd")
                        .withMetadataNamespace(prefix + "_namespace"))
                .collect(Collectors.toList());

        // when
        List<MetadataFormat> filtered = resolver.filterSupportedFormats(allFormats);

        // then
        assertThat(filtered.stream().map(MetadataFormat::getMetadataPrefix).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("oai_dc", "DDI", "oai_ddi", "dataverse_json");
    }

    @Test
    public void filterSupportedFormats__empty_list() {
        // given
        List<MetadataFormat> allFormats = Collections.emptyList();

        // when
        List<MetadataFormat> filtered = resolver.filterSupportedFormats(allFormats);

        // then
        assertThat(filtered).isEmpty();
    }

    @Test
    public void filterSupportedFormats__none_match() {
        // given
        List<MetadataFormat> input = ImmutableList.of(new MetadataFormat()
                .withMetadataPrefix("oai_datacite")
                .withMetadataNamespace("http://datacite.org/schema/kernel-3")
                .withSchema("http://schema.datacite.org/meta/kernel-3.1/metadata.xsd"));

        // when
        List<MetadataFormat> filtered = resolver.filterSupportedFormats(input);

        // then
        assertThat(filtered).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "oai_dc,http://www.openarchives.org/OAI/2.0/oai_dc/,http://www.openarchives.org/OAI/2.0/oai_dc.xsd,DUBLIN_CORE",
            "oai_ddi,ddi:codebook:2_5,https://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd,DDI",
            "DDI,ddi:codebook:2_5,https://www.ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd,DDI",
            "dataverse_json,'','',DATAVERSE_JSON"
    })
    public void resolveImporterType(String prefix, String namespace, String schema, HarvestImporterType expected) {
        // given
        MetadataFormat input = new MetadataFormat().withMetadataPrefix(prefix).withMetadataNamespace(namespace).withSchema(schema);

        // when
        Optional<HarvestImporterType> importerType = resolver.resolveImporterType(input);

        // then
        assertThat(importerType).isPresent();
        assertThat(importerType).hasValue(expected);
    }

    @Test
    public void resolveImporterType__unsupported() {
        // given
        MetadataFormat input = new MetadataFormat()
                .withMetadataPrefix("oai_datacite")
                .withMetadataNamespace("http://datacite.org/schema/kernel-3")
                .withSchema("http://schema.datacite.org/meta/kernel-3.1/metadata.xsd");

        // when
        Optional<HarvestImporterType> importerType = resolver.resolveImporterType(input);

        // then
        assertThat(importerType).isEmpty();
    }
}
