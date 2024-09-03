/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.GuestbookResponse;
import edu.harvard.iq.dataverse.GuestbookServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRole;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.nextId;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 * @author skraffmi
 */
public class MoveDatasetCommandTest {

    Dataset moved, movedResponses, movedPerms, movedSamePerms;
    Dataverse root, childA, childB, grandchildAA, childDraft, grandchildBB, childEditor, sibEditor;
    DataverseEngine testEngine;
    MetadataBlock blockA, blockB, blockC, blockD;
    AuthenticatedUser auth, nobody;
    Guestbook gbA, gbB, gbC;
    GuestbookResponse gbResp;
    @Context
    protected HttpServletRequest httpRequest;

    @BeforeEach
    public void setUp() {

        auth = makeAuthenticatedUser("Super", "User");
        auth.setSuperuser(true);
        nobody = makeAuthenticatedUser("Nick", "Nobody");
        nobody.setSuperuser(false);

        root = new Dataverse();
        root.setName("root");
        root.setId(1l);
        root.setPublicationDate(new Timestamp(new Date().getTime()));
        root.setDefaultContributorRole(roles.findBuiltinRoleByAlias(DataverseRole.CURATOR));

        childA = new Dataverse();
        childA.setName("childA");
        childA.setId(2l);
        childA.setPublicationDate(new Timestamp(new Date().getTime()));

        childB = new Dataverse();
        childB.setName("childB");
        childB.setId(3l);
        childB.setPublicationDate(new Timestamp(new Date().getTime()));

        grandchildAA = new Dataverse();
        grandchildAA.setName("grandchildAA");
        grandchildAA.setId(4l);
        grandchildAA.setPublicationDate(new Timestamp(new Date().getTime()));

        childDraft = new Dataverse();
        childDraft.setName("childDraft");
        childDraft.setId(5l);

        grandchildBB = new Dataverse();
        grandchildBB.setName("grandchildBB");
        grandchildBB.setId(6l);
        grandchildBB.setPublicationDate(new Timestamp(new Date().getTime()));

        childEditor = new Dataverse();
        childEditor.setName("childEditor");
        childEditor.setId(7l);
        childEditor.setDefaultContributorRole(roles.findBuiltinRoleByAlias(DataverseRole.EDITOR));

        sibEditor = new Dataverse();
        sibEditor.setName("sibEditor");
        sibEditor.setId(8l);
        sibEditor.setDefaultContributorRole(roles.findBuiltinRoleByAlias(DataverseRole.EDITOR));

        movedPerms = new Dataset();
        movedPerms.setOwner(childEditor);
        DatasetLock lock = new DatasetLock(DatasetLock.Reason.InReview, nobody, null);
        movedPerms.addLock(lock);

        movedSamePerms = new Dataset();
        movedSamePerms.setOwner(childEditor);
        movedSamePerms.addLock(lock);

        moved = new Dataset();
        moved.setOwner(root);
        moved.setPublicationDate(new Timestamp(new Date().getTime()));
        moved.setId(1l);

        movedResponses = new Dataset();
        movedResponses.setOwner(root);
        movedResponses.setPublicationDate(new Timestamp(new Date().getTime()));
        movedResponses.setId(2l);

        childA.setOwner(root);
        childB.setOwner(root);
        grandchildAA.setOwner(childA);
        grandchildBB.setOwner(childA);
        childDraft.setOwner(childA);

        gbA = new Guestbook();
        gbA.setId(1l);
        gbB = new Guestbook();
        gbB.setId(2l);
        gbC = new Guestbook();
        gbC.setId(3l);

        moved.setGuestbook(gbA);
        movedResponses.setGuestbook(gbA);

        GuestbookResponse gbResp = new GuestbookResponse();
        gbResp.setGuestbook(gbA);
        gbResp.setDataset(movedResponses);

        List<Guestbook> includeA = new ArrayList();
        includeA.add(gbA);
        includeA.add(gbB);

        grandchildAA.setGuestbooks(includeA);

        List<Guestbook> notIncludeA = new ArrayList();
        notIncludeA.add(gbC);
        notIncludeA.add(gbB);

        childB.setGuestbooks(notIncludeA);

        List<Guestbook> none = new ArrayList();
        root.setGuestbooks(none);
        grandchildBB.setGuestbooks(none);
        grandchildBB.setGuestbookRoot(false);
        childA.setGuestbooks(includeA);

        testEngine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public DataverseServiceBean dataverses() {
                return new DataverseServiceBean() {
                    @Override
                    public Dataverse save(Dataverse dataverse) {
                        // no-op. The superclass accesses databases which we don't have.
                        return dataverse;
                    }
                };
            }

            @Override
            public DatasetServiceBean datasets() {
                return new DatasetServiceBean() {
                    @Override
                    public void removeDatasetLocks(Dataset dataset, DatasetLock.Reason aReason) {
                        new HashSet<>(dataset.getLocks()).stream()
                                .filter(l -> l.getReason() == aReason)
                                .forEach(lock -> {
                                    dataset.removeLock(lock);
                                });

                    }
                };
            }

            @Override
            public GuestbookServiceBean guestbooks() {
                return new GuestbookServiceBean() {
                    @Override
                    public Long findCountResponsesForGivenDataset(Long guestbookId, Long datasetId) {
                        //We're going to fake a response for a dataset with responses
                        if (datasetId == 1) {
                            return new Long(0);
                        } else {
                            return new Long(1);
                        }
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
                return new MockEntityManager() {

                };
            }

            @Override
            public PermissionServiceBean permissions() {
                return new PermissionServiceBean() {

                    @Override
                    public boolean isUserAllowedOn(RoleAssignee roleAssignee, Command<?> command, DvObject dvObject) {
                        AuthenticatedUser authenticatedUser = (AuthenticatedUser) roleAssignee;
                        if (authenticatedUser.getFirstName().equals("Super")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                };
            }

        });
    }

    DataverseRoleServiceBean roles = new DataverseRoleServiceBean() {

        List<RoleAssignment> assignments = new LinkedList<>();

        Map<String, DataverseRole> builtInRoles;

        {
            builtInRoles = new HashMap<>();
            builtInRoles.put(DataverseRole.EDITOR, makeRole("default-editor", false));
            builtInRoles.put(DataverseRole.ADMIN, makeRole("default-admin"));
            builtInRoles.put(DataverseRole.MANAGER, makeRole("default-manager"));
            builtInRoles.put(DataverseRole.CURATOR, makeRole("curator"));
        }

        @Override
        public DataverseRole findBuiltinRoleByAlias(String alias) {
            return builtInRoles.get(alias);
        }

        @Override
        public RoleAssignment save(RoleAssignment assignment) {
            assignment.setId(nextId());
            assignments.add(assignment);
            return assignment;
        }

        @Override
        public RoleAssignment save(RoleAssignment assignment, boolean index) {
            return save(assignment);
        }

        @Override
        public List<RoleAssignment> directRoleAssignments(DvObject dvo) {
            // works since there's only one dataverse involved in the context 
            // of this unit test.
            return assignments;
        }

    };

    /**
     * Moving ChildB to ChildA
     *
     * @throws Exception - should not throw an exception
     */
    @Test
    public void testValidMove() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, childA, null));

