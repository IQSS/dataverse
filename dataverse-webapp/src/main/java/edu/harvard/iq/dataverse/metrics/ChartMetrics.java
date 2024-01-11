package edu.harvard.iq.dataverse.metrics;

import java.util.Objects;

public class ChartMetrics {

    private int year;
    private int month;
    private Long count;

    public ChartMetrics(int year, int month, Long count) {
        this.year = year;
        this.month = month;
        this.count = count;
    }

    public ChartMetrics(int year, Long count) {
        this.year = year;
        this.count = count;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChartMetrics that = (ChartMetrics) o;
        return year == that.year &&
                month == that.month &&
                Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, count);
    }

    @Override
    public String toString() {
        return "" + year;
    }
}
