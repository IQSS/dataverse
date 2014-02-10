package edu.harvard.iq.dataverse;

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

    private DataverseUser dataverseUser;

    public void init() {
        logger.info("init called");
        if (dataverseUser == null) {
            logger.info("dataverseUser is null");
            dataverseUser = session.getUser();
        }
        logger.info("last name: " + dataverseUser.getLastName());
    }

    public DataverseUser getDataverseUser() {
        return dataverseUser;
    }

    public void setDataverseUser(DataverseUser dataverseUser) {
        this.dataverseUser = dataverseUser;
    }

}
