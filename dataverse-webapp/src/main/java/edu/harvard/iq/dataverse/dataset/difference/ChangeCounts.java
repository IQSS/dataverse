package edu.harvard.iq.dataverse.dataset.difference;

/**
 * Class that contains number of values that
 * have been added/removed/changed inside item
 * of some type.
 * 
 * @author madryk
 *
 * @param <T> type of item with changes
 */
public class ChangeCounts<T> {
    private T item;
    private int addedCount;
    private int removedCount;
    private int changedCount;
    
    // -------------------- CONSTRUCTORS --------------------
    
    public ChangeCounts(T item) {
        this.item = item;
    }
    
    // -------------------- GETTERS --------------------
    
    public T getItem() {
        return item;
    }
    public int getAddedCount() {
        return addedCount;
    }
    public int getRemovedCount() {
        return removedCount;
    }
    public int getChangedCount() {
        return changedCount;
    }
    
    // -------------------- LOGIC --------------------
    
    public void incrementAdded(int count) {
        addedCount += count;
    }
    public void incrementRemoved(int count) {
        removedCount += count;
    }
    public void incrementChanged(int count) {
        changedCount += count;
    }
}