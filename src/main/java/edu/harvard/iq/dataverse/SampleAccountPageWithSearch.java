package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.logging.Logger;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ViewScoped
@Named("SampleAccountPageWithSearch")
public class SampleAccountPageWithSearch {

    private static final Logger logger = Logger.getLogger(SampleAccountPageWithSearch.class.getCanonicalName());

    @Inject
    DataverseSession session;

    private User dataverseUser;

    public void init() {
        logger.info("init called");
        if (dataverseUser == null) {
            logger.info("dataverseUser is null");
            dataverseUser = session.getUser();
        }
        logger.info("user: " + dataverseUser.getDisplayInfo().getTitle() );
    }

    public User getDataverseUser() {
        return dataverseUser;
    }

    public void setDataverseUser(User dataverseUser) {
        this.dataverseUser = dataverseUser;
    }

}
