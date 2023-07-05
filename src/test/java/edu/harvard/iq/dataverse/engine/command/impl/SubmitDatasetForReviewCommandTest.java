package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.NoOpTestEntityManager;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.TestEntityManager;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

public class SubmitDatasetForReviewCommandTest {

    private Dataset dataset;
    private DataverseRequest dataverseRequest;
    private TestDataverseEngine testEngine;

    @Before
    public void setUp() {
        dataset = new Dataset();

        HttpServletRequest aHttpServletRequest = null;
        dataverseRequest = new DataverseRequest(MocksFactory.makeAuthenticatedUser("First", "Last"), aHttpServletRequest);

        testEngine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public AuthenticationServiceBean authentication() {
                return new AuthenticationServiceBean() {
                    @Override
                    public AuthenticatedUser getAuthenticatedUser(String id) {
                        return MocksFactory.makeAuthenticatedUser("First", "Last");
                    }
                };
            }

            @Override
            public IndexServiceBean index() {
                return new IndexServiceBean() {
                    @Override
                    public void asyncIndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) {
                    }
                };
            }

            @Override
            public EntityManager em() {
                return new TestEntityManager() {

                    @Override
                    public <T> T merge(T entity) {
                        return entity;
                    }

                    @Override
                    public void flush() {
                        //nothing to do here
                    }

                };
            }

            @Override
            public DatasetServiceBean datasets() {
                return new DatasetServiceBean() {
                    
                    {
                        em = new NoOpTestEntityManager();
                    }
                    
                    @Override
                    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user) {
                        return null;
                    }

                    @Override
                    public DatasetLock addDatasetLock(Long datasetId, DatasetLock.Reason reason, Long userId, String info) {
                        return null;
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
            public PermissionServiceBean permissions() {
                return new PermissionServiceBean() {
                    @Override
                    public List<AuthenticatedUser> getUsersWithPermissionOn(Permission permission, DvObject dvo) {
                        // We only need permissions for notifications, which we are testing in InReviewWorkflowIT.
                        return Collections.emptyList();
                    }
                };
            }

        }
        );
    }

    @Test( expected=IllegalArgumentException.class )
    public void testDatasetNull() {
        new SubmitDatasetForReviewCommand(dataverseRequest, null);
    }
    
    @Test
    public void testReleasedDataset() {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.RELEASED);
        String expected = "Latest version of dataset is already released. Only draft versions can be submitted for review.";
        String actual = null;
        try {
            testEngine.submit(new SubmitDatasetForReviewCommand(dataverseRequest, dataset));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testDraftDataset() {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.DRAFT);
        Dataset updatedDataset = null;
        try {
            updatedDataset = testEngine.submit(new SubmitDatasetForReviewCommand(dataverseRequest, dataset));
        } catch (CommandException ex) {
            System.out.println("Error updating dataset: " + ex.getMessage() );
        }
        assertNotNull(updatedDataset);
    }

}
