package edu.harvard.iq.dataverse.dataverse.messages;

import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataverse.messages.dto.DataverseTextMessageDto;
import org.apache.commons.lang.StringUtils;
import org.primefaces.model.LazyDataModel;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@ViewScoped
@Named("TextMessagePage")
public class TextMessagePage implements Serializable {

    private long dataverseId;

    @EJB
    private LazyDataverseTextMessage lazydataverseTextMessages;

    @Inject
    private DataverseTextMessageServiceBean textMessageService;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    public String init() {
        lazydataverseTextMessages.setDataverseId(dataverseId);

        if (!permissionsWrapper.canIssueEditDataverseTextMessages(dataverseId)) {
            return permissionsWrapper.notAuthorized();
        }

        return StringUtils.EMPTY;
    }

    public String newTextMessagePage() {
        return "/dataverse-editTextMessages.xhtml?dataverseId=" + dataverseId + "&faces-redirect=true";
    }

    public String reuseTextMessage(String textMessageId) {
        return "/dataverse-editTextMessages.xhtml?dataverseId=" + dataverseId +
                "&id=" + textMessageId + "&faces-redirect=true";
    }

    public long getDataverseId() {
        return dataverseId;
    }

    public LazyDataModel<DataverseTextMessageDto> getLazydataverseTextMessages() {
        return lazydataverseTextMessages;
    }

    public DataverseTextMessageServiceBean getTextMessageService() {
        return textMessageService;
    }

    public void setDataverseId(long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public void setLazydataverseTextMessages(LazyDataverseTextMessage lazydataverseTextMessages) {
        this.lazydataverseTextMessages = lazydataverseTextMessages;
    }
}
