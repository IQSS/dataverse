package edu.harvard.iq.dataverse.ror;

import edu.harvard.iq.dataverse.api.dto.RorEntryDTO;
import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.persistence.ror.RorLabel;
import edu.harvard.iq.dataverse.search.ror.RorDto;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Simple converter for Ror objects.
 */
@Stateless
public class RorConverter {

    private static final String ROR_URL_PREFIX = "https://ror.org/";

    // -------------------- LOGIC --------------------

    public RorData toEntity(RorEntryDTO entry) {
        RorData converted = new RorData();

        converted.setRorId(extractRor(entry.getId()));
        converted.setName(entry.getName());

        if (entry.getCities().length > 0) {
            converted.setCity(entry.getCities()[0].getCity());
        }

        if (entry.getLinks().length > 0) {
            converted.setWebsite(entry.getLinks()[0]);
        }

        if (entry.getCountry() != null) {
            converted.setCountryName(entry.getCountry().getCountryName());
            converted.setCountryCode(entry.getCountry().getCountryCode());
        }

        converted.getAcronyms().addAll(Arrays.asList(entry.getAcronyms()));
        converted.getNameAliases().addAll(Arrays.asList(entry.getAliases()));
        converted.getLabels().addAll(
                Arrays.stream(entry.getLabels())
                      .map(l -> new RorLabel(l.getLabel(), l.getIso639()))
                      .collect(Collectors.toSet()));

        return converted;
    }

    public RorDto toSolrDto(RorData entry) {
        RorDto converted = new RorDto();

        converted.setRorId(entry.getRorId());
        converted.setRorUrl(ROR_URL_PREFIX + entry.getRorId());
        converted.setName(entry.getName());

        converted.setCity(entry.getCity());
        converted.setWebsite(entry.getWebsite());

        converted.setCountryName(entry.getCountryName());
        converted.setCountryCode(entry.getCountryCode());


        converted.getAcronyms().addAll(entry.getAcronyms());
        converted.getNameAliases().addAll(entry.getNameAliases());
        converted.getLabels().addAll(
                entry.getLabels()
                     .stream()
                      .map(RorLabel::getLabel)
                      .collect(Collectors.toSet()));

        return converted;
    }

    // -------------------- PRIVATE --------------------

    private String extractRor(String rorId) {
        if (StringUtils.isBlank(rorId) || !rorId.contains("/0")) {
            return StringUtils.EMPTY;
        }
        return rorId.substring(rorId.lastIndexOf("/") + 1);
    }
}
