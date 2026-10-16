package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import edu.harvard.iq.dataverse.jsf.support.CreateDatasetOptions;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import edu.harvard.iq.dataverse.jsf.support.pages.PermissionsPage;
import org.junit.jupiter.api.DisplayName;

public class PermissionsJT {

    @JsfTest
    @DisplayName("Dataset permissions: page lists the existing role assignments")
    public void testDatasetPermissionsPage(Page page, JsfFixtures fixtures) {
        fixtures.createDataset();
        new DatasetPage(page).openPermissions(DatasetPage.PermissionsScope.DATASET);

        PermissionsPage permissions = new PermissionsPage(page);
        assertThat(permissions.assignButton()).isVisible();
        // The creator's own role assignment is always listed.
        assertThat(permissions.assignedRoles().locator("tbody tr").first()).isVisible();
    }

    @JsfTest
    @DisplayName("Dataset permissions: assign and remove a role")
    public void testAssignAndRemoveRole(Page page, JsfFixtures fixtures) {
        fixtures.createDataset();
        new DatasetPage(page).openPermissions(DatasetPage.PermissionsScope.DATASET);

        PermissionsPage permissions = new PermissionsPage(page);
        permissions.assignRole(":authenticated-users", "Curator");
        permissions.removeRole(":authenticated-users", "Curator");
    }

    @JsfTest
    @DisplayName("File permissions: page loads")
    public void testFilePermissionsPage(Page page, JsfFixtures fixtures) {
        fixtures.createDataset(new CreateDatasetOptions().setFiles(TestData.SAMPLE_TEXT));
        new DatasetPage(page).openPermissions(DatasetPage.PermissionsScope.FILE);
        assertThat(new PermissionsPage(page).assignButton()).isVisible();
    }
}
