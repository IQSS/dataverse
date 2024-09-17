package edu.harvard.iq.dataverse.harvest.client;

/**
 * Harvest result keeping track of preformed operations.
 */
public class HarvesterResult {
    private int numHarvested = 0;
    private int numFailed = 0;
    private int numDeleted = 0;

    // -------------------- GETTERS --------------------

    public int getNumDeleted() {
        return numDeleted;
    }

    public int getNumFailed() {
        return numFailed;
    }

    public int getNumHarvested() {
        return numHarvested;
    }

    // -------------------- LOGIC --------------------

    public void incrementHarvested() {
        numHarvested++;
    }

    public void incrementFailed() {
        numFailed++;
    }

    public void incrementDeleted() {
        numDeleted++;
    }
}
