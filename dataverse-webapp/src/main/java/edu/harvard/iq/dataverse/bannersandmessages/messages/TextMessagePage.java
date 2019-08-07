package edu.harvard.iq.dataverse.bannersandmessages.messages;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.bannersandmessages.messages.dto.DataverseTextMessageDto;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
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
    private Dataverse dataverse;

    @EJB
    private DataverseServiceBean dataverseServiceBean;

    @EJB
    private LazyDataverseTextMessage lazydataverseTextMessages;

    @EJB
    private DataverseTextMessageServiceBean textMessageService;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    public String init() {
        lazydataverseTextMessages.setDataverseId(dataverseId);
        dataverse = dataverseServiceBean.find(dataverseId);

        if (!permissionsWrapper.canEditDataverseTextMessagesAndBanners(dataverseId)) {
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

    public Dataverse getDataverse() {
        return dataverse;
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
