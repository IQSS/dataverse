/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author oscardssmith
 */
public class SwiftAccessIOTest {

    private SwiftAccessIO<Dataset> datasetAccess;
    private SwiftAccessIO<DataFile> datafileAccess;
    private Dataset dataset;
    private DataFile datafile;
    private SwiftAccessIO swiftAccess;
    

    public SwiftAccessIOTest() {
    }

    @BeforeEach
    public void setUpClass() throws IOException {
        datafile = MocksFactory.makeDataFile();
        dataset = MocksFactory.makeDataset();
        datafile.setOwner(dataset);
        String dummyDriverId="dummy";
        datasetAccess = new SwiftAccessIO<>(dataset, null, dummyDriverId);
        datafileAccess = new SwiftAccessIO<>(datafile, null, dummyDriverId);
        swiftAccess = new SwiftAccessIO();
    }

    /**
     * Test of canRead, canWrite, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testPerms() throws IOException {
        assertEquals(false, datasetAccess.canRead());
        assertEquals(false, datasetAccess.canWrite());
    }
    
    @Test
    public void testIsExpiryExpired() {
        long currentTime = 1502221467;
        assertEquals(false, swiftAccess.isExpiryExpired(60, 1502281, currentTime));
    }
    
    @Test
    public void testGenerateTempUrlExpiry() {
        long currentTime = 1502221467;
        assertEquals(1502281, datafileAccess.generateTempUrlExpiry(60, currentTime));
    }
    
    @Test
    public void testToHexString() {
        String str = "hello";
	byte[] bytes = str.getBytes();
        assertEquals("68656c6c6f", swiftAccess.toHexString(bytes));
    }
    
    @Test
    public void testCalculateRFC2104HMAC() throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        assertEquals("104152c5bfdca07bc633eebd46199f0255c9f49d", swiftAccess.calculateRFC2104HMAC("data", "key"));
    }
}
