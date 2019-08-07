package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeletePrivateUrlCommandTest {

    private TestDataverseEngine testEngine;
    Dataset dataset;
    private final Long noPrivateUrlToDelete = 1l;
    private final Long hasPrivateUrlToDelete = 2l;

    @Before
    public void setUp() {
        testEngine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public PrivateUrlServiceBean privateUrl() {
                return new PrivateUrlServiceBean() {

                    @Override
                    public PrivateUrl getPrivateUrlFromDatasetId(long datasetId) {
                        if (datasetId == noPrivateUrlToDelete) {
                            return null;
                        } else if (datasetId == hasPrivateUrlToDelete) {
                            Dataset dataset = new Dataset();
                            dataset.setId(hasPrivateUrlToDelete);
                            String token = null;
                            PrivateUrlUser privateUrlUser = new PrivateUrlUser(datasetId);
                            RoleAssignment roleAssignment = new RoleAssignment(null, privateUrlUser, dataset, token);
                            return new PrivateUrl(roleAssignment, dataset, "FIXME");
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
                    public List<RoleAssignment> directRoleAssignments(RoleAssignee roas, DvObject dvo) {
                        RoleAssignment roleAssignment = new RoleAssignment();
                        List<RoleAssignment> list = new ArrayList<>();
                        list.add(roleAssignment);
                        return list;
                    }

                    @Override
                    public void revoke(RoleAssignment ra) {
                        // no-op
                    }

                };
            }

        });
    }

    @Test
    public void testDatasetNull() {
        dataset = null;
        String expected = "Can't delete Private URL. Dataset is null.";
        String actual = null;
        try {
            testEngine.submit(new DeletePrivateUrlCommand(null, dataset));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
    }

}
