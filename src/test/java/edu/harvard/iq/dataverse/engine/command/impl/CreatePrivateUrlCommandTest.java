package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreatePrivateUrlCommandTest {

    private TestDataverseEngine testEngine;
    private Dataset dataset;
    private final Long privateUrlAlreadyExists = 1l;
    private final Long latestVersionIsNotDraft = 2l;
    private final Long createDatasetLong = 3l;
    private final Long versionIsReleased = 4l;
    
    
    @BeforeEach
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
        PrivateUrl privateUrl = null;
        try {
            privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset, false));
            //Should not get here - exception expected
            assertTrue(false);
        } catch (CommandException ex) {
        }
        assertNull(privateUrl);
    }

    @Test
    public void testAlreadyExists() {
        dataset.setId(privateUrlAlreadyExists);
        PrivateUrl privateUrl = null;
        try {
            privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset, false));
            assertTrue(false);
        } catch (CommandException ex) {
        }
        assertNull(privateUrl);
    }

    @Test
    public void testAttemptCreatePrivateUrlOnNonDraft() {
        dataset = new Dataset();
        List<DatasetVersion> versions = new ArrayList<>();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        versions.add(datasetVersion);
        dataset.setVersions(versions);
        dataset.setId(latestVersionIsNotDraft);
        PrivateUrl privateUrl = null;
        try {
            privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset, false));
            assertTrue(false);
        } catch (CommandException ex) {
        }
        assertNull(privateUrl);
    }

    @Test
    public void testCreatePrivateUrlSuccessfully() throws CommandException {
        dataset = new Dataset();
        dataset.setId(createDatasetLong);
        PrivateUrl privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset, false));
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
    public void testCreateAnonymizedAccessPrivateUrlSuccessfully() throws CommandException {
        dataset = new Dataset();
        dataset.setId(createDatasetLong);
        PrivateUrl privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset, true));
        assertNotNull(privateUrl);
        assertNotNull(privateUrl.getDataset());
        assertNotNull(privateUrl.getRoleAssignment());
        PrivateUrlUser expectedUser = new PrivateUrlUser(dataset.getId());
        assertEquals(expectedUser.getIdentifier(), privateUrl.getRoleAssignment().getAssigneeIdentifier());
        assertEquals(expectedUser.isSuperuser(), false);
        assertEquals(expectedUser.isAuthenticated(), false);
        assertEquals(expectedUser.getDisplayInfo().getTitle(), "Private URL Enabled");
        assertNotNull(privateUrl.getToken());
        assertTrue(privateUrl.isAnonymizedAccess());
        assertEquals("https://dataverse.example.edu/privateurl.xhtml?token=" + privateUrl.getToken(), privateUrl.getLink());
    }
    
    @Test
    public void testAttemptCreateAnonymizedAccessPrivateUrlOnReleased() {
        dataset = new Dataset();
        List<DatasetVersion> versions = new ArrayList<>();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        DatasetVersion datasetVersion2 = new DatasetVersion();
        
        versions.add(datasetVersion);
        versions.add(datasetVersion2);
        dataset.setVersions(versions);
        dataset.setId(versionIsReleased);
        PrivateUrl privateUrl = null;
        try {
            privateUrl = testEngine.submit(new CreatePrivateUrlCommand(null, dataset, true));
            assertTrue(false);
        } catch (CommandException ex) {
        }
        assertNull(privateUrl);
    }

}
