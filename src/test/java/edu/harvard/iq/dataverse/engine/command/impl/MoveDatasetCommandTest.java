/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
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
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author skraffmi
 */
public class MoveDatasetCommandTest {
        Dataset moved;
    	Dataverse root, childA, childB, grandchildAA;
	DataverseEngine testEngine;
        MetadataBlock blockA, blockB, blockC, blockD;
        AuthenticatedUser auth, nobody;
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
        childA = new Dataverse();
        childA.setName("childA");
        childA.setId(2l);
        childB = new Dataverse();
        childB.setName("childB");
        childB.setId(3l);
                
        grandchildAA = new Dataverse();
        grandchildAA.setName("grandchildAA");
        grandchildAA.setId(4l);
        
        moved = new Dataset();
        moved.setOwner(root);

        childA.setOwner(root);
        childB.setOwner(root);
        grandchildAA.setOwner(childA);

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
            public IndexServiceBean index(){
                return new IndexServiceBean(){
                    @Override
                    public Future<String> indexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp){
                        return null;
                    }
                };
            }
            
            @Override
            public EntityManager em() {
                return new MockEntityManager() {

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
        testEngine.submit(new MoveDatasetCommand(aRequest, moved, childA));

        assertEquals(childA, moved.getOwner());

    }
	
	/**
	 * Moving DS to its owning DV 
        * @throws java.lang.Exception
	 */
    @Test(expected = IllegalCommandException.class)
    public void testInvalidMove() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                new MoveDatasetCommand(aRequest, moved, root));
        fail();
    }
        
        /**
	 * Moving DS Without Being Super User
         * Fails due to Permission Exception
        * @throws java.lang.Exception
	 */
    @Test(expected = PermissionException.class)
    public void testNotSuperUser() throws Exception {

        DataverseRequest aRequest = new DataverseRequest(nobody, httpRequest);
        testEngine.submit(
                new MoveDatasetCommand(aRequest, moved, root));
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
