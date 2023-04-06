package edu.harvard.iq.dataverse.api.dto;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class FileLabelsChangeOptionsDTO {
    /**
     * Ids of files that should undergo the label change even if they're not
     * matching the pattern
     */
    private List<Long> filesToIncludeIds = new ArrayList<>();
    /** Ids of files that should be excluded from label change */
    private List<Long> filesToExcludeIds = new ArrayList<>();

    /**
     * Pattern used to find the files with matching labels. If the wildcard is
     * used, the pattern is matched against the whole label, otherwise it's
     * only checked whether the pattern is contained within the label.
     */
    private String pattern = "*";

    /**
     * Java-type regex for describing the fragment of label that should
     * be replaced
     */
    private String from = StringUtils.EMPTY;

    /** Replacement for fragments matching 'from' pattern */
    private String to = StringUtils.EMPTY;

    /** When set to true result of the replace won't be saved */
    private boolean preview = false;

    // -------------------- GETTERS --------------------

    public List<Long> getFilesToIncludeIds() {
        return filesToIncludeIds;
    }

    public List<Long> getFilesToExcludeIds() {
        return filesToExcludeIds;
    }

    public String getPattern() {
        return pattern;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isPreview() {
        return preview;
    }

    // -------------------- SETTERS --------------------

    public void setFilesToIncludeIds(List<Long> filesToIncludeIds) {
        this.filesToIncludeIds = filesToIncludeIds;
    }

    public void setFilesToExcludeIds(List<Long> filesToExcludeIds) {
        this.filesToExcludeIds = filesToExcludeIds;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }
}
