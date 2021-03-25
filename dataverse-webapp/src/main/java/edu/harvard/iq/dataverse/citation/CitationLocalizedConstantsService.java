package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.common.BundleUtil;

import javax.ejb.Stateless;

/**
 * Currently this is used by RDS overlay only.
 */
@Stateless
public class CitationLocalizedConstantsService {
    public enum Constants {
        DATA("citation.constants.data"),
        PRODUCER("citation.constants.producer"),
        DISTRIBUTOR("citation.constants.distributor"),
        PUBLISHER("citation.constants.publisher");

        private final String key;

        public String getKey() {
            return key;
        }

        Constants(String key) {
            this.key = key;
        }
    }

    // -------------------- LOGIC --------------------

    public String get(Constants constant) {
        return "[" + BundleUtil.getStringFromBundle(constant.getKey()) + "]";
    }
}
