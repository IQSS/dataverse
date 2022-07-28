package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.xlsx;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class XlsxColumnIndexConverter {
    private static final Map<String, Integer> LETTER_TO_NUM = Initializer.initializeLetterToNum();
    private static final Map<Integer, String> NUM_TO_LETTER = Initializer.initializeNumToLetter();
    private static final int BASE = 26;

    // -------------------- LOGIC --------------------

    public int columnToIndex(String column) {
        if (StringUtils.isBlank(column)) {
            return -1;
        }
        int currentPower = column.length() - 1;
        int total = 0;
        for (String digit : column.toUpperCase().split("")) {
            total += letterToNum(digit) * Math.pow(BASE, currentPower);
            currentPower--;
        }
        return total - 1; // Indexes are 0-based, so we're subtracting 1 to convert to
    }

    public String indexToColumn(int index) {
        if (index < 0) {
            return null;
        }
        // This is a bit trickier than converting a number to another base,
        // see: https://en.wikipedia.org/wiki/Bijective_numeration
        int toDivide = index + 1; // Indexes are 0-based, so we're adding 1 to convert from
        int quotient;
        StringBuilder result = new StringBuilder();
        do {
            quotient = (int) Math.ceil((double) toDivide / BASE) - 1;
            result.append(numToLetter(toDivide - quotient * BASE));
            toDivide = quotient;
        } while (quotient > 0);
        return result.reverse()
                .toString();
    }

    // -------------------- PRIVATE --------------------

    private String numToLetter(int num) {
        return getOrThrow(NUM_TO_LETTER, num);
    }

    private int letterToNum(String letter) {
        return getOrThrow(LETTER_TO_NUM, letter);
    }

    private <K, V> V getOrThrow(Map<K, V> map, K key) {
        V result = map.get(key);
        if (result == null) {
            throw new IllegalArgumentException("No mapping for argument: " + key);
        } else {
            return result;
        }
    }

    // -------------------- INNER CLASSES --------------------

    private static class Initializer {
        private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        public static Map<String, Integer> initializeLetterToNum() {
            return Arrays.stream(LETTERS.split(""))
                    .collect(Collectors.toMap(s -> s, s -> LETTERS.indexOf(s) + 1, (prev, next) -> next));
        }

        public static Map<Integer, String> initializeNumToLetter() {
            return IntStream.rangeClosed(1, 26)
                    .boxed()
                    .collect(Collectors.toMap(i -> i, i -> LETTERS.substring(i - 1, i),  (prev, next) -> next));
        }
    }
}
