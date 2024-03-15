package edu.harvard.iq.dataverse.api.imports;

import com.google.common.collect.ImmutableMap;
import org.dspace.xoai.model.oaipmh.MetadataFormat;

import javax.ejb.Stateless;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolver allowing to match incoming {@link MetadataFormat} to a supported {@link HarvestImporterType}.
 */
@Stateless
public class HarvestImporterTypeResolver {
    private final Map<HarvestImporterType, Function<MetadataFormat, Boolean>> matchers = ImmutableMap.of(
            HarvestImporterType.DDI, this::matchDDI,
            HarvestImporterType.DUBLIN_CORE, this::matchDublinCore,
            HarvestImporterType.DATAVERSE_JSON, this::matchDataverseJson
    );

    /**
     * Filter out the list of metadataFormats and return only those which are supported, and we're able to import.
     */
    public List<MetadataFormat> filterSupportedFormats(List<MetadataFormat> metadataFormats) {
        return metadataFormats.stream()
                .filter(format -> matchers.values().stream().anyMatch(f -> f.apply(format)))
                .collect(Collectors.toList());
    }

    /**
     * Resolve the first matching {@link HarvestImporterType} from the provided metadataFormat or empty if none match.
     */
    public Optional<HarvestImporterType> resolveImporterType(MetadataFormat metadataFormat) {
        return matchers.entrySet().stream()
                .filter(entry -> entry.getValue().apply(metadataFormat))
                .map(Map.Entry::getKey)
                .findAny();
    }

    private boolean matchDDI(MetadataFormat metadataFormat) {
        // FIXME: should match on namespace instead of prefix
        String metadataFormatPrefix = metadataFormat.getMetadataPrefix();
        return "ddi".equalsIgnoreCase(metadataFormatPrefix) || "oai_ddi".equals(metadataFormatPrefix)
                || metadataFormatPrefix.toLowerCase().matches("^oai_ddi.*");
    }

    private boolean matchDublinCore(MetadataFormat metadataFormat) {
        // FIXME: should match on namespace instead of prefix
        String metadataFormatPrefix = metadataFormat.getMetadataPrefix();
        return "dc".equalsIgnoreCase(metadataFormatPrefix) || "oai_dc".equals(metadataFormatPrefix);
    }

    private boolean matchDataverseJson(MetadataFormat metadataFormat) {
        String metadataFormatPrefix = metadataFormat.getMetadataPrefix();
        return "dataverse_json".equals(metadataFormatPrefix);
    }
}
