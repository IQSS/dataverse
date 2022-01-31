package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.api.dto.DataverseDTO.DataverseContactDTO;
import edu.harvard.iq.dataverse.api.dto.DataverseDTO.DataverseThemeDTO;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DataverseDTOConverterTest {

    @Test
    void convert() {
        // given
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);
        dataverse.setAlias("dataverse-alias");
        dataverse.setDataverseContacts(Stream.of("1", "2", "3")
                .map(n -> new DataverseContact(dataverse, "mail" + n + "@mail.com"))
                .collect(Collectors.toList()));
        dataverse.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED);
        DataverseTheme theme = new DataverseTheme();
        theme.setId(11L);
        theme.setLogo("logo");
        dataverse.setDataverseTheme(theme);

        // when
        DataverseDTO converted = new DataverseDTO.Converter().convert(dataverse);

        // then
        assertThat(converted).extracting(DataverseDTO::getId, DataverseDTO::getAlias, DataverseDTO::getDataverseType)
                .containsExactly(1L, "dataverse-alias", "UNCATEGORIZED");
        assertThat(converted.getDataverseContacts())
                .extracting(DataverseContactDTO::getContactEmail, DataverseContactDTO::getDisplayOrder)
                .containsExactly(tuple("mail1@mail.com", 0), tuple("mail2@mail.com", 0), tuple("mail3@mail.com", 0));
        assertThat(converted.getTheme())
                .extracting(DataverseThemeDTO::getId, DataverseThemeDTO::getLogo)
                .containsExactly(11L, "logo");
    }
}