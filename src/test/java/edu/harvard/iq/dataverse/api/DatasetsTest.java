package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.util.DateUtil;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetsTest {

    /**
     * Test cleanup filter
     */
    @Test
    public void testCleanup() {
        Set<String> datasetFiles = new HashSet<>() {
            {
                add("1837fda0b6c-90779481d439");
                add("1837fda0e17-4b0926f6d44e");
                add("1837fda1b80-46a899909269");
            }
        };
        Set<String> filesOnDrive = new HashSet<>() {
            {
                add("1837fda0b6c-90779481d439");
                add("1837fda0e17-4b0926f6d44e");
                add("1837fda1b80-46a899909269");
                add("prefix_1837fda0b6c-90779481d439");
                add("1837fda0e17-4b0926f6d44e_suffix");
                add("1837fda1b80-extra-46a899909269");
                add("1837fda0e17-4b0926f6d44e.aux");
                add("1837fda1994-5f74d57e6e47");
                add("1837fda17ce-d7b9987fc6e9");
                add("18383198c49-aeda08ccffff");
                add("prefix_1837fda1994-5f74d57e6e47");
                add("1837fda17ce-d7b9987fc6e9_suffix");
                add("18383198c49-extra-aeda08ccffff");
                add("some_other_file");
                add("1837fda17ce-d7b9987fc6e9.aux");
                add("18383198c49.aeda08ccffff");
                add("1837fda17ce-d7b9987fc6xy");
            }
        };

        Predicate<String> toDeleteFilesFilter = Datasets.getToDeleteFilesFilter(datasetFiles);
        Set<String> deleted = filesOnDrive.stream().filter(toDeleteFilesFilter).collect(Collectors.toSet());

        assertEquals(5, deleted.size());
        assertTrue(deleted.contains("1837fda1994-5f74d57e6e47"));
        assertTrue(deleted.contains("1837fda17ce-d7b9987fc6e9"));
        assertTrue(deleted.contains("18383198c49-aeda08ccffff"));
        assertTrue(deleted.contains("1837fda17ce-d7b9987fc6e9_suffix"));
        assertTrue(deleted.contains("1837fda17ce-d7b9987fc6e9.aux"));
    }

    @Test
    public void testValidateInternalTimestampIsNotOutdated() {
        Datasets dsBean = new Datasets();
        Dataset ds = new Dataset();
        DatasetVersion dsv = new DatasetVersion();
        dsv.setDataset(ds);
        ds.setVersions(List.of(dsv));

        // setLastUpdateTime LOCAL TIME ZONE
        String str1 = "2023-10-01T10:00:00+05:00";
        LocalDateTime dt1 = LocalDateTime.parse(str1, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        dsv.setLastUpdateTime(Date.from(dt1.atZone(ZoneId.systemDefault()).toInstant()));

        // Test not Equal
        String str2 = "2023-10-01T11:00:00-08:00";
        try {
            dsBean.validateInternalTimestampIsNotOutdated(ds,str2);
            fail();
        } catch (AbstractApiBean.WrappedResponse e) {
        }

        // Test Equal
        try {
            dsBean.validateInternalTimestampIsNotOutdated(ds,str1);
        } catch (AbstractApiBean.WrappedResponse e) {
            fail();
        }

        // setLastUpdateTime UTC
        String str11 = "2023-10-01T10:00:00Z";
        Date dt11 = DateUtil.parseDate(str11, "yyyy-MM-dd'T'HH:mm:ss'Z'");
        dsv.setLastUpdateTime(dt11);

        // Test Not Equal
        String str12 = "2023-10-01T11:00:00Z";
        try {
            dsBean.validateInternalTimestampIsNotOutdated(ds,str12);
            fail();
        } catch (AbstractApiBean.WrappedResponse e) {
        }

        // Test Equal
        try {
            dsBean.validateInternalTimestampIsNotOutdated(ds,str11);
        } catch (AbstractApiBean.WrappedResponse e) {
            fail();
        }

        // Test BAD Date/Time
        try {
            dsBean.validateInternalTimestampIsNotOutdated(ds,"invalid");
            fail();
        } catch (AbstractApiBean.WrappedResponse e) {
        }
    }
}
