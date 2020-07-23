package edu.harvard.iq.dataverse.consent.action;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

@Stateless
public class ConsentActionFactory {

    private MailService mailService;
    private DataverseDao dataverseDao;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ConsentActionFactory() {
    }

    @Inject
    public ConsentActionFactory(MailService mailService, DataverseDao dataverseDao) {
        this.mailService = mailService;
        this.dataverseDao = dataverseDao;
    }

    // -------------------- LOGIC --------------------

    /**
     * Retrives action according to it's type.
     * @param authenticatedUser it is necessary to pass user otherwise it would had to be injected via Session,
     *                          which would heavily bound this class to the idea that user was set in session already and the method position would matter,
     *                          even though it is not a necessity to have an user set in session in time of this method execution.
     */
    public Action retrieveAction(ConsentActionType consentActionType, AuthenticatedUser authenticatedUser) {

        if (ConsentActionType.SEND_NEWSLETTER_EMAIL.equals(consentActionType)) {
            return new SendNewsletterEmailAction(mailService,
                    dataverseDao.findRootDataverse().getName(),
                    authenticatedUser);
        }
        return null;
    }

}
