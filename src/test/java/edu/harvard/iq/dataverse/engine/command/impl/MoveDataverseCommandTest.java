package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.NoOpTestEntityManager;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeAuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexBatchServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class MoveDataverseCommandTest {

    Dataverse root, childA, childB, grandchildAA, childC, grandchildCC, childD, grandchildDD, childE, grandchildEE, childF;
    Dataset datasetC, datasetCC;
    Guestbook gbA;
    Template templateA;
    MetadataBlock mbA, mbB;
    DataverseEngine testEngine;
    AuthenticatedUser auth, nobody;
    protected HttpServletRequest httpRequest;

    @Before
    public void setUp() {
        // authentication 
        auth = makeAuthenticatedUser("Super", "User");
        auth.setSuperuser(true);
        nobody = makeAuthenticatedUser("Nick", "Nobody");
        nobody.setSuperuser(false);

        // Dataverses
        root = new Dataverse();
        root.setName("root");
        root.setId(1l);
        root.setPublicationDate(new Timestamp(new Date().getTime()));
        
        childA = new Dataverse();
        childA.setOwner( root );
        childA.setId(2l);
        
        childB = new Dataverse();
        childB.setOwner( root );
        childB.setId(3l);
        
        grandchildAA = new Dataverse();
        grandchildAA.setOwner( childA );
        grandchildAA.setId(4l);

        childC = new Dataverse();
        childC.setOwner(root);
        childC.setId(5l);
        
        grandchildCC = new Dataverse();
        grandchildCC.setOwner(childC);
        grandchildCC.setId(6l);
        
        childD = new Dataverse();
        childD.setOwner(root);
        childD.setId(7l);
        
        grandchildDD = new Dataverse();
        grandchildDD.setOwner(childD);
        grandchildDD.setId(8l);
        
        childE = new Dataverse();
        childE.setOwner(root);
        childE.setId(9l);
        
        grandchildEE = new Dataverse();
        grandchildEE.setOwner(childE);
        grandchildEE.setId(10l);
                
        // Datasets
        datasetC = new Dataset();
        datasetC.setOwner(childC);
        datasetC.setId(1l);
        
        datasetCC = new Dataset();
        datasetCC.setOwner(grandchildCC);
        datasetCC.setId(2l);
        
        // Guestbooks
        gbA= new Guestbook();
        gbA.setId(1l);
        gbA.setDataverse(childC);
        
        List<Guestbook> gbs = new ArrayList<>();
        gbs.add(gbA);
        childC.setGuestbooks(gbs);
        childC.setGuestbookRoot(true);
        grandchildCC.setGuestbookRoot(false);
        datasetC.setGuestbook(gbA);
        datasetCC.setGuestbook(gbA);
        
        List<Guestbook> noneGb = new ArrayList();
        root.setGuestbooks(noneGb);
        childA.setGuestbooks(noneGb);
        grandchildAA.setGuestbooks(noneGb);
        childB.setGuestbooks(noneGb);
        grandchildCC.setGuestbooks(noneGb);
        childD.setGuestbooks(noneGb);
        grandchildDD.setGuestbooks(noneGb);
        
        // Templates
        List<Template> ts = new ArrayList<>();
        templateA = new Template();
        templateA.setName("TemplateA");
        templateA.setDataverse(childD);
        ts.add(templateA);
        childD.setTemplates(ts);
        childD.setTemplateRoot(true);
        grandchildDD.setTemplateRoot(false);
        grandchildDD.setDefaultTemplate(templateA);
        
        List<Template> noneT = new ArrayList<>();
        root.setTemplates(noneT);
        childA.setTemplates(noneT);
        grandchildAA.setTemplates(noneT);
        childB.setTemplates(noneT);
        childC.setTemplates(noneT);
        grandchildCC.setTemplates(noneT);
        grandchildDD.setTemplates(noneT);
        
        // Metadata blocks
        List<MetadataBlock> mbsE = new ArrayList<>();
        List<MetadataBlock> mbsEE = new ArrayList<>();
        mbA = new MetadataBlock();
        mbA.setOwner(root);
        mbA.setId(1l);
        mbA.setName("mbA");
        mbB = new MetadataBlock();
        mbB.setOwner(childE);
        mbB.setId(2l);
        mbB.setName("mbB");
        mbsE.add(mbB);
        mbsEE.add(mbA);
        mbsEE.add(mbB);
        childE.setMetadataBlocks(mbsE);
        childE.setMetadataBlockRoot(true);
        grandchildEE.setMetadataBlockRoot(false);
        grandchildEE.setMetadataBlocks(mbsEE);
            
        testEngine = new TestDataverseEngine( new TestCommandContext(){
            @Override
            public DataverseServiceBean dataverses() {
                return new DataverseServiceBean(){
                    @Override
                    public Dataverse save(Dataverse dataverse) {
                            // no-op. The superclass accesses databases which we don't have.
                            return dataverse;
                    }
                    @Override 
                    public Dataverse find(Object pk) {
                    // fake this for what we need
                        if (pk instanceof Long) {
                            if ((Long)pk == 10) {
                                return grandchildEE;
                            }
                        }
                        return new Dataverse();
                    }
                    @Override
                    public List<Dataverse> findByOwnerId(Long ownerId) {
                        return new ArrayList<>();
                    }
                    @Override
                    public List<Long> findAllDataverseDataverseChildren(Long dvId) {
                        // fake this for what we need 
                        List<Long> fakeChildren = new ArrayList<>();
                        if (dvId == 9){ 
                            fakeChildren.add(grandchildEE.getId());
                        }
                        return fakeChildren;
                    }
                    @Override
                    public List<Long> findAllDataverseDatasetChildren(Long dvId) {
                        // fake this for what we need
                        List<Long> fakeChildren = new ArrayList<>();
                        if (dvId == 6) {
                            fakeChildren.add(datasetCC.getId());
                        }
                        return fakeChildren;
                    }
                };
            }
            @Override
            public IndexServiceBean index(){
                return new IndexServiceBean(){
                    @Override
                    public Future<String> indexDataverse(Dataverse dataverse){
                        return null;
                    }

                    @Override
                    public void asyncIndexDataset(Dataset dataset, boolean doNormalSolrDocCleanUp){
                    }
                    @Override
                    public Future<String> indexDataverseInNewTransaction(Dataverse dataverse){
                        return null;
                    }

                    @Override
                    public void indexDatasetInNewTransaction(Long id){
                    }                    
                };

            }
            @Override
            public IndexBatchServiceBean indexBatch(){
                return new IndexBatchServiceBean(){
                    @Override
                    public void indexDataverseRecursively(Dataverse dataverse) {

                    }
                };

            }
            @Override
            public DatasetServiceBean datasets() {
                return new DatasetServiceBean() {
                    @Override
                    public List<Dataset> findByOwnerId(Long ownerId) {
                        return new ArrayList<>();
                    }
                    @Override
                    public Dataset find(Object pk) {
                        // fake this for what we need
                        if (pk instanceof Long) {
                            if ((Long)pk == 2) {
                                return datasetCC;
                            }
                        }
                        return new Dataset();
                    }
                };
            }
            @Override
            public EntityManager em() {
                return new NoOpTestEntityManager();
            }
            @Override
            public DataverseLinkingServiceBean dvLinking() {
                return new DataverseLinkingServiceBean() {
                    
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
        System.out.println("testValidMove");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);

        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childB, childA, null));

        assertEquals( childA, childB.getOwner() );
        assertEquals( Arrays.asList(root, childA), childB.getOwners() );
        
        // move back
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childB, root, null));
        
        assertEquals( root, childB.getOwner() );
        assertEquals( Arrays.asList(root), childB.getOwners() );
    }

    /**
     * Moving ChildA to its child (illegal).
     */
    @Test( expected=IllegalCommandException.class )
    public void testInvalidMove() throws Exception {
        System.out.println("testInvalidMove");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childA, grandchildAA, null));
        fail();
    }
    
    /**
     * Calling API as a non super user (illegal).
     */
    @Test(expected = PermissionException.class)
    public void testNotSuperUser() throws Exception {
        System.out.println("testNotSuperUser");
        DataverseRequest aRequest = new DataverseRequest(nobody, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childB, childA, null));
        fail();
    }
    
    @Test( expected=IllegalCommandException.class )
    public void testMoveIntoSelf() throws Exception {
        System.out.println("testMoveIntoSelf");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childB, childB, null));
        fail();
    }
    
    @Test( expected=IllegalCommandException.class )
    public void testMoveIntoParent() throws Exception {
        System.out.println("testMoveIntoParent");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildAA, childA, null));
        fail();
    }
    
    @Test
    public void testKeepGuestbook() throws Exception {
        System.out.println("testKeepGuestbook");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childC, childB, null));
        assertNotNull(datasetC.getGuestbook());
        
        // move back
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childC, root, null));
        assertEquals( root, childC.getOwner() );
    }
    
    @Test(expected = IllegalCommandException.class)
    public void testRemoveGuestbookWithoutForce() throws Exception {
        System.out.println("testRemoveGuestbookWithoutForce");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildCC, root, null));
        fail();
    }
    
    @Test
    public void testRemoveGuestbook() throws Exception {
        System.out.println("testRemoveGuestbook");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildCC, root, true));
        assertNull( datasetCC.getGuestbook());
        
        // move back
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildCC, childC, true));
        assertEquals( childC, grandchildCC.getOwner() );
    }
    
    @Test
    public void testKeepTemplate() throws Exception {
        System.out.println("testKeepTemplate");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childD, childB, null));
        assertNotNull(grandchildDD.getDefaultTemplate());
        
        // move back
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childD, root, null));
        assertEquals( root, childD.getOwner() );
        
    }
    
    @Test(expected = IllegalCommandException.class)
    public void testRemoveTemplateWithoutForce() throws Exception {
        System.out.println("testRemoveTemplateWithoutForce");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildDD, root, null));
        fail();
    }
    
    @Test
    public void testRemoveTemplate() throws Exception {
        System.out.println("testRemoveTemplate");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildDD, root, true));
        assertNull( grandchildDD.getDefaultTemplate());
        
        // move back
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildDD, childD, true));
        assertEquals( childD, grandchildDD.getOwner() );
    }
    
    @Test
    public void testKeepMetadataBlock() throws Exception {
        System.out.println("testKeepMetadataBlock");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childE, childB, null));
        assertEquals(Arrays.asList(mbB), childE.getMetadataBlocks());
        
        // move back
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, childE, root, null));
        assertEquals( root, childE.getOwner() );
    }
    
    @Test(expected = IllegalCommandException.class)
    public void testRemoveMetadataBlockWithoutForce() throws Exception {
        System.out.println("testRemoveMetadataBlockWithoutForce");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildEE, root, null));
        fail();
    }
    
    @Test
    public void testRemoveMetadataBlock() throws Exception {
        System.out.println("testRemoveMetadataBlock");
        DataverseRequest aRequest = new DataverseRequest(auth, httpRequest);
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildEE, root, true));
        assertEquals(Arrays.asList(mbA), grandchildEE.getMetadataBlocks(true));
        // move back
        testEngine.submit(
                        new MoveDataverseCommand(aRequest, grandchildEE, childE, true));
        assertEquals( childE, grandchildEE.getOwner() );
    }
}
