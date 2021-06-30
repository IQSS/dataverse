package edu.harvard.iq.dataverse.api.dto;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;

public class RorDataResponse implements Serializable {

    private int numberOfUpdatedRecords = 0;
    private SortedMap<String, Integer> stats = new TreeMap<>();

    public RorDataResponse(int numberOfUpdatedRecords, SortedMap<String, Integer> stats) {
        this.numberOfUpdatedRecords = numberOfUpdatedRecords;
        this.stats = stats;
    }

    public int getNumberOfUpdatedRecords() {
        return numberOfUpdatedRecords;
    }

    public SortedMap<String, Integer> getStats() {
        return stats;
    }
}
