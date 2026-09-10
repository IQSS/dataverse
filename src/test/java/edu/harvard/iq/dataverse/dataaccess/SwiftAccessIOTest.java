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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

    private static final Predicate<String> ORPHANS = name -> name.startsWith("orphan");

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
        assertFalse(datasetAccess.canRead());
        assertFalse(datasetAccess.canWrite());
    }
    
    @Test
    public void testIsExpiryExpired() {
        long currentTime = 1502221467;
        assertFalse(swiftAccess.isExpiryExpired(60, 1502281, currentTime));
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

    private StoredObject storedObject(String prefix, String name, Duration age) {
        StoredObject item = mock(StoredObject.class);
        when(item.getName()).thenReturn(prefix + name);
        when(item.getLastModifiedAsDate()).thenReturn(Date.from(Instant.now().minus(age)));
        return item;
    }

    @Test
    public void testCleanUp_dryRunSkipsRecentlyModifiedObjects() throws IOException {
        // datafile is owned by dataset, so both resolve to the same container name.
        String prefix = datafileAccess.getSwiftContainerName() + "/";
        // Build the stored objects before stubbing the container, so their own
        // stubbing does not nest inside the container's.
        List<StoredObject> stored = List.of(
                storedObject(prefix, "orphan-old", Duration.ofDays(30)),
                storedObject(prefix, "orphan-fresh", Duration.ofHours(2)),
                storedObject(prefix, "referenced-old", Duration.ofDays(30)));

        Container container = mock(Container.class);
        when(container.list(anyString(), nullable(String.class), anyInt()))
                .thenReturn(stored)
                .thenReturn(List.of());
        datasetAccess.swiftContainer = container;
        // Skip open(): the container above is the one under test.
        datasetAccess.isWriteAccess = true;

        List<String> reported = datasetAccess.cleanUp(ORPHANS, Duration.ofDays(7), true);

        assertEquals(List.of("orphan-old"), reported);
    }
}
