package edu.harvard.iq.dataverse.search;

import java.util.ArrayList;
import java.util.List;

public class IndexUtil {

    public static List<Long> findDvObjectIdsToProcessEqualParts(List<Long> dvObjectIds, int startingPoint, int offset) {
        if (startingPoint < 1) {
            int saneStartingPoint = 1;
            startingPoint = saneStartingPoint;
        }
        if (offset < 1) {
            int saneOffset = 1;
            offset = saneOffset;
        }
        List<Long> subsetToProcess = new ArrayList<>();
        for (int i = startingPoint - 1; i < dvObjectIds.size(); i += offset) {
            Long dvObjectId = dvObjectIds.get(i);
            if (dvObjectId > 0) {
                subsetToProcess.add(dvObjectId);
            }
        }
        return subsetToProcess;
    }

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
