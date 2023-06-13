package edu.harvard.iq.dataverse.util;

import java.util.Arrays;

public class OptimizedSumStatCalculator {

    // -------------------- LOGIC --------------------

    /**
     * WARNING: This method computes some sample statistics, but alters the input
     * vector by sorting it and replacing nulls with NaNs.
     * Therefore it should be called right before leaving input array
     * to be garbage-collected.
     */
    public static double[] calculateSummaryStatisticsDestructively(Double[] vector) {

        // {"mean", "medn", "mode", "vald", "invd", "min", "max", "stdev"};
        double[] stats = new double[8];

        int invalid = countAndNormalizeInvalidValues(vector);
        stats[4] = invalid;
        int toProcessLength = vector.length - invalid;
        stats[3] = toProcessLength;

        Arrays.sort(vector);

        stats[0] = calculateMean(vector, toProcessLength);
        stats[1] = calculateMedian(vector, toProcessLength);
        stats[2] = 0.0; // OMITTED

        stats[5] = vector[0]; // Min: as array is sorted, it's the first element
        stats[6] = toProcessLength > 0 ? vector[toProcessLength - 1] : Double.NaN; // Max: as array is sorted, it's the last valid element
        stats[7] = Math.sqrt(variance(vector, stats[0], toProcessLength));
        return stats;
    }

    // -------------------- PRIVATE --------------------

    private static int countAndNormalizeInvalidValues(Double[] vector) {
        int counter = 0;
        for (int i = 0; i < vector.length; i++) {
            Double number = vector[i];
            if (number == null || Double.isNaN(number)) {
                counter++;
            }
            if (number == null) {
                vector[i] = Double.NaN;
            }
        }
        return counter;
    }

    private static double calculateMedian(Double[] values, int length) {
        if (length == 0) {
            return Double.NaN;
        }
        if (length == 1) {
            return values[0]; // always return single value for n = 1
        }
        double pos = ((double) length + 1) / 2;
        double fpos = Math.floor(pos);
        int intPos = (int) fpos;
        double dif = pos - fpos;

        double lower = values[intPos - 1];
        double upper = values[intPos];

        return lower + dif * (upper - lower);
    }

    private static double calculateMean(Double[] values, int length) {

        if (values == null || length == 0) {
            return Double.NaN;
        }

        // Compute initial estimate using definitional formula
        double sum = 0.0;
        for (int i = 0; i < length; i++) {
            sum += values[i];
        }
        double xbar = sum / (double) length;

        // Compute correction factor in second pass
        double correction = 0;
        for (int i = 0; i < length; i++) {
            correction += values[i] - xbar;
        }
        return xbar + (correction / (double) length);
    }

    /**
     * This is a method from org.apache.commons.math.stat.StatUtils
     * originally licensed under http://www.apache.org/licenses/LICENSE-2.0
     * slightly modified and optimized for use with Double[] input
     */
    private static double variance(final Double[] values, final double mean, final int length) {
        double var = Double.NaN;
        if (length == 1) {
            var = 0.0;
        } else if (length > 1) {
            double accum = 0.0;
            double dev;
            double accum2 = 0.0;
            for (int i = 0; i < length; i++) {
                dev = values[i] - mean;
                accum += dev * dev;
                accum2 += dev;
            }
            var = (accum - (accum2 * accum2 / (double) length)) / ((double) length - 1.0);
        }
        return var;
    }
}
