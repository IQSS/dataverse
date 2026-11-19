package edu.harvard.iq.dataverse.jsf.collections;

import com.microsoft.playwright.Page;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionPage;
import edu.harvard.iq.dataverse.jsf.support.pages.PermissionsPage;
import org.junit.jupiter.api.DisplayName;

public class RolesJT {

    @JsfTest
    @DisplayName("Assign and remove a role on a collection")
    public void testAssignAndRemoveRole(Page page, JsfFixtures fixtures) {
        // Role changes are made on a throwaway collection, never on the root collection.
        String alias = fixtures.createCollection().alias();

        CollectionPage collection = new CollectionPage(page);
        collection.open(alias);
        collection.openEditMenuItem("managePermissions");

        PermissionsPage permissions = new PermissionsPage(page);
        permissions.assignRole(":authenticated-users", "Member");
        permissions.removeRole(":authenticated-users", "Member");
    }
}
