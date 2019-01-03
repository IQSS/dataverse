package edu.harvard.iq.dataverse.dataverse.messages;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.util.List;
import java.util.logging.Logger;

@ViewScoped
@Named("TextMessagesFragment")
public class TextMessagesFragment implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(TextMessagesFragment.class.getCanonicalName());

    @EJB
    private DataverseTextMessageServiceBean textMessageService;

    public List<String> textMessages(Long dataverseId) {
        return textMessageService.getTextMessagesForDataverse(dataverseId);
    }
}
