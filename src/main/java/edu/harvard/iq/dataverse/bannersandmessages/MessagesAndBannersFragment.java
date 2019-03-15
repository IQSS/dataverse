package edu.harvard.iq.dataverse.bannersandmessages;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.bannersandmessages.banners.BannerDAO;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.ImageWithLinkDto;
import edu.harvard.iq.dataverse.bannersandmessages.messages.DataverseTextMessageServiceBean;

import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Responsible for displaying messages and banners across the dataverse.
 */
@ViewScoped
@Named("MessagesAndBannersFragment")
public class MessagesAndBannersFragment implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(MessagesAndBannersFragment.class.getCanonicalName());

    @EJB
    private DataverseTextMessageServiceBean textMessageService;

    @Inject
    private BannerDAO bannerDAO;
    
    @Inject
    private DataverseSession dataverseSession;


    public List<String> textMessages(Long dataverseId) {
        return textMessageService.getTextMessagesForDataverse(dataverseId, dataverseSession.getLocaleCode());
    }

    public List<ImageWithLinkDto> banners(Long dataverseId) {
        return bannerDAO.getBannersForDataverse(dataverseId, dataverseSession.getLocaleCode());
    }

    public void redirect(String link) throws IOException {

        if (!link.startsWith("http")) {
            link = "http://" + link;
        }
        FacesContext.getCurrentInstance().getExternalContext().redirect(link);
    }
}
