package edu.harvard.iq.dataverse.consent.action;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import io.vavr.control.Option;

import javax.ejb.Stateful;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Stateful
public class ConsentActionFactory {

    private MailService mailService;
    private DataverseDao dataverseDao;

    private Map<ConsentActionType, Action> consentActions = new HashMap<>();

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

        return Option.of(consentActions.get(consentActionType))
                .getOrElse(() -> loadAllActions(authenticatedUser).get(consentActionType));
    }

    // -------------------- PRIVATE --------------------

    private Map<ConsentActionType, Action> loadAllActions(AuthenticatedUser authenticatedUser) {
        consentActions.put(ConsentActionType.SEND_NEWSLETTER_EMAIL, new SendNewsletterEmailAction(mailService,
                                                                                                  dataverseDao.findRootDataverse().getName(),
                                                                                                  authenticatedUser));
        return consentActions;
    }
}
