package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.StringReader;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class DatasetVersionTest {
    
    private static final Logger logger = Logger.getLogger(DatasetVersion.class.getCanonicalName());
    
    @BeforeAll
    public static void setUp() {
        BrandingUtilTest.setupMocks();
    }
    
    @AfterAll
    public static void tearDown() {
        BrandingUtilTest.setupMocks();
    }
    
    @Test
    public void testComparator() {
        DatasetVersion ds1_0 = new DatasetVersion();
        ds1_0.setId(0l);
        ds1_0.setVersionNumber( 1l );
        ds1_0.setMinorVersionNumber( 0l );
        ds1_0.setVersionState(DatasetVersion.VersionState.RELEASED);
        
        DatasetVersion ds1_1 = new DatasetVersion();
        ds1_1.setId(1l);
        ds1_1.setVersionNumber( 1l );
        ds1_1.setMinorVersionNumber( 1l );
        ds1_1.setVersionState(DatasetVersion.VersionState.RELEASED);
        
        DatasetVersion ds2_0 = new DatasetVersion();
        ds2_0.setId(2l);
        ds2_0.setVersionNumber( 2l );
        ds2_0.setMinorVersionNumber( 0l );
        ds2_0.setVersionState(DatasetVersion.VersionState.RELEASED);
        
        DatasetVersion ds_draft = new DatasetVersion();
        ds_draft.setId(3l);
        ds_draft.setVersionState(DatasetVersion.VersionState.DRAFT);
        
        List<DatasetVersion> expected = Arrays.asList( ds1_0, ds1_1, ds2_0, ds_draft );
        List<DatasetVersion> actual = Arrays.asList( ds2_0, ds1_0, ds_draft, ds1_1 );
        Collections.sort(actual, DatasetVersion.compareByVersion);
        assertEquals( expected, actual );
    }

    @Test
    public void testIsInReview() {
        Dataset ds = MocksFactory.makeDataset();
        
        DatasetVersion draft = ds.getCreateVersion(null);
        draft.setVersionState(DatasetVersion.VersionState.DRAFT);
        ds.addLock(new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("Lauren", "Ipsumowitch")));
        assertTrue(draft.isInReview());

        DatasetVersion nonDraft = new DatasetVersion();
        nonDraft.setVersionState(DatasetVersion.VersionState.RELEASED);
        assertEquals(false, nonDraft.isInReview());
        
        ds.addLock(null);
        assertFalse(nonDraft.isInReview());
    }


}
