package edu.harvard.iq.dataverse.privateurl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.RoleAssignment;

/**
 * Dataset authors can create and send a Private URL to a reviewer to see the
 * lasted draft of their dataset (even if the dataset has never been published)
 * without having to create an account. When the dataset is published, the
 * Private URL is deleted.
 */
public class PrivateUrl {

    private final Dataset dataset;
    private final RoleAssignment roleAssignment;
    /**
     * The unique string of characters in the Private URL that associates it
     * (the link) with a particular dataset.
     *
     * The token is also available at roleAssignment.getPrivateUrlToken().
     */
    private final String token;
    /**
     * This is the link that the reviewer will click.
     *
     * @todo This link should probably be some sort of URL object rather than a
     * String.
     */
    private final String link;

    public PrivateUrl(RoleAssignment roleAssignment, Dataset dataset, String dataverseSiteUrl) {
        this.token = roleAssignment.getPrivateUrlToken();
        this.link = dataverseSiteUrl + "/previewurl.xhtml?token=" + token;
        this.dataset = dataset;
        this.roleAssignment = roleAssignment;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public RoleAssignment getRoleAssignment() {
        return roleAssignment;
    }

    public String getToken() {
        return token;
    }

    public String getLink() {
        return link;
    }

    public boolean isAnonymizedAccess() {
        return roleAssignment.isAnonymizedAccess();
    }

}
