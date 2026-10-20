package edu.harvard.iq.dataverse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.harvard.iq.dataverse.settings.FeatureFlags;

public enum ExternalIdentifier {
    ORCID("ORCID", "https://orcid.org/%s", "^(https:\\/\\/(sandbox\\.)?orcid\\.org\\/)?\\d{4}-\\d{4}-\\d{4}-(\\d{4}|\\d{3}X)$"),
    ISNI("ISNI", "http://www.isni.org/isni/%s", "^(http:\\/\\/www\\.isni\\.org\\/isni\\/)?(\\d{16}|\\d{15}X)$"),
    LCNA("LCNA", "http://id.loc.gov/authorities/names/%s", "^(http:\\/\\/id\\.loc\\.gov\\/authorities\\/names\\/)?[a-z]+\\d+$"),
    VIAF("VIAF", "https://viaf.org/viaf/%s", "^(https:\\/\\/viaf\\.org\\/viaf\\/)?\\d*$"),
    // GND regex from https://www.wikidata.org/wiki/Property:P227
    GND("GND", "https://d-nb.info/gnd/%s", "^(https:\\/\\/d-nb\\.info\\/gnd\\/)?(1[01]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X])$"),
    // note: DAI is missing from this list, because it doesn't have resolvable URL
    ResearcherID("ResearcherID", "https://publons.com/researcher/%s/", "^([A-Z\\d][A-Z\\d-]+[A-Z\\d]|(https:\\/\\/publons\\.com\\/researcher\\/)?[A-Z\\d][A-Z\\d-]+[A-Z\\d]\\/)$"),
    ScopusID("ScopusID", "https://www.scopus.com/authid/detail.uri?authorId=%s", "^(https:\\/\\/www\\.scopus\\.com\\/authid\\/detail\\.uri\\?authorId=)?\\d*$"),
    // ROR regex from https://ror.readme.io/docs/identifier
    ROR("ROR", "https://ror.org/%s", "^(https:\\/\\/ror\\.org\\/)?0[a-hj-km-np-tv-z|0-9]{6}[0-9]{2}$"),
    DAI("DAI", "info:eu-repo/dai/nl/%s", "^(info:eu-repo\\/dai\\/nl\\/)?[\\d]?\\d{8}[0-9X]$");

    private static boolean treatSandboxOrcidsAsPids =
            FeatureFlags.TREAT_SANDBOX_ORCIDS_AS_PIDS.enabled();

    private String name;
    private String template;
    private Pattern pattern;
    private Matcher matcher;

    ExternalIdentifier(String name, String template, String regex) {
      this.template = template;
      this.pattern = Pattern.compile(regex);
      this.matcher = pattern.matcher("");
    }

    public ExternalIdentifier of(String name) {
        System.err.println(name);
        for (ExternalIdentifier identifier : values()) {
            System.err.println(" vs " + identifier.name);
            if (identifier.name.toLowerCase().equals(name.toLowerCase())) {
              return identifier;
            }
        }
        return null;
    }

    public boolean isValidIdentifier(String userInput) {
        if (this == ORCID && !treatSandboxOrcidsAsPids
                && userInput != null && userInput.startsWith("https://sandbox.orcid.org/")) {
            return false;
        }
        return matcher.reset(userInput).matches();
    }

    public String getName() {
      return name;
    }

    public String getTemplate() {
      return template;
    }

    public Pattern getPattern() {
      return pattern;
    }

    public String format(String idValue) {
        if (this == ORCID && idValue.startsWith("https://sandbox.orcid.org/")) {
            return idValue;
        }
        if(idValue.startsWith(template.substring(0,template.indexOf("%s")))) {
            return idValue; 
        }
        if (this == ORCID && treatSandboxOrcidsAsPids) {
            return String.format("https://sandbox.orcid.org/%s", idValue);
        }
        return String.format(template, idValue);
    }

    // Allows tests to change the flag
    static void setTreatSandboxOrcidsAsPids(boolean treatSandboxOrcidsAsPids) {
        ExternalIdentifier.treatSandboxOrcidsAsPids = treatSandboxOrcidsAsPids;
    }
}
