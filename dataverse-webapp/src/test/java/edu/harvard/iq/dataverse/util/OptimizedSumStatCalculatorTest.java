package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class OptimizedSumStatCalculatorTest {
    private static final int TEST_SIZE = 1000;

    @Test
    void compareSummaryStatistics() {
        // given
        Random random = new Random(1111111L);
        Double[] vector = random.doubles(TEST_SIZE, 0.0, 1.e10)
                .boxed()
                .toArray(Double[]::new);

        for (int i : random.ints().limit(TEST_SIZE / 20)
                .map(i -> Math.abs(i) % TEST_SIZE)
                .toArray()) {
            vector[i] = i % 2 == 0 ? null : Double.NaN; // Add some special values
        }

        // when
        double[] old = SumStatCalculator.calculateSummaryStatistics(vector);
        double[] optimized = OptimizedSumStatCalculator.calculateSummaryStatisticsDestructively(vector);

        // then
        assertThat(optimized)
                .usingComparatorWithPrecision(1.e-4d) // We add some precision tolerance, as sum of sorted and
                                                      // unsorted array of doubles may differ by a small amount
                .containsExactly(old);
    }
}