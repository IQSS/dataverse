package edu.harvard.iq.dataverse;

import java.util.regex.Pattern;

public class LinkTemplate {
  private String template;
  private Pattern pattern;

  public LinkTemplate(String template, String regex) {
    this.template = template;
    this.pattern = Pattern.compile(regex);
  }

  public String getTemplate() {
    return template;
  }

  public Pattern getPattern() {
    return pattern;
  }
}
