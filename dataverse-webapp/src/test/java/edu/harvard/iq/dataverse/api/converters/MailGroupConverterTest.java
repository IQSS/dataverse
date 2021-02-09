package edu.harvard.iq.dataverse.api.converters;

import edu.harvard.iq.dataverse.api.dto.MailDomainGroupDTO;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.group.MailDomainItem;
import edu.harvard.iq.dataverse.persistence.group.MailDomainProcessingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MailGroupConverterTest {

    MailGroupConverter converter = new MailGroupConverter();

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should convert MailDomainGroup to MailDomainGroupDTO")
    void toDTO() {

        // given
        MailDomainGroup toConvert = new MailDomainGroup();
        toConvert.setPersistedGroupAlias("group1");
        toConvert.setDisplayName("Group 1");
        toConvert.setDescription("Description");
        MailDomainItem itemToInclude = new MailDomainItem("domain1.edu", MailDomainProcessingType.INCLUDE, toConvert);
        MailDomainItem itemToExclude = new MailDomainItem("domain2.edu", MailDomainProcessingType.EXCLUDE, toConvert);
        toConvert.getDomainItems().add(itemToInclude);
        toConvert.getDomainItems().add(itemToExclude);

        // when
        MailDomainGroupDTO converted = converter.toDTO(toConvert);

        // then
        assertThat(converted.getAlias()).isEqualTo(toConvert.getPersistedGroupAlias());
        assertThat(converted.getDisplayName()).isEqualTo(toConvert.getDisplayName());
        assertThat(converted.getDescription()).isEqualTo(toConvert.getDescription());
        assertThat(converted.getInclusions())
                .containsExactly(itemToInclude.getDomain());
        assertThat(converted.getExclusions())
                .containsExactly(itemToExclude.getDomain());
    }

    @Test
    @DisplayName("Should convert MailDomainGroupDTO to MailDomainGroup")
    void toEntity() {

        // given
        MailDomainGroupDTO toConvert = new MailDomainGroupDTO();
        toConvert.setAlias("group1");
        toConvert.setDisplayName("Group 1");
        toConvert.setDescription("Description");
        toConvert.getInclusions().add("include.domain");
        toConvert.getExclusions().add("exclude.domain");

        // when
        MailDomainGroup converted = converter.toEntity(toConvert);

        // then
        assertThat(converted.getPersistedGroupAlias()).isEqualTo(toConvert.getAlias());
        assertThat(converted.getDisplayName()).isEqualTo(toConvert.getDisplayName());
        assertThat(converted.getDescription()).isEqualTo(toConvert.getDescription());
        assertThat(converted.getDomainItems())
                .extracting(MailDomainItem::getDomain, MailDomainItem::getProcessingType, MailDomainItem::getOwner)
                .containsExactlyInAnyOrder(
                        tuple("include.domain", MailDomainProcessingType.INCLUDE, converted),
                        tuple("exclude.domain", MailDomainProcessingType.EXCLUDE, converted));
    }
}