package edu.harvard.iq.dataverse.harvest.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.vavr.control.Option;

import java.util.Collections;
import java.util.List;

/**
 * Parameters used by the datacite DOI harvester.
 */
public class DataciteHarvesterParams extends HarvesterParams {

    private final static String DOI_PART_SEPARATOR = "/";

    private List<DOIValue> doiImport;

    // -------------------- GETTERS --------------------

    public List<DOIValue> getDoiImport() {
        return Option.of(doiImport).getOrElse(Collections.emptyList());
    }

    // -------------------- SETTERS --------------------

    public void setDoiImport(List<DOIValue> doiImport) {
        this.doiImport = doiImport;
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
