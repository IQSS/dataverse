/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.GuestbookResponse;
import edu.harvard.iq.dataverse.GuestbookServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.PermissionServiceBean;
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
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author skraffmi
 */
public class MoveDatasetCommandTest {
        Dataset moved, movedResponses;
    	Dataverse root, childA, childB, grandchildAA, childDraft, grandchildBB;
	DataverseEngine testEngine;
        MetadataBlock blockA, blockB, blockC, blockD;
        AuthenticatedUser auth, nobody;
        Guestbook gbA, gbB, gbC;
        GuestbookResponse gbResp;
        @Context
        protected HttpServletRequest httpRequest;
	
    @Before
    public void setUp() {

        auth = makeAuthenticatedUser("Super", "User");
        auth.setSuperuser(true);
        nobody = makeAuthenticatedUser("Nick", "Nobody");
        nobody.setSuperuser(false);

        
        
        root = new Dataverse();
        root.setName("root");
        root.setId(1l);
        root.setPublicationDate(new Timestamp(new Date().getTime()));
        
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
        
        gbA= new Guestbook();
        gbA.setId(1l);
        gbB= new Guestbook();
        gbB.setId(2l);
        gbC= new Guestbook();
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
            public GuestbookServiceBean guestbooks() {
                return new GuestbookServiceBean() {
                    @Override
                    public Long findCountResponsesForGivenDataset(Long guestbookId, Long datasetId) {
                        //We're going to fake a response for a dataset with responses
                        if(datasetId == 1){
                            return new Long(0);
                        } else{
                            return new Long(1);
                        }
                    }
                };
            }
            
            @Override
            public IndexServiceBean index(){
                return new IndexServiceBean(){
                    @Override
                    public void asyncIndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp){
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
	
	/**
	 * Moving ChildB to ChildA
	 * @throws Exception - should not throw an exception
	 */
    @Test
    public void testValidMove() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, childA, null));

        assertEquals(childA, moved.getOwner());

    }
    
    /**
	 * Moving  grandchildAA
	 * Guestbook is not null because target includes it.
	 */
    @Test
    public void testKeepGuestbook() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, grandchildAA, null));

        assertNotNull(moved.getGuestbook());

    }
    
        /**
	 * Moving to grandchildBB
	 * Guestbook is not null because target inherits it.
	 */
    
    @Test
    public void testKeepGuestbookInherit() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, grandchildBB, null));

        assertNotNull(moved.getGuestbook());

    }
    
    
    /**
	 * Moving to ChildB
	 * Guestbook is null because target does not include it
	 */
    @Test
    public void testRemoveGuestbook() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, childB, true));
        assertNull( moved.getGuestbook());

    }
    	
	
	/**
	 * Moving DS to its owning DV 
        * @throws IllegalCommandException
	 */
    @Test(expected = IllegalCommandException.class)
    public void testInvalidMove() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                new MoveDatasetCommand(aRequest, moved, root, false));
        fail();
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
    @Ignore
    @Test(expected = PermissionException.class)
    public void testAuthenticatedUserWithNoRole() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(nobody, httpRequest);
        testEngine.submit(
                new MoveDatasetCommand(aRequest, moved, childA, null));
        fail();
    }

    /**
     * Moving a dataset without being an AuthenticatedUser fails with
     * PermissionException.
     *
     * @throws java.lang.Exception
     */
    @Test(expected = PermissionException.class)
    public void testNotAuthenticatedUser() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(GuestUser.get(), httpRequest);
        testEngine.submit(
                new MoveDatasetCommand(aRequest, moved, root, null));
        fail();
    }
    
    	/**
	 * Moving published  DS to unpublished DV
        * @throws IllegalCommandException
	 */
    @Test(expected = IllegalCommandException.class)
    public void testInvalidMovePublishedToUnpublished() throws Exception {
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                new MoveDatasetCommand(aRequest, moved, childDraft, null));
        fail();
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
