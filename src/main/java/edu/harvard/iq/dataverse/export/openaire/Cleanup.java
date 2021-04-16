package edu.harvard.iq.dataverse.export.openaire;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author francesco.cadili@4science.it
 */
public class Cleanup {
    
    /**
     * Normalize sentence
     *
     * @param sentence full name or organization name
     * @return normalize string value
     */
    static public String normalize(String sentence) {
        if (StringUtils.isBlank(sentence)) {
            return "";
        }

        sentence = sentence.trim()
                .replaceAll(", *", ", ")
                .replaceAll(" +", " ");

        return sentence;
    }
}
