package edu.harvard.iq.dataverse.dataverse.messages.dto;

import edu.harvard.iq.dataverse.dataverse.messages.DataverseTextMessage;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateless
public class DataverseMessagesMapper {

    public DataverseTextMessageDto dataverseTextMessageToDto(DataverseTextMessage dataverseTextMessage) {
        DataverseTextMessageDto dataverseTextMessageDto = new DataverseTextMessageDto();

        dataverseTextMessageDto.setActive(dataverseTextMessage.isActive());
        dataverseTextMessageDto.setFromTime(dataverseTextMessage.getFromTime());
        dataverseTextMessageDto.setToTime(dataverseTextMessage.getToTime());

        Set<DataverseLocalizedMessageDto> dataverseLocalizedMessageDto = new HashSet<>();
        dataverseTextMessage.getDataverseLocalizedMessages()
                .forEach(dlm -> dataverseLocalizedMessageDto.add(new DataverseLocalizedMessageDto(dlm.getLocale(), dlm.getMessage())));

        dataverseTextMessageDto.setDataverseLocalizedMessage(dataverseLocalizedMessageDto);

        return dataverseTextMessageDto;
    }

    public List<DataverseTextMessageDto> DataverseTextMessageToDtos(List<DataverseTextMessage> dataverseTextMessages) {
        List<DataverseTextMessageDto> dataverseTextMessageDtos = new ArrayList<>();

        dataverseTextMessages.forEach(dataverseTextMessage ->
                dataverseTextMessageDtos.add(this.dataverseTextMessageToDto(dataverseTextMessage)));

        return dataverseTextMessageDtos;
    }
}
