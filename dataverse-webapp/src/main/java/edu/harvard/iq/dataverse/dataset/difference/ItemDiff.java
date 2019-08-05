package edu.harvard.iq.dataverse.dataset.difference;

/**
 * Class that contains old and new value
 * of some item that is different between two
 * versions.
 * 
 * @author madryk
 *
 * @param <T> type of item with differences between versions
 */
public class ItemDiff<T> {
    private T oldValue;
    private T newValue;
    
    // -------------------- CONSTRUCTORS --------------------
    
    public ItemDiff(T oldValue, T newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    // -------------------- GETTERS --------------------
    
    public T getOldValue() {
        return oldValue;
    }

    public T getNewValue() {
        return newValue;
    }
}