/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.TestEntityManager;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author skraffmi
 */
public class SubmitDatasetForReviewCommandTest {
    private Dataset dataset;
    private DataverseRequest dataverseRequest;
    private TestDataverseEngine testEngine;
    
    public SubmitDatasetForReviewCommandTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
        

    
    @Before
    public void setUp() {
        dataset = new Dataset();
        
        HttpServletRequest aHttpServletRequest = null;
        dataverseRequest = new DataverseRequest(MocksFactory.makeAuthenticatedUser("First", "Last"), aHttpServletRequest);
        
        testEngine = new TestDataverseEngine(new TestCommandContext() {
            @Override
            public AuthenticationServiceBean authentication(){
                return new AuthenticationServiceBean(){
                    @Override
                    public AuthenticatedUser getAuthenticatedUser(String id){
                        return MocksFactory.makeAuthenticatedUser("First", "Last");
                    }
                };
            }
            
            
            @Override 
            public IndexServiceBean index(){
                return new IndexServiceBean(){
                    @Override     public Future<String> indexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp) {
                     return null;
                    }                   
                };
            } 
            @Override
            public EntityManager em(){
                return new TestEntityManager(){

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
                return new DatasetServiceBean(){
                    @Override
                    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user){
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


        }
        );
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testDatasetNull() {
        dataset = null;
        String expected = "Can't submit for review. Dataset is null.";
        String actual = null;
        Dataset updatedDataset = null;

        try {
            updatedDataset = testEngine.submit(new SubmitDatasetForReviewCommand(dataverseRequest, dataset));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        assertNull(updatedDataset);
    }
    
    @Test
    public void testReleasedDataset(){
               
        dataset.setIdentifier("DUMMY");
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.RELEASED);
        String expected = "Latest version of dataset DUMMY is already released. Only draft versions can be submitted for review.";
        String actual = null;
        Dataset updatedDataset = null;
        try {
            
             updatedDataset = testEngine.submit(new SubmitDatasetForReviewCommand(dataverseRequest, dataset));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);      
        
        
    }
    
    @Test
    public void testDraftDataset(){
               
        dataset.setIdentifier("DUMMY");
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.DRAFT);

        String actual = null;
        Dataset updatedDataset = null;
        try {
            
             updatedDataset = testEngine.submit(new SubmitDatasetForReviewCommand(dataverseRequest, dataset));
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertNotNull(updatedDataset);      
        
        
    }
    



    
}
