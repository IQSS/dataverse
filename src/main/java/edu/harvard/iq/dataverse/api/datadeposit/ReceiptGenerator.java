package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import java.util.logging.Logger;
import org.apache.abdera.i18n.iri.IRI;
import org.swordapp.server.DepositReceipt;

public class ReceiptGenerator {

    private static final Logger logger = Logger.getLogger(ReceiptGenerator.class.getCanonicalName());

    DepositReceipt createReceipt(String baseUrl, Dataset dataset) {
        logger.fine("baseUrl was: " + baseUrl);
        DepositReceipt depositReceipt = new DepositReceipt();
        /**
         * @todo is dataset.getGlobalId() being populated properly?
         * https://redmine.hmdc.harvard.edu/issues/3988 ?
         */
        String globalId = dataset.getGlobalId();
        /**
         * @todo should these URLs continue to have "study" in them? Do we need
         * to keep it as "study" for backwards compatibility or it ok to use
         * "dataset"? http://irclog.iq.harvard.edu/dvn/2014-05-14#i_9404
         */
        String editIri = baseUrl + "/edit/study/" + globalId;
        depositReceipt.setEditIRI(new IRI(editIri));
        /**
         * @todo: should setLocation depend on if an atom entry or a zip file
         * was deposited? (This @todo has been carried over from the DVN 3.x
         * version.)
         */
        depositReceipt.setLocation(new IRI(editIri));
        depositReceipt.setEditMediaIRI(new IRI(baseUrl + "/edit-media/study/" + globalId));
        depositReceipt.setStatementURI("application/atom+xml;type=feed", baseUrl + "/statement/study/" + globalId);
        depositReceipt.addDublinCore("bibliographicCitation", dataset.getLatestVersion().getCitation());
        /**
         * @todo is dataset.getPersistentURL() still returning the database id?
         * https://redmine.hmdc.harvard.edu/issues/3988 ?
         */
        depositReceipt.setSplashUri(dataset.getPersistentURL());
        return depositReceipt;
    }

}
