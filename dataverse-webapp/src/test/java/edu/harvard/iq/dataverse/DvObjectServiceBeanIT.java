package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DvObjectServiceBeanIT extends WebappArquillianDeployment {

    @Inject
    private DvObjectServiceBean dvObjectServiceBean;

    @Inject
    private DataFileServiceBean dataFileService;

    // -------------------- TESTS --------------------

    @Test
    public void getObjectPathsByIds() {
        //given
        Set<Long> objectsIds = new HashSet<>();
        objectsIds.add(68L);
        objectsIds.add(51L);

        Map<Long, String> expectedResults = new HashMap<>();
        expectedResults.put(68L, "/1/19/68");
        expectedResults.put(51L, "/1/51");

        //when
        Map<Long, String> results = dvObjectServiceBean.getObjectPathsByIds(objectsIds);

        //then
        Assertions.assertEquals(expectedResults, results);
    }

    @Test
    public void getDataverseHierarchyFor() {
        // given
        DataFile dataFile = dataFileService.find(53L);

        // when
        String result = dvObjectServiceBean.getDataverseHierarchyFor(dataFile);

        // then
        Assertions.assertEquals("/root/unreleased", result);
    }
}
