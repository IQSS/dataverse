package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import org.apache.commons.lang3.StringUtils;
import org.dataverse.unf.UNFUtil;
import org.dataverse.unf.UnfException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OptimizedUNFUtilTest {
    private static final int TEST_SIZE = 1000;

    // -------------------- TESTS --------------------

    @Test
    void compareForNumericVector() throws IOException, UnfException {
        // given
        long seed = System.nanoTime();
        Random random = new Random(seed);
        Long[] vector = random.longs(TEST_SIZE).boxed().toArray(Long[]::new);

        // when
        String oldMethodResult = UNFUtil.calculateUNF(vector);
        String optimizedMethodResult = OptimizedUNFUtil.calculateUNF(vector);

        // then
        assertThat(optimizedMethodResult)
                .withFailMessage("Failed for seed: " + seed)
                .isEqualTo(oldMethodResult);
    }

    @Test
    void compareForNumericVector2() throws IOException, UnfException {
        // given
        long seed = System.nanoTime();
        Random random = new Random(seed);
        Long[] vector = random.longs(TEST_SIZE).boxed().toArray(Long[]::new);

        // when
        String oldMethodResult = UNFUtil.calculateUNF(vector);
        String optimizedMethodResult = OptimizedUNFUtil.calculateUNF(vector);

        // then
        assertThat(optimizedMethodResult)
                .withFailMessage("Failed for seed: " + seed)
                .isEqualTo(oldMethodResult);
    }

    @Test
    void compareForStringVector() throws IOException, UnfException {
        // given
        long seed = System.nanoTime();
        Random random = new Random(seed);

        String[] randomParts = { "a48rv97", "23h7c58", "ą³431€ASDG↓¢€", "@#$%fgaj", "SD$€DS34" };
        int length = randomParts.length;

        String[] vector =  Stream.generate(() -> randomParts[random.nextInt(length)] + randomParts[random.nextInt(length)])
                .limit(TEST_SIZE)
                .toArray(String[]::new);

        // when
        String oldMethodResult = UNFUtil.calculateUNF(vector);
        String optimizedMethodResult = OptimizedUNFUtil.calculateUNF(vector);

        // then
        assertThat(optimizedMethodResult)
                .withFailMessage("Failed for seed: " + seed)
                .isEqualTo(oldMethodResult);
    }

    @Test
    void testEquivalence__longAndDouble() throws IOException, UnfException {
        // given
        long seed = System.nanoTime();
        Random random = new Random(seed);
        Long[] vector = random.longs(TEST_SIZE).boxed().toArray(Long[]::new);
        vector[TEST_SIZE / 2] = Long.MAX_VALUE;
        vector[TEST_SIZE / 2 + 1] = Long.MIN_VALUE;

        Double[] doubles = Arrays.stream(vector).map(Double::new).toArray(Double[]::new);

        // when
        String oldMethodResult = UNFUtil.calculateUNF(vector);
        String optimizedMethodResult = OptimizedUNFUtil.calculateUNF(doubles);

        // then
        assertThat(optimizedMethodResult)
                .withFailMessage("Failed for seed: " + seed)
                .isEqualTo(oldMethodResult);
    }

    @Test
    void testEquivalence__floatAndDouble() throws IOException, UnfException {
        // given
        long seed = System.nanoTime();
        Random random = new Random(seed);
        Float[] vector = Stream.generate(random::nextFloat).limit(TEST_SIZE).toArray(Float[]::new);

        vector[TEST_SIZE / 2] = Float.MIN_NORMAL;
        vector[TEST_SIZE / 2 + 1] = Float.NEGATIVE_INFINITY;
        vector[TEST_SIZE / 2 + 2] = Float.POSITIVE_INFINITY;
        vector[TEST_SIZE / 2 + 3] = 0.0f;
        vector[TEST_SIZE / 2 + 4] = -0.0f;
        vector[TEST_SIZE / 2 + 5] = Float.MIN_VALUE;
        vector[TEST_SIZE / 2 + 6] = Float.MAX_VALUE;
        vector[TEST_SIZE / 2 + 7] = Float.NaN;

        Double[] doubles = Arrays.stream(vector).map(Double::new).toArray(Double[]::new);

        // when
        String oldMethodResult = UNFUtil.calculateUNF(vector);
        String optimizedMethodResult = OptimizedUNFUtil.calculateUNF(doubles);

        // then
        assertThat(optimizedMethodResult)
                .withFailMessage("Failed for seed: " + seed)
                .isEqualTo(oldMethodResult);
    }

    @Test
    void compareTimes() throws IOException, UnfException {
        // given
        long seed = System.nanoTime();
        Random random = new Random(seed);
        String savedDateTimeFormat = "yyyy-MM-dd HH:mm:ss.SSS z";

        SimpleDateFormat formatter = new SimpleDateFormat(savedDateTimeFormat);
        String[] dateVector = random.longs(TEST_SIZE)
                .mapToObj(l -> new Date(Math.abs(l)))
                .map(formatter::format)
                .toArray(String[]::new);
        for (int i = 0; i < TEST_SIZE / 10; i++) {
            // this triggers the bug in Harvard implementation
            dateVector[Math.abs(random.nextInt()) % TEST_SIZE] = "2023-01-01 12:00:00 UTC";
        }

        // when
        DataFile dataFile = createDataFileWithVariable("time", savedDateTimeFormat);
        calculateUNF(dataFile, 0, dateVector);
        String oldMethodResult = dataFile.getDataTable().getDataVariables().get(0).getUnf();
        String optimizedMethodResult = OptimizedUNFUtil.calculateTimeUNF(dateVector, savedDateTimeFormat, true);

        // then
        assertThat(optimizedMethodResult)
                .withFailMessage("Failed for seed: " + seed)
                .isEqualTo(oldMethodResult);
    }

    @Test
    void compareDates() throws IOException, UnfException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // given
        long seed = System.nanoTime();
        Random random = new Random(seed);
        String savedDateTimeFormat = "yyyy-MM-dd";

        SimpleDateFormat formatter = new SimpleDateFormat(savedDateTimeFormat);
        String[] dateVector = random.longs(TEST_SIZE)
                .mapToObj(l -> new Date(Math.abs(l)))
                .map(formatter::format)
                .toArray(String[]::new);

        // when
        DataFile dataFile = createDataFileWithVariable("date", savedDateTimeFormat);
        calculateUNF(dataFile, 0, dateVector);
        String oldMethodResult = dataFile.getDataTable().getDataVariables().get(0).getUnf();
        String optimizedMethodResult = OptimizedUNFUtil.calculateDateUNF(dateVector, savedDateTimeFormat);

        // then
        assertThat(optimizedMethodResult)
                .withFailMessage("Failed for seed: " + seed)
                .isEqualTo(oldMethodResult);
    }


    // -------------------- PRIVATE --------------------

    private DataFile createDataFileWithVariable(String formatCategory, String format) {
        DataFile dataFile = new DataFile();
        DataTable dataTable = new DataTable();
        dataFile.setDataTable(dataTable);
        DataVariable dataVariable = new DataVariable();
        dataVariable.setFormatCategory(formatCategory);
        dataVariable.setFormat(format);
        dataTable.setDataVariables(new ArrayList<>());
        dataTable.getDataVariables().add(dataVariable);
        return dataFile;
    }

    // Old method from IngestServiceBean
    private void calculateUNF(DataFile dataFile, int varnum, String[] dataVector) throws IOException {
        Logger logger = Logger.getAnonymousLogger();
        String dateTimeFormat_ymdhmsS = "yyyy-MM-dd HH:mm:ss.SSS";
        String dateFormat_ymd = "yyyy-MM-dd";

        String unf = null;

        String[] dateFormats = null;

        // Special handling for Character strings that encode dates and times:

        if ("time".equals(dataFile.getDataTable().getDataVariables().get(varnum).getFormatCategory())) {
            dateFormats = new String[dataVector.length];
            String savedDateTimeFormat = dataFile.getDataTable().getDataVariables().get(varnum).getFormat();
            String timeFormat = StringUtils.isNotEmpty(savedDateTimeFormat)
                    ? savedDateTimeFormat : dateTimeFormat_ymdhmsS;

            /* What follows is special handling of a special case of time values
             * non-uniform precision; specifically, when some have if some have
             * milliseconds, and some don't. (and that in turn is only
             * n issue when the timezone is present... without the timezone
             * the time string would still evaluate to the end, even if the
             * format has the .SSS part and the string does not.
             * This case will be properly handled internally, once we permanently
             * switch to UNF6.
             * -- L.A. 4.0 beta 8
             */
            String simplifiedFormat = null;
            SimpleDateFormat fullFormatParser = null;
            SimpleDateFormat simplifiedFormatParser = null;

            if (timeFormat.matches(".*\\.SSS z$")) {
                simplifiedFormat = timeFormat.replace(".SSS", "");

                fullFormatParser = new SimpleDateFormat(timeFormat);
                simplifiedFormatParser = new SimpleDateFormat(simplifiedFormat);
            }

            for (int i = 0; i < dataVector.length; i++) {
                if (dataVector[i] == null) {
                    continue;
                }

                if (simplifiedFormatParser != null) {
                    // first, try to parse the value against the "full"
                    // format (with the milliseconds part):
                    fullFormatParser.setLenient(false);

                    try {
                        logger.fine("trying the \"full\" time format, with milliseconds: " + timeFormat + ", " + dataVector[i]);
                        fullFormatParser.parse(dataVector[i]);
                    } catch (ParseException ex) {
                        // try the simplified (no time zone) format instead:
                        logger.fine("trying the simplified format: " + simplifiedFormat + ", " + dataVector[i]);
                        simplifiedFormatParser.setLenient(false);
                        try {
                            simplifiedFormatParser.parse(dataVector[i]);
                            timeFormat = simplifiedFormat;
                        } catch (ParseException ex1) {
                            logger.warning("no parseable format found for time value " + i + " - " + dataVector[i]);
                            throw new IOException("no parseable format found for time value " + i + " - " + dataVector[i]);
                        }
                    }

                }
                dateFormats[i] = timeFormat;
            }
        } else if ("date".equals(dataFile.getDataTable().getDataVariables().get(varnum).getFormatCategory())) {
            dateFormats = new String[dataVector.length];
            String savedDateFormat = dataFile.getDataTable().getDataVariables().get(varnum).getFormat();
            for (int i = 0; i < dataVector.length; i++) {
                if (dataVector[i] != null) {
                    dateFormats[i] = StringUtils.isNotEmpty(savedDateFormat)
                            ? savedDateFormat : dateFormat_ymd;
                }
            }
        }

        try {
            if (dateFormats == null) {
                logger.fine("calculating the UNF value for string vector; first value: " + dataVector[0]);
                unf = OptimizedUNFUtil.calculateUNF(dataVector);
            } else {
                unf = UNFUtil.calculateUNF(dataVector, dateFormats);
            }
        } catch (IOException iex) {
            logger.warning("IO exception thrown when attempted to calculate UNF signature for (character) variable " + varnum);
        } catch (UnfException uex) {
            logger.warning("UNF Exception: thrown when attempted to calculate UNF signature for (character) variable " + varnum);
        }

        if (unf != null) {
            dataFile.getDataTable().getDataVariables().get(varnum).setUnf(unf);
        } else {
            logger.warning("failed to calculate UNF signature for variable " + varnum);
        }
    }
}