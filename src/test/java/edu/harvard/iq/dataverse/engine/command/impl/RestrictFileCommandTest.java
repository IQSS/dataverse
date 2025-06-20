/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItemServiceBean;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeDataFile;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeDataset;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 *
 * @author sarahferry
 */
@ExtendWith(MockitoExtension.class)
public class RestrictFileCommandTest {
    
    TestDataverseEngine engine;
    private DataFile file;
    private Dataset dataset;
    boolean restrict = true;
    boolean unrestrict = false;
    static boolean publicInstall = false;
    @Mock
    DataverseFeaturedItemServiceBean dataverseFeaturedItems;
    
    
    public RestrictFileCommandTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
        dataset = makeDataset();
        file = makeDataFile();

        engine = new TestDataverseEngine(new TestCommandContext(){

            @Override
            public DataverseFeaturedItemServiceBean dataverseFeaturedItems() {
                return dataverseFeaturedItems;
            }
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
    
    @AfterEach
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
        dataset.setPublicationDate(new Timestamp(new Date().getTime()));
        // Restrict on a published file will cause the creation of a new draft dataset version
        // and should update only the FileMetadata in the draft version for the test file.
        // So we need to make sure that we use one of the files in the dataset for the test 
        DataFile file = dataset.getFiles().get(0);
        // And make sure is is file.isReleased() == true
        file.setPublicationDate(dataset.getPublicationDate());
        // And set its owner, which is usually done automatically, but not in the test setup
        file.setOwner(dataset);
        //And set the version state to released so that the RestrictFileCommand will create a draft version
        dataset.getLatestVersion().setVersionState(VersionState.RELEASED);
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), restrict);
        engine.submit(cmd);

        //asserts
        assertTrue(!file.isRestricted());
        boolean fileFound = false;
        for (FileMetadata fmw : dataset.getOrCreateEditVersion().getFileMetadatas()) {
            if (file.equals(fmw.getDataFile())) {
                fileFound=true;
                //If it worked fmw is for the draft version and file.getFileMetadata() is for the published version
                assertTrue(fmw.isRestricted());
                assertTrue(!file.getFileMetadata().isRestricted());
                break;
            }
        }
        assertTrue(fileFound);
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
    public void testRestrictRestrictedNewFile() throws Exception {
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
    public void testUnrestrictUnpublishedFile() throws CommandException{
        file.setOwner(dataset);
        file.setRestricted(true);
        file.getFileMetadata().setRestricted(true);
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), unrestrict);
        engine.submit(cmd);
        
        assertTrue(!file.isRestricted());
        assertTrue(!file.getFileMetadata().isRestricted());
        
    }
    
    @Test
    public void testUnrestrictPublishedFile() throws Exception{
        //see comments in testRestrictPublishedFile()
        dataset.setPublicationDate(new Timestamp(new Date().getTime()));
        DataFile file = dataset.getFiles().get(0);
        file.setOwner(dataset);
        file.setPublicationDate(dataset.getPublicationDate());
        file.setRestricted(true);
        file.getFileMetadata().setRestricted(true);
        dataset.getLatestVersion().setVersionState(VersionState.RELEASED);
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), unrestrict);
        engine.submit(cmd);
        //asserts
        assertTrue(file.isRestricted());
        boolean fileFound = false;
        for (FileMetadata fmw : dataset.getOrCreateEditVersion().getFileMetadatas()) {
            if (file.equals(fmw.getDataFile())) {
                fileFound = true;
                assertTrue(!fmw.isRestricted());
                assertTrue(file.getFileMetadata().isRestricted());
                break;
            }
        }
        assertTrue(fileFound);
    }
    
    
    @Test
    public void testUnrestrictNewFile() throws Exception {
        file.setRestricted(true);
        file.getFileMetadata().setRestricted(true);
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), unrestrict);
        engine.submit(cmd);
        assertTrue(!file.isRestricted());
        assertTrue(!file.getFileMetadata().isRestricted());
    }
    
    @Test
    public void testUnrestrictUnrestrictedFile() throws Exception {
        file.setOwner(dataset);
        String expected = "File " + file.getDisplayName() + " is already unrestricted";
        String actual = null;
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), unrestrict);
        try {
            engine.submit(cmd);
        } catch (CommandException ex) {
            actual = ex.getMessage();
        }
        
        assertEquals(expected, actual);
        
    }
    
    @Test
    public void testUnrestrictUnrestrictedNewFile() throws Exception {
        
        String expected = "File " + file.getDisplayName() + " is already unrestricted";
        String actual = null;
        RestrictFileCommand cmd = new RestrictFileCommand(file, makeRequest(), unrestrict);
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
