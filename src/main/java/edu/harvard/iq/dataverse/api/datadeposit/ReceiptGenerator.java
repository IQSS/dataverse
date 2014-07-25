package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import java.util.logging.Logger;
import org.apache.abdera.i18n.iri.IRI;
import org.swordapp.server.DepositReceipt;

public class ReceiptGenerator {

    private static final Logger logger = Logger.getLogger(ReceiptGenerator.class.getCanonicalName());

    /**
     * @todo rename to createDatasetReceipt?
     */
    DepositReceipt createReceipt(String baseUrl, Dataset dataset) {
        logger.fine("baseUrl was: " + baseUrl);
        DepositReceipt depositReceipt = new DepositReceipt();
        /**
         * @todo is dataset.getGlobalId() being populated properly?
         * https://github.com/IQSS/dataverse/issues/569 ?
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
         * https://github.com/IQSS/dataverse/issues/569 ?
         */
        depositReceipt.setSplashUri(dataset.getPersistentURL());
        return depositReceipt;
    }

    DepositReceipt createDataverseReceipt(String baseUrl, Dataverse dataverse) {
        logger.fine("baseUrl was: " + baseUrl);
        DepositReceipt depositReceipt = new DepositReceipt();
        String globalId = dataverse.getAlias();
        String collectionIri = baseUrl + "/collection/dataverse/" + globalId;
        depositReceipt.setSplashUri(collectionIri);
        /**
         * @todo We have to include and "edit" IRI or else we get
         * NullPointerException in getAbderaEntry at
         * https://github.com/swordapp/JavaServer2.0/blob/sword2-server-1.0/src/main/java/org/swordapp/server/DepositReceipt.java#L52
         *
         * Do we want to support a replaceMetadata of dataverses?
         *
         * Typically, we only operate on the "collection" IRI for dataverses, to
         * create a dataset.
         */
        String editIri = baseUrl + "/edit/dataverse/" + globalId;
        depositReceipt.setEditIRI(new IRI(editIri));
        return depositReceipt;
    }

}
