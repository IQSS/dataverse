package edu.harvard.iq.dataverse.dataverse.messages.dto;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.dataverse.messages.DataverseTextMessage;
import edu.harvard.iq.dataverse.locale.DataverseLocaleBean;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.EMPTY;

@Stateless
public class DataverseMessagesMapper {

    public DataverseTextMessageDto mapToDto(DataverseTextMessage textMessage) {
        DataverseTextMessageDto dto = new DataverseTextMessageDto();

        dto.setId(textMessage.getId());
        dto.setActive(textMessage.isActive());
        dto.setFromTime(textMessage.getFromTime());
        dto.setToTime(textMessage.getToTime());
        dto.setDataverseId(textMessage.getDataverse().getId());

        List<DataverseLocalizedMessageDto> dataverseLocalizedMessageDto = Lists.newArrayList();
        ofNullable(textMessage.getDataverseLocalizedMessages()).orElseGet(Sets::newHashSet)
                .forEach(dlm -> dataverseLocalizedMessageDto.add(new DataverseLocalizedMessageDto(
                        dlm.getLocale(),
                        dlm.getMessage(),
                        new DataverseLocaleBean().getLanguage(dlm.getLocale()))));
        dto.setDataverseLocalizedMessage(dataverseLocalizedMessageDto);

        return dto;
    }

    public List<DataverseTextMessageDto> mapToDtos(List<DataverseTextMessage> textMessages) {
        List<DataverseTextMessageDto> dtos = new ArrayList<>();

        textMessages.forEach(dataverseTextMessage -> dtos.add(this.mapToDto(dataverseTextMessage)));

        return dtos;
    }

    public List<DataverseLocalizedMessageDto> mapDefaultLocales() {
        Map<String, String> locales = new DataverseLocaleBean().getDataverseLocales();

        return locales.entrySet().stream()
                .map(e -> new DataverseLocalizedMessageDto(e.getKey(), EMPTY, e.getValue()))
                .collect(Collectors.toList());
    }

    public DataverseTextMessageDto mapToNewTextMessage(Long dataverseId) {
        DataverseTextMessageDto dto = new DataverseTextMessageDto();

        dto.setDataverseId(dataverseId);
        dto.setDataverseLocalizedMessage(mapDefaultLocales());

        return dto;
    }

}
