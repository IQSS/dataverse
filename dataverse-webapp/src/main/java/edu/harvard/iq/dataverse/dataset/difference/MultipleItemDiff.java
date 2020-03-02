package edu.harvard.iq.dataverse.dataset.difference;

import java.util.List;

public class MultipleItemDiff<T> {

    private List<T> oldValues;
    private List<T> newValues;

    // -------------------- CONSTRUCTORS --------------------

    public MultipleItemDiff(List<T> oldValue, List<T> newValue) {
        this.oldValues = oldValue;
        this.newValues = newValue;
    }

    // -------------------- GETTERS --------------------

    public List<T> getOldValue() {
        return oldValues;
    }

    public List<T> getNewValue() {
        return newValues;
    }
}
