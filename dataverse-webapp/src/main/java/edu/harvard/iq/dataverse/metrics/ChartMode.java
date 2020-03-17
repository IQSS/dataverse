package edu.harvard.iq.dataverse.metrics;

import java.util.Arrays;

public enum ChartMode {
    CUMULATIVE("YEAR_CUMULATIVE"),
    YEARLY("YEAR"),
    MONTHLY("MONTH");

    private String mode;

    ChartMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public static ChartMode of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.getMode().equals(name))
                .findFirst().orElseThrow(IllegalAccessError::new);
    }

    @Override
    public String toString() {
        return mode;
    }
}
