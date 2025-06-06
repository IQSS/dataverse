package edu.harvard.iq.dataverse.featured;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class DataverseFeaturedItemTest {

    DataverseFeaturedItem dfi = new DataverseFeaturedItem();
    Dataset ds = new Dataset();
    DataFile df = new DataFile();
    String validTypes = Arrays.stream(DataverseFeaturedItem.TYPES.values()).map(t -> t.name()).collect(Collectors.joining(", "));

    @BeforeEach
    public void setUp() {
        dfi.setType("custom");
        dfi.setDvObject(null);
    }
    @Test
    public void test_validTypeAndDvObject() {
        // set a Dataset
        ds.setPublicationDate(Timestamp.from(Instant.now()));
        dfi.setDvObject("Dataset", ds);
        assertEquals("dataset", dfi.getType());
        assertEquals(ds, dfi.getDvObject());

        // update with Datafile
        dfi.setDvObject("Datafile", df);
        assertEquals("datafile", dfi.getType());
        assertEquals(df, dfi.getDvObject());
    }

    @Test
    public void test_typeDvObjectMismatch() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> dfi.setDvObject("Dataverse", new Dataset()));
        String msg = BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.typeAndDvObjectMismatch");
        assertEquals(msg, exception.getMessage());
        assertEquals("custom", dfi.getType());
        assertNull(dfi.getDvObject());
    }

    @Test
    public void test_typeWithNullDvObject() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> dfi.setDvObject("Dataverse", null));
        String msg = BundleUtil.getStringFromBundle("find.dvo.error.dvObjectNotFound", List.of("unknown"));
        assertEquals(msg, exception.getMessage());
        assertEquals("custom", dfi.getType());
        assertNull(dfi.getDvObject());
    }

    @Test
    public void test_typeWithInvalidType() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> dfi.setDvObject("xdataset", new Dataset()));
        String msg = BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.invalidType", List.of(validTypes));
        assertEquals(msg, exception.getMessage());
        assertEquals("custom", dfi.getType());
        assertNull(dfi.getDvObject());
    }
}
