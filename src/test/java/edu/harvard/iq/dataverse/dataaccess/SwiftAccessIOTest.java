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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private static final Predicate<String> ORPHANS = name -> name.startsWith("orphan");

    private String datasetPrefix() {
        return dataset.getProtocolForFileStorage() + "_"
                + dataset.getAuthorityForFileStorage().replace("/", "_").replace(".", "_") + "_"
                + dataset.getIdentifierForFileStorage() + "_";
    }

    private StoredObject storedObject(String name, Duration age) {
        StoredObject object = mock(StoredObject.class);
        when(object.getName()).thenReturn(datasetPrefix() + name);
        when(object.getLastModifiedAsDate()).thenReturn(Date.from(Instant.now().minus(age)));
        return object;
    }

    private Container givenContainerHolds(StoredObject... objects) throws ReflectiveOperationException {
        Container container = mock(Container.class);
        // Later pages fall through to Mockito's empty collection, which ends the listing.
        when(container.list(eq(datasetPrefix()), isNull(), anyInt())).thenReturn(List.of(objects));
        var containerField = SwiftAccessIO.class.getDeclaredField("swiftContainer");
        containerField.setAccessible(true);
        containerField.set(datasetAccess, container);
        // Skip open(): the injected container is already the one under test.
        datasetAccess.isWriteAccess = true;
        return container;
    }

    @Test
    public void testCleanUp_dryRunSkipsRecentAndUndatedObjects() throws IOException, ReflectiveOperationException {
        StoredObject undated = mock(StoredObject.class);
        when(undated.getName()).thenReturn(datasetPrefix() + "orphan-undated");
        when(undated.getLastModifiedAsDate()).thenThrow(new IllegalStateException("no Last-Modified header"));
        Container container = givenContainerHolds(
                storedObject("orphan-old", Duration.ofDays(30)),
                storedObject("orphan-fresh", Duration.ofHours(2)),
                storedObject("referenced-old", Duration.ofDays(30)),
                undated);

        List<String> reported = datasetAccess.cleanUp(ORPHANS, Duration.ofDays(7), true);

        assertEquals(List.of("orphan-old"), reported);
        verify(container, never()).getObject(anyString());
    }

    @Test
    public void testCleanUp_deletesOnlyOldOrphans() throws IOException, ReflectiveOperationException {
        Container container = givenContainerHolds(
                storedObject("orphan-old", Duration.ofDays(30)),
                storedObject("orphan-fresh", Duration.ofHours(2)));
        StoredObject old = mock(StoredObject.class);
        when(old.exists()).thenReturn(true);
        when(container.getObject(datasetPrefix() + "orphan-old")).thenReturn(old);

        List<String> deleted = datasetAccess.cleanUp(ORPHANS, Duration.ofDays(7), false);

        assertEquals(List.of("orphan-old"), deleted);
        verify(old).delete();
        verify(container, never()).getObject(datasetPrefix() + "orphan-fresh");
    }
}
