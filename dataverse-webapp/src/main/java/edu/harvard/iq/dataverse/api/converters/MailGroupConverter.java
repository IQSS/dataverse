package edu.harvard.iq.dataverse.api.converters;

import edu.harvard.iq.dataverse.api.dto.MailDomainGroupDTO;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;
import edu.harvard.iq.dataverse.persistence.group.MailDomainItem;
import edu.harvard.iq.dataverse.persistence.group.MailDomainProcessingType;

import javax.ejb.Stateless;
import javax.validation.Valid;
import java.util.stream.Collectors;

@Stateless
public class MailGroupConverter {

    // -------------------- LOGIC --------------------

    public MailDomainGroupDTO toDTO(MailDomainGroup group) {
        MailDomainGroupDTO converted = new MailDomainGroupDTO();
        converted.setAlias(group.getPersistedGroupAlias());
        converted.setDisplayName(group.getDisplayName());
        converted.setDescription(group.getDescription());
        converted.setInclusions(group.getInclusionsStream()
                .map(MailDomainItem::getDomain)
                .collect(Collectors.toList()));
        converted.setExclusions(group.getExclusionsStream()
                .map(MailDomainItem::getDomain)
                .collect(Collectors.toList()));
        return converted;
    }

    public MailDomainGroup toEntity(@Valid MailDomainGroupDTO groupDTO) {
        MailDomainGroup converted = new MailDomainGroup();
        converted.setPersistedGroupAlias(groupDTO.getAlias());
        converted.setDisplayName(groupDTO.getDisplayName());
        converted.setDescription(groupDTO.getDescription());
        converted.getDomainItems().addAll(groupDTO.getInclusions().stream()
                .map(i -> new MailDomainItem(i, MailDomainProcessingType.INCLUDE, converted))
                .collect(Collectors.toSet()));
        converted.getDomainItems().addAll(groupDTO.getExclusions().stream()
                .map(e -> new MailDomainItem(e, MailDomainProcessingType.EXCLUDE, converted))
                .collect(Collectors.toSet()));
        return converted;
    }
}
