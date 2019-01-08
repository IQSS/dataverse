package edu.harvard.iq.dataverse.dataverse.messages;

import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseTextMessageDto;
import org.primefaces.model.LazyDataModel;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@ViewScoped
@Named("TextMessagePage")
public class TextMessagePage implements Serializable {

    private long dataverseId;

    @Inject
    private LazyDataverseTextMessage lazydataverseTextMessages;

    @SuppressWarnings("Duplicates")
    public void init() {

    }

    private void mockData() {
        /*List<DataverseLocalizedMessageDto> set1 =
                Lists.newArrayList(new DataverseLocalizedMessageDto("pl", "hello", "Polosh"),
                        new DataverseLocalizedMessageDto("en", "arigato", "English"));

        List<DataverseLocalizedMessageDto> set2 = Lists.newArrayList(new DataverseLocalizedMessageDto("pl", "hello NEIN,","Polish"),
                new DataverseLocalizedMessageDto("en", "arigato gozaimasu","English"));

        DataverseTextMessageDto dtm1 = new DataverseTextMessageDto();
        dtm1.setActive(true);
        dtm1.setFromTime(Date.from(Instant.ofEpochSecond(123456123456L)));
        dtm1.setToTime(Date.from(Instant.now()));
        dtm1.setDataverseLocalizedMessage(set1);

        DataverseTextMessageDto dtm2 = new DataverseTextMessageDto();
        dtm2.setActive(true);
        dtm2.setFromTime(Date.from(Instant.ofEpochSecond(1231326123456L)));
        dtm2.setToTime(Date.from(Instant.now()));
        dtm2.setDataverseLocalizedMessage(set2);

        dataverseTextMessage.add(dtm1);
        dataverseTextMessage.add(dtm2);*/
    }

    public long getDataverseId() {
        return dataverseId;
    }

    public LazyDataModel<DataverseTextMessageDto> getLazydataverseTextMessages() {
        return lazydataverseTextMessages;
    }

    public void setDataverseId(long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public void setDataverseTextMessage(List<DataverseTextMessageDto> dataverseTextMessage) {
        this.dataverseTextMessage = dataverseTextMessage;
    }

    public String newTextMessagePage() {
        return "/dataverse-editTextMessages.xhtml?dataverseId=" + dataverseId + "&faces-redirect=true";
    }
}
