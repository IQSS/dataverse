package edu.harvard.iq.dataverse.feedback;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseContact;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Contact information of a feedback recipient.
 */
public class FeedbackContact {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackContact.class);

    private final String name;
    private final String email;

    // -------------------- CONSTRUCTORS --------------------

    public FeedbackContact(String email) {
        this.name = null;
        this.email = email;
    }

    public FeedbackContact(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // -------------------- GETTERS --------------------

    public Option<String> getName() {
        return Option.of(name);
    }

    public String getEmail() {
        return email;
    }

    // -------------------- LOGIC --------------------

    public static List<FeedbackContact> fromDataverse(Dataverse dataverse) {
        List<FeedbackContact> dataverseContacts = new ArrayList<>();
        for (DataverseContact dc : dataverse.getDataverseContacts()) {
            FeedbackContact dataverseContact = new FeedbackContact(dc.getContactEmail());
            dataverseContacts.add(dataverseContact);
        }
        return dataverseContacts;
    }

    public static List<FeedbackContact> fromDataset(Dataset dataset) {
        List<FeedbackContact> datasetContacts = new ArrayList<>();
        for (DatasetField dsf : dataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getTypeName().equals(DatasetFieldConstant.datasetContact)) {
                String contactName = null;
                String contactEmail = null;

                for (DatasetField subField : dsf.getDatasetFieldsChildren()) {
                    if (subField.getTypeName().equals(DatasetFieldConstant.datasetContactName)) {
                        contactName = subField.getValue();
                        logger.debug("contactName: {}", contactName);
                    }
                    if (subField.getTypeName().equals(DatasetFieldConstant.datasetContactEmail)) {
                        contactEmail = subField.getValue();
                        logger.debug("contactEmail: {}", contactEmail);
                    }

                }

                if (contactEmail != null) {
                    FeedbackContact datasetContact = new FeedbackContact(contactName, contactEmail);
                    datasetContacts.add(datasetContact);
                }

            }
        }
        return datasetContacts;
    }
}
