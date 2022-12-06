package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.NoOpTestEntityManager;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.notification.NotificationParameter;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReturnDatasetToAuthorCommandTest {

    private Dataset dataset;
    private DataverseRequest dataverseRequest;
    private TestDataverseEngine testEngine;

    private static final String TEST_MAIL="test@reply.to";

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
                    public Future<String> indexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) {
                        return null;
                    }
                };
            }

            @Override
            public EntityManager em() {
                return new NoOpTestEntityManager();
            }

            @Override
            public DatasetDao datasets() {
                return new DatasetDao() {
                    {
                        em = new NoOpTestEntityManager();
                    }

                    @Override
                    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user) {
                        return null;
                    }

                    @Override
                    public WorkflowComment addWorkflowComment(WorkflowComment comment) {
                        return comment;
                    }

                    @Override
                    public void removeDatasetLocks(Dataset dataset, DatasetLock.Reason aReason) { }
                };
            }

            @Override
            public DataverseRoleServiceBean roles() {
                return new DataverseRoleServiceBean() {

                    @Override
                    public DataverseRole findBuiltinRoleByAlias(BuiltInRole builtInRole) {
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
                    public List<AuthenticatedUser> getUsersWithPermissionOn(Permission permission, DvObject dvObject) {
                        // We only need permissions for notifications, which we are testing in InReviewWorkflowIT.
                        return Collections.emptyList();
                    }
                };
            }
        });
    }

    // -------------------- TESTS --------------------

    @Test(expected = IllegalArgumentException.class)
    public void testDatasetNull()  {
        new ReturnDatasetToAuthorCommand(dataverseRequest, null, createParams("", TEST_MAIL));
    }

    @Test
    public void testReleasedDataset() {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.RELEASED);
        String expected = "This dataset cannot be return to the author(s) because the latest version is not In Review. The author(s) needs to click Submit for Review first.";
        String actual = null;
        try {
            testEngine.submit(new ReturnDatasetToAuthorCommand(dataverseRequest, dataset, createParams("", TEST_MAIL)));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testNotInReviewDataset() {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.DRAFT);
        String expected = "This dataset cannot be return to the author(s) because the latest version is not In Review. The author(s) needs to click Submit for Review first.";
        String actual = null;
        try {
            testEngine.submit(new ReturnDatasetToAuthorCommand(dataverseRequest, dataset, createParams("", TEST_MAIL)));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testAllGood() {
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.DRAFT);
        Dataset updatedDataset = null;
        try {
            testEngine.submit(new AddLockCommand(dataverseRequest, dataset,
                                                 new DatasetLock(DatasetLock.Reason.InReview, dataverseRequest.getAuthenticatedUser())));
            updatedDataset = testEngine.submit(new ReturnDatasetToAuthorCommand(dataverseRequest, dataset,
                    createParams("Update Your Files, Dummy", TEST_MAIL)));
        } catch (CommandException ex) {
            System.out.println("Error updating dataset: " + ex.getMessage());
        }
        assertNotNull(updatedDataset);
    }

    // -------------------- PRIVATE --------------------

    private Map<String, String> createParams(String message, String replyTo) {
        Map<String, String> params = new HashMap<>();
        params.put(NotificationParameter.MESSAGE.key(), message);
        params.put(NotificationParameter.REPLY_TO.key(), replyTo);
        return params;
    }
}
