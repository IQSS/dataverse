package edu.harvard.iq.dataverse.api.converters;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.api.dto.RorEntryDTO;
import edu.harvard.iq.dataverse.persistence.ror.RorData;
import edu.harvard.iq.dataverse.persistence.ror.RorLabel;
import edu.harvard.iq.dataverse.ror.RorConverter;
import edu.harvard.iq.dataverse.search.ror.RorDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

class RorConverterTest {

    private final RorConverter rorConverter = new RorConverter();

    @Test
    void toEntity() {
        //given
        final RorEntryDTO rorEntryDTO = new RorEntryDTO();
        final String rorId = "/0123884";
        rorEntryDTO.setId(rorId);
        final String acronym = "acronym";
        rorEntryDTO.setAcronyms(new String[]{acronym});
        final String alias = "alias";
        rorEntryDTO.setAliases(new String[]{alias});
        final RorEntryDTO.City city = new RorEntryDTO.City();
        city.setCity("city");
        rorEntryDTO.setCities(new RorEntryDTO.City[]{city});
        final String testName = "testName";
        rorEntryDTO.setName(testName);
        final RorEntryDTO.Label label = new RorEntryDTO.Label();
        label.setLabel("label");
        rorEntryDTO.setLabels(new RorEntryDTO.Label[]{label});
        final String link = "link";
        rorEntryDTO.setLinks(new String[]{link});
        final RorEntryDTO.Country country = new RorEntryDTO.Country();
        final String countryName = "countryName";
        country.setCountryName(countryName);
        final String countryCode = "countryCode";
        country.setCountryCode(countryCode);
        rorEntryDTO.setCountry(country);

        //when
        final RorData rorData = rorConverter.toEntity(rorEntryDTO);

        //then
        Assertions.assertThat(rorData).extracting(RorData::getRorId).isEqualTo(rorId.substring(1));
        Assertions.assertThat(acronym).isIn(rorData.getAcronyms());
        Assertions.assertThat(alias).isIn(rorData.getNameAliases());
        Assertions.assertThat(city.getCity()).isIn(rorData.getCity());
        Assertions.assertThat(rorData).extracting(RorData::getName).isEqualTo(testName);

        final Set<String> extractedLabelNames = rorData.getLabels().stream().map(RorLabel::getLabel).collect(Collectors.toSet());
        Assertions.assertThat(label.getLabel()).isIn(extractedLabelNames);
        Assertions.assertThat(link).isIn(rorData.getWebsite());
        Assertions.assertThat(rorData.getCountryCode()).isEqualTo(countryCode);
        Assertions.assertThat(rorData.getCountryName()).isEqualTo(countryName);
    }

    @Test
    void toSolrDto() {
        //given
        final String label = "label";
        final String acronym = "acronym";
        final String alias = "alias";
        final String ror = "ror";
        final String name = "name";
        final String countryName = "countryName";
        final String countryCode = "countryCode";
        final String website = "website";
        final String city = "city";
        final RorData rorData = new RorData(ror, name, countryName, countryCode, website, city,
                                            Sets.newHashSet(alias),
                                            Sets.newHashSet(acronym),
                                            Sets.newHashSet(new RorLabel(label, "code")));

        //when
        final RorDto convertedDto = rorConverter.toSolrDto(rorData);

        //then
        Assertions.assertThat(convertedDto.getRorId()).isEqualTo(ror);
        Assertions.assertThat(convertedDto.getRorUrl()).isEqualTo("https://ror.org/" + ror);
        Assertions.assertThat(convertedDto.getLabels().get(0)).isEqualTo(label);
        Assertions.assertThat(convertedDto.getAcronyms().get(0)).isEqualTo(acronym);
        Assertions.assertThat(convertedDto.getNameAliases().get(0)).isEqualTo(alias);
        Assertions.assertThat(convertedDto.getCountryCode()).isEqualTo(countryCode);
        Assertions.assertThat(convertedDto.getCountryName()).isEqualTo(countryName);
        Assertions.assertThat(convertedDto.getName()).isEqualTo(name);
        Assertions.assertThat(convertedDto.getCity()).isEqualTo(city);
        Assertions.assertThat(convertedDto.getWebsite()).isEqualTo(website);

    }
}