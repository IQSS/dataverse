package edu.harvard.iq.dataverse.settings;

import java.util.function.Supplier;

class LazyLoaded<T> {

    private final Supplier<T> loader;
    private T value;

    LazyLoaded(Supplier<T> loader) {
        this.loader = loader;
    }

    T get() {
        if (value == null) {
            value = loader.get();
        }
        return value;
    }
}
