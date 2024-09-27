package edu.harvard.iq.dataverse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ExternalIdentifier {
    ORCID("ORCID", "https://orcid.org/%s", "^\\d{4}-\\d{4}-\\d{4}-(\\d{4}|\\d{3}X)$"),
    ISNI("ISNI", "http://www.isni.org/isni/%s", "^\\d*$"),
    LCNA("LCNA", "http://id.loc.gov/authorities/names/%s", "^[a-z]+\\d+$"),
    VIAF("VIAF", "https://viaf.org/viaf/%s", "^\\d*$"),
    // GND regex from https://www.wikidata.org/wiki/Property:P227
    GND("GND", "https://d-nb.info/gnd/%s", "^1[01]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X]$"),
    // note: DAI is missing from this list, because it doesn't have resolvable URL
    ResearcherID("ResearcherID", "https://publons.com/researcher/%s/", "^[A-Z\\d][A-Z\\d-]+[A-Z\\d]$"),
    ScopusID("ScopusID", "https://www.scopus.com/authid/detail.uri?authorId=%s", "^\\d*$"),
    //Requiring ROR to be URL form as we use it where there is no id type field and matching any 9 digit number starting with 0 seems a bit aggressive
    ROR("ROR", "https://ror.org/%s", "^(https:\\/\\/ror.org\\/)0[a-hj-km-np-tv-z|0-9]{6}[0-9]{2}$");

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
        return String.format(template, idValue);
    }
}
