package edu.harvard.iq.dataverse.search;

import java.util.ArrayList;
import java.util.List;

public class IndexUtil {

    public static List<Long> findDvObjectIdsToProcessMod(List<Long> dvObjectIds, long mod, long which) {
        List<Long> subsetToProcess = new ArrayList<>();
        for (Long dvObjectId : dvObjectIds) {
            Long toAdd = dvObjectId % mod;
            if (toAdd.equals(which)) {
                subsetToProcess.add(dvObjectId);
            }
        }
        return subsetToProcess;
    }

}