        assertEquals(childA, moved.getOwner());

    }

    /**
     * Moving grandchildAA Guestbook is not null because target includes it.
     */
    @Test
    public void testKeepGuestbook() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, grandchildAA, null));

        assertNotNull(moved.getGuestbook());

    }

    /**
     * Moving to grandchildBB Guestbook is not null because target inherits it.
     */
    @Test
    public void testKeepGuestbookInherit() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, grandchildBB, null));

        assertNotNull(moved.getGuestbook());

    }

    /**
     * Moving to ChildB Guestbook is null because target does not include it
     */
    @Test
    public void testRemoveGuestbook() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, childB, true));
        assertNull(moved.getGuestbook());

    }

    @Test
    public void testMoveToDifferentPerms() throws Exception {
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, movedPerms, root, true));
        assertTrue(movedPerms.getLocks().isEmpty());
        assertTrue(movedPerms.getOwner().equals(root));
    }

    @Test
    public void testMoveToSamePerms() throws Exception {
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, movedSamePerms, sibEditor, true));
        assertTrue(movedSamePerms.getLocks().size() == 1);
        assertTrue(movedSamePerms.getOwner().equals(sibEditor));
    }

    /**
     * Moving DS to its owning DV
     *
     * @throws IllegalCommandException
     */
    @Test
    void testInvalidMove() {
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        assertThrows(IllegalCommandException.class,
                () -> testEngine.submit(new MoveDatasetCommand(aRequest, moved, root, false)));
    }

    /**
     * Moving a dataset without having enough permission fails with
     * PermissionException.
     *
     * @throws java.lang.Exception
     *
     * Ignoring after permissions change in 47fb045. Did that change make this
     * case untestable? Unclear.
     */
    @Disabled("Unstable test. Disabled since #5115 by @pdurbin. See commit 7a917177")
    @Test
    void testAuthenticatedUserWithNoRole() {

        DataverseRequest aRequest = new DataverseRequest(nobody, httpRequest);
        assertThrows(IllegalCommandException.class,
                () -> testEngine.submit(new MoveDatasetCommand(aRequest, moved, childA, null)));
    }

    /**
     * Moving a dataset without being an AuthenticatedUser fails with
     * PermissionException.
     *
     * @throws java.lang.Exception
     */
    @Test
    void testNotAuthenticatedUser() {

        DataverseRequest aRequest = new DataverseRequest(GuestUser.get(), httpRequest);
        assertThrows(PermissionException.class,
                () -> testEngine.submit(new MoveDatasetCommand(aRequest, moved, root, null)));
    }

    /**
     * Moving published DS to unpublished DV
     *
     * @throws IllegalCommandException
     */
    @Test
    void testInvalidMovePublishedToUnpublished() {
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        assertThrows(IllegalCommandException.class,
                () -> testEngine.submit(new MoveDatasetCommand(aRequest, moved, childDraft, null)));
    }

    private static class EntityManagerImpl implements EntityManager {

        @Override
        public void persist(Object entity) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> T merge(T entity) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void remove(Object entity) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> T find(Class<T> entityClass, Object primaryKey) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> T getReference(Class<T> entityClass, Object primaryKey) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setFlushMode(FlushModeType flushMode) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public FlushModeType getFlushMode() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void lock(Object entity, LockModeType lockMode) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void refresh(Object entity) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void refresh(Object entity, Map<String, Object> properties) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void refresh(Object entity, LockModeType lockMode) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void detach(Object entity) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean contains(Object entity) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public LockModeType getLockMode(Object entity) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setProperty(String propertyName, Object value) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Map<String, Object> getProperties() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Query createQuery(String qlString) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Query createQuery(CriteriaUpdate updateQuery) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Query createQuery(CriteriaDelete deleteQuery) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Query createNamedQuery(String name) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Query createNativeQuery(String sqlString) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Query createNativeQuery(String sqlString, Class resultClass) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Query createNativeQuery(String sqlString, String resultSetMapping) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void joinTransaction() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isJoinedToTransaction() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> T unwrap(Class<T> cls) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object getDelegate() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isOpen() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public EntityTransaction getTransaction() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public EntityManagerFactory getEntityManagerFactory() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public CriteriaBuilder getCriteriaBuilder() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Metamodel getMetamodel() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public EntityGraph<?> createEntityGraph(String graphName) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public EntityGraph<?> getEntityGraph(String graphName) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    private static class MockEntityManager extends EntityManagerImpl {

        @Override
        public <T> T merge(T entity) {
            return entity;
        }

    }
}
