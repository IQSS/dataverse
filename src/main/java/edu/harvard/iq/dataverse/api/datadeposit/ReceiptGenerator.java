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
         * @todo is this same as globalId?
         */
        String globalId = dataset.getIdentifier();
        /**
         * @todo should these URLs continue to have "study" in them? Do we need
         * to keep it as "study" for backwards compatibility or it ok to use
         * "dataset"?
         */
        String editIri = baseUrl + "/edit/study/" + globalId;
        depositReceipt.setEditIRI(new IRI(editIri));
        /**
         * @todo: should setLocation depend on if an atom entry or a zip file
         * was deposited?
         */
        depositReceipt.setLocation(new IRI(editIri));
        depositReceipt.setEditMediaIRI(new IRI(baseUrl + "/edit-media/study/" + globalId));
        depositReceipt.setStatementURI("application/atom+xml;type=feed", baseUrl + "/statement/study/" + globalId);
        /**
         * @todo re-enable bibliographicCitation
         */
//        depositReceipt.addDublinCore("bibliographicCitation", study.getLatestVersion().getMetadata().getCitation(false));
        /**
         * @todo re-enable this
         */
//        depositReceipt.setSplashUri(study.getPersistentURL());
        return depositReceipt;
    }

}
