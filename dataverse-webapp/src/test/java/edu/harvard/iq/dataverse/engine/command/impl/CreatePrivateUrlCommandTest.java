package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CreatePrivateUrlCommandTest {

    private TestDataverseEngine testEngine;
    private Dataset dataset;
    private final Long privateUrlAlreadyExists = 1l;
    private final Long latestVersionIsNotDraft = 2l;
    private final Long createDatasetLong = 3l;

    @Before
    public void setUp() {
        dataset = new Dataset();
        testEngine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public PrivateUrlServiceBean privateUrl() {
                return new PrivateUrlServiceBean() {

                    @Override
                    public PrivateUrl getPrivateUrlFromDatasetId(long datasetId) {
                        if (datasetId == privateUrlAlreadyExists) {
                            Dataset dataset = new Dataset();
                            dataset.setId(privateUrlAlreadyExists);
                            String token = null;
                            PrivateUrlUser privateUrlUser = new PrivateUrlUser(datasetId);
                            RoleAssignment roleAssignment = new RoleAssignment(null, privateUrlUser, dataset, token);
                            return new PrivateUrl(roleAssignment, dataset, "FIXME");
                        } else if (datasetId == latestVersionIsNotDraft) {
                            return null;
                        } else {
                            return null;
                        }
                    }

                };
            }

            @Override
            public DataverseRoleServiceBean roles() {
                return new DataverseRoleServiceBean() {

                    @Override
                    public DataverseRole findBuiltinRoleByAlias(String alias) {
                        return new DataverseRole();
                    }

                    @Override
                    public RoleAssignment save(RoleAssignment assignment) {
                        // no-op
                        return assignment;
                    }

                };
            }

            @Override
            public SystemConfig systemConfig() {
                return new SystemConfig() {

                    @Override
                    public String getDataverseSiteUrl() {
                        return "https://dataverse.example.edu";
                    }

                };

            }

        }
        );
    }

    @Test
    public void testDatasetNull() {
        dataset = null;
        String expected = "Can't create Private URL. Dataset is null.";
        String actual = null;
        PrivateUrl privateUrl = null;
        try {
            privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        assertNull(privateUrl);
    }

    @Test
    public void testAlreadyExists() {
        dataset.setId(privateUrlAlreadyExists);
        String expected = "Private URL already exists for dataset id " + privateUrlAlreadyExists + ".";
        String actual = null;
        PrivateUrl privateUrl = null;
        try {
            privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        assertNull(privateUrl);
    }

    @Test
    public void testCreatePrivateUrlSuccessfully_onReleased() {
        // given
        dataset = new Dataset();
        List<DatasetVersion> versions = new ArrayList<>();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        versions.add(datasetVersion);
        dataset.setVersions(versions);
        dataset.setId(latestVersionIsNotDraft);

        // when
        PrivateUrl privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset));

        // then
        assertNotNull(privateUrl);
        assertNotNull(privateUrl.getDataset());
        assertNotNull(privateUrl.getRoleAssignment());

        PrivateUrlUser expectedUser = new PrivateUrlUser(dataset.getId());
        assertEquals(expectedUser.getIdentifier(), privateUrl.getRoleAssignment().getAssigneeIdentifier());
        assertEquals(expectedUser.isSuperuser(), false);
        assertEquals(expectedUser.isAuthenticated(), false);
        assertEquals(expectedUser.getDisplayInfo().getTitle(), "Private URL Enabled");
        assertNotNull(privateUrl.getToken());
        assertEquals("https://dataverse.example.edu/privateurl.xhtml?token=" + privateUrl.getToken(), privateUrl.getLink());
    }

    @Test
    public void testCreatePrivateUrlSuccessfully_onDraft()  {
        dataset = new Dataset();
        dataset.setId(createDatasetLong);
        PrivateUrl privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset));
        assertNotNull(privateUrl);
        assertNotNull(privateUrl.getDataset());
        assertNotNull(privateUrl.getRoleAssignment());
        PrivateUrlUser expectedUser = new PrivateUrlUser(dataset.getId());
        assertEquals(expectedUser.getIdentifier(), privateUrl.getRoleAssignment().getAssigneeIdentifier());
        assertEquals(expectedUser.isSuperuser(), false);
        assertEquals(expectedUser.isAuthenticated(), false);
        assertEquals(expectedUser.getDisplayInfo().getTitle(), "Private URL Enabled");
        assertNotNull(privateUrl.getToken());
        assertEquals("https://dataverse.example.edu/privateurl.xhtml?token=" + privateUrl.getToken(), privateUrl.getLink());
    }

}
