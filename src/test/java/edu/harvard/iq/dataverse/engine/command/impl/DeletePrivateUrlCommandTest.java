package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class DeletePrivateUrlCommandTest {

    private TestDataverseEngine testEngine;
    Dataset dataset;
    private final Long noPrivateUrlToDelete = 1l;
    private final Long hasPrivateUrlToDelete = 2l;

    @BeforeEach
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

    @Test
    public void testSuccessfulDelete() {
        dataset = new Dataset();
        dataset.setId(hasPrivateUrlToDelete);
        String actual = null;
        try {
            testEngine.submit(new DeletePrivateUrlCommand(null, dataset));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertNull(actual);
        /**
         * @todo How would we confirm that the role assignement is actually
         * gone? Really all we're testing above is that there was no
         * IllegalCommandException from submitting the command.
         */
    }

}
