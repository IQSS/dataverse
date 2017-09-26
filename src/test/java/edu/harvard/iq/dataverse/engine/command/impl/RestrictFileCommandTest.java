/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeDataFile;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeDataset;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;


/**
 *
 * @author sarahferry
 */
public class RestrictFileCommandTest {
    
    TestDataverseEngine engine;
    private DataFile file;
    private Dataset dataset;
    boolean restrict = true;
    static boolean publicInstall = false;
    
    
    public RestrictFileCommandTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        dataset = makeDataset();
        file = makeDataFile();

        engine = new TestDataverseEngine(new TestCommandContext(){

            @Override
            public SettingsServiceBean settings(){
                return new SettingsServiceBean(){
                    //override for a public install, 
                    //assume false
                    @Override
                    public boolean isTrueForKey(SettingsServiceBean.Key key, boolean defaultValue) {
                        return publicInstall;
                    }
                };
            }
        });
            
    }
    
    @After
    public void tearDown() {
    }
        
    @Test
    public void testRestrictUnpublishedFile() throws CommandException{
        file.setOwner(dataset);
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), restrict);
        engine.submit(cmd);
        
        assertTrue(file.isRestricted());
        assertTrue(file.getFileMetadata().isRestricted());
        
    }
    
    @Test
    public void testRestrictPublishedFile() throws Exception{
        file.setOwner(dataset);
        dataset.setPublicationDate(new Timestamp(new Date().getTime()));
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), restrict);
        engine.submit(cmd);

        //asserts
        assertTrue(!file.isRestricted());
        for (FileMetadata fmw : dataset.getEditVersion().getFileMetadatas()) {
            if (file.equals(fmw.getDataFile())) {
                assertEquals(fmw, file.getFileMetadata());
                assertTrue(fmw.isRestricted());
            }
        }
    }
    
    
    @Test
    public void testRestrictNewFile() throws Exception {
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), restrict);
        engine.submit(cmd);
        assertTrue(file.isRestricted());
        assertTrue(file.getFileMetadata().isRestricted());
    }
    
    @Test
    public void testRestrictRestrictedFile() throws Exception {
        file.setOwner(dataset);
        String expected = "File " + file.getDisplayName() + " is already restricted";
        String actual = null;
        file.setRestricted(true);
        file.getFileMetadata().setRestricted(restrict);
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), restrict);
        try {
            engine.submit(cmd);
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        
        assertEquals(expected, actual);
        
    }
    
    @Test 
    public void testPublicInstall() throws CommandException {
        file.setOwner(dataset);
        String expected = "Restricting files is not permitted on a public installation.";
        String actual = null;
        publicInstall = true;

        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), restrict);
        try {
            engine.submit(cmd);
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        assertEquals(expected, actual);
        publicInstall = false;
    }
    
}
