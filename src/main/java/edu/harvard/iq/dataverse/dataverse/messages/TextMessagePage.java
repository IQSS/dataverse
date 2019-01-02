package edu.harvard.iq.dataverse.dataverse.messages;

import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseLocalizedMessageDto;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseMessagesMapper;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseTextMessageDto;
import org.apache.commons.compress.utils.Sets;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@ViewScoped
@Named("TextMessagePage")
public class TextMessagePage implements Serializable {

    @Inject
    private DataverseTextMessageServiceBean dataverseTextMessageService;

    @Inject
    private DataverseMessagesMapper dataverseMessagesMapper;

    private long dataverseId;
    private List<DataverseTextMessageDto> dataverseTextMessage;

    @SuppressWarnings("Duplicates")
    public void init() {
        /*List<DataverseTextMessage> dataverseTextMessages = dataverseTextMessageService.fetchAllTextMessagesForDataverse(dataverseId);
        dataverseTextMessage = dataverseMessagesMapper.DataverseTextMessageToDtos(dataverseTextMessages);*/

        HashSet<DataverseLocalizedMessageDto> set1 =
                Sets.newHashSet(new DataverseLocalizedMessageDto("pl", "hello"),
                        new DataverseLocalizedMessageDto("en", "arigato"));

        HashSet<DataverseLocalizedMessageDto> set2 = Sets.newHashSet(new DataverseLocalizedMessageDto("pl", "hello NEIN"),
                new DataverseLocalizedMessageDto("en", "arigato gozaimasu"));

        DataverseTextMessageDto dtm1 = new DataverseTextMessageDto();
        dtm1.setActive(true);
        dtm1.setFromTime(LocalDateTime.of(1990, 12, 12, 12, 12));
        dtm1.setToTime(LocalDateTime.now());
        dtm1.setDataverseLocalizedMessage(set1);

        DataverseTextMessageDto dtm2 = new DataverseTextMessageDto();
        dtm2.setActive(true);
        dtm2.setFromTime(LocalDateTime.of(1990, 12, 12, 12, 12));
        dtm2.setToTime(LocalDateTime.now());
        dtm2.setDataverseLocalizedMessage(set2);

        dataverseTextMessage.add(dtm1);
        dataverseTextMessage.add(dtm2);

    }

    public long getDataverseId() {
        return dataverseId;
    }

    public List<DataverseTextMessageDto> getDataverseTextMessage() {
        return dataverseTextMessage;
    }

    public void setDataverseId(long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public void setDataverseTextMessage(List<DataverseTextMessageDto> dataverseTextMessage) {
        this.dataverseTextMessage = dataverseTextMessage;
    }
}
