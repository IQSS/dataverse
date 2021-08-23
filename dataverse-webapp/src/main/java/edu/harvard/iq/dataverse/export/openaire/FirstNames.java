package edu.harvard.iq.dataverse.export.openaire;

import edu.harvard.iq.dataverse.export.ExportUtil;
import io.vavr.Tuple2;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author francesco.cadili@4science.it
 */
public class FirstNames {

    private static FirstNames instance = null;

    private final int capacity = (int) ((49000 + 33000) * 1.2 * 0.8);
    private final boolean loadNameDict = true;
    private final boolean loadYobDict = true;
    private final boolean loadHint = true;
    private final int nameDictStart = 2;
    private final int nameDictLength = 27;
    private int duplicates = 0;     // 20%, factor 0.8

    private HashMap<String, String> map = new HashMap<String, String>(capacity);
    private static final Logger logger = Logger.getLogger(FirstNames.class.getCanonicalName());
    public static String NAME_DICT_FILENAME = "edu/harvard/iq/dataverse/firstNames/nam_dict.txt";
    public static String YOB_FILENAME = "edu/harvard/iq/dataverse/firstNames/yob2017.txt";

    public static HashMap<String, String> hints = new HashMap<String, String>();
    public static String HINT_FILENAME = "edu/harvard/iq/dataverse/firstNames/hint.txt";

    /**
     * Singleton method
     *
     * @return The FirstNames object
     */
    public static synchronized FirstNames getInstance() {
        if (instance == null) {
            instance = new FirstNames();
        }

        return instance;
    }

    /**
     * Initialize hash map.
     *
     */
    private FirstNames() {
        if (loadNameDict) {
            try {
                readNameDict();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "I cannot read {0} file", NAME_DICT_FILENAME);
            }
        }
        if (loadYobDict) {
            try {
                readYob();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "I cannot read {0} file", YOB_FILENAME);
            }
        }
        if (loadHint) {
            try {
                readHint();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "I cannot read {0} file", HINT_FILENAME);
            }
        }
    }

    /**
     * Check if firstName exists on map.
     *
     * @param firstName
     * @return
     */
    public boolean isFirstName(String firstName) {
        return (map.containsKey(firstName.toLowerCase()));
    }

    /**
     * extracts first name and given name from full name
     * @param fullname text potentially representing full name of a person
     * @return Pair of text values, 1st element stores first names, 2nd element stores last names;
     *          values separated by space (" "). Returns null if extraction fails.
     */
    public Tuple2<String, String> extractFirstAndLastName(String fullname) {
        int commaCount = StringUtils.countMatches(fullname, ",");
        if (commaCount == 0) {
            String[] words = fullname.split("\\s+");
            int firstNameWordsCount = 0;

            for (int i=0; i<words.length; ++i) {
                String word = words[i];
                boolean isLastWord = (i == (words.length - 1));
                boolean isFirstWord = (i == 0);

                if (isWordPartOfFirstName(word, isFirstWord, isLastWord)) {
                    firstNameWordsCount = i+1;
                } else {
                    break;
                }

            }

            return new Tuple2<String, String>(
                    StringUtils.join(words, ' ', 0, firstNameWordsCount),
                    StringUtils.join(words, ' ', firstNameWordsCount, words.length));

        } else if (commaCount == 1 && !fullname.endsWith(",")) {
            String[] words = fullname.split(",");
            return new Tuple2<>(words[1].trim(), words[0].trim());
        }
        return null;
    }

    /**
     * Used to optimize capacity.
     *
     * @return
     */
    int getDuplicates() {
        return duplicates;
    }

    /**
     * *
     * Check if a word is a beginning of a last name
     *
     * @param word
     * @return true/false
     */
    private boolean isStartOfLastName(String word) {
        if (hints.containsKey(word.toLowerCase())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Read name_dict.txt file. (GNU Free Documentation License)
     *
     * @see
     * <a href="https://github.com/lead-ratings/gender-guesser/blob/master/gender_guesser/data/nam_dict.txt">
     * List of first names and gender</a>
     */
    private void readNameDict() throws IOException {
        InputStream fis = FirstNames.class.getClassLoader().getResourceAsStream(NAME_DICT_FILENAME);

        if (fis != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String s;
            StringBuilder name = new StringBuilder(nameDictLength);
            int pos = 0;
            while ((s = br.readLine()) != null) {
                if ('#' != s.charAt(0)) {
                    name.setLength(0);
                    name.append(s.substring(nameDictStart, nameDictStart + nameDictLength).trim());
                    while ((pos = name.indexOf("+")) >= 0) {
                        name.replace(pos, pos + 1, " ");
                    }
                    addName(name);
                }
            }
            br.close();
        }
    }

    /**
     *
     * Read yob2017.txt file. (Creative Commons CCZero)
     *
     * @see
     * <a href="https://catalog.data.gov/dataset/baby-names-from-social-security-card-applications-national-level-data">
     * Baby Names from Social Security Card Applications - National Level
     * Data</a>
     */
    private void readYob() throws IOException {
        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(YOB_FILENAME);

        if (fis != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String s;
            StringBuilder name = new StringBuilder(100);
            while ((s = br.readLine()) != null) {
                if ('#' != s.charAt(0)) {
                    int end = s.indexOf(",");

                    name.setLength(0);
                    name.ensureCapacity(end);
                    name.append(s.substring(0, end));
                    addName(name);
                }
            }
            br.close();
        }
    }

    private void addName(StringBuilder name) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Name: {0}", name);
        }

        String normalizedName = ExportUtil.normalizeAccents(name.toString());
        String old = map.put(normalizedName.toLowerCase(), normalizedName);
        if (old != null) {
            duplicates++;
        }
    }

    /**
     * Read hint.txt file. *
     */
    private void readHint() throws IOException {
        InputStream fis = FirstNames.class.getClassLoader().getResourceAsStream(HINT_FILENAME);

        if (fis != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String hint;
            while ((hint = br.readLine()) != null) {
                if ('#' != hint.charAt(0)) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "Hint: {0}", hint);
                    }

                    hints.put(hint.toLowerCase(), hint);
                }
            }
            br.close();
        }
    }

    private boolean isWordPartOfFirstName(String word, boolean isFirstWord, boolean isLastWord) {
        boolean wordIsAFirstName = isFirstName(word);

        if (wordIsAFirstName) {
            if (isFirstWord && isLastWord) {
                return true;
            }
            return !isLastWord && !isStartOfLastName(word);
        }
        return false;
    }
}