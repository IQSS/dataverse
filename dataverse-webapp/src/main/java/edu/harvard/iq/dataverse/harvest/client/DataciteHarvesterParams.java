package edu.harvard.iq.dataverse.harvest.client;

import io.vavr.control.Option;

import java.util.Collections;
import java.util.List;

/**
 * Parameters used by the datacite DOI harvester.
 */
public class DataciteHarvesterParams extends HarvesterParams {

    private final static String DOI_PART_SEPARATOR = "/";

    private List<DOIValue> doiImport;

    private List<DOIValue> doiRemove;

    // -------------------- GETTERS --------------------

    public List<DOIValue> getDoiImport() {
        return Option.of(doiImport).getOrElse(Collections.emptyList());
    }

    public List<DOIValue> getDoiRemove() {
        return Option.of(doiRemove).getOrElse(Collections.emptyList());
    }

    // -------------------- SETTERS --------------------

    public void setDoiImport(List<DOIValue> doiImport) {
        this.doiImport = doiImport;
    }

    public void setDoiRemove(List<DOIValue> doiRemove) {
        this.doiRemove = doiRemove;
    }

    // -------------------- INNER CLASSES --------------------

    static public class DOIValue {
        private final String authority;
        private final String id;

        // -------------------- CONSTRUCTORS --------------------

        public DOIValue(String authority, String id) {
            this.authority = authority;
            this.id = id;
        }

        public DOIValue(String fullDoi) {
            String[] doiParts = fullDoi.split(DOI_PART_SEPARATOR);
            if (doiParts.length != 2) {
                throw new IllegalArgumentException("Invalid DOI: " + fullDoi);
            }
            this.authority = doiParts[0];
            this.id = doiParts[1];
        }

        // -------------------- GETTERS --------------------

        public String getAuthority() {
            return authority;
        }

        public String getId() {
            return id;
        }

        public String getFull() {
            return authority + DOI_PART_SEPARATOR + id;
        }
    }
}
