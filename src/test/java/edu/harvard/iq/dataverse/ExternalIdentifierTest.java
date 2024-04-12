package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExternalIdentifierTest {

  @Test
  public void testIsValidAuthorIdentifierOrcid() {
    ExternalIdentifier identifier = ExternalIdentifier.valueOf("ORCID");
    assertTrue(identifier.isValidIdentifier("0000-0002-1825-0097"));
    // An "X" at the end of an ORCID is less common but still valid.
    assertTrue(identifier.isValidIdentifier("0000-0002-1694-233X"));
    assertFalse(identifier.isValidIdentifier("0000 0002 1825 0097"));
    assertFalse(identifier.isValidIdentifier(" 0000-0002-1825-0097"));
    assertFalse(identifier.isValidIdentifier("0000-0002-1825-0097 "));
    assertFalse(identifier.isValidIdentifier("junk"));
  }

  @Test
  public void testIsValidAuthorIdentifierIsni() {
    ExternalIdentifier identifier = ExternalIdentifier.valueOf("ISNI");
    assertTrue(identifier.isValidIdentifier("0000000121032683"));
    assertFalse(identifier.isValidIdentifier("junk"));
  }

  @Test
  public void testIsValidAuthorIdentifierLcna() {
    ExternalIdentifier identifier = ExternalIdentifier.valueOf("LCNA");
    assertTrue(identifier.isValidIdentifier("n82058243"));
    assertTrue(identifier.isValidIdentifier("foobar123"));
    assertFalse(identifier.isValidIdentifier("junk"));
  }

  @Test
  public void testIsValidAuthorIdentifierViaf() {
    ExternalIdentifier identifier = ExternalIdentifier.valueOf("VIAF");
    assertTrue(identifier.isValidIdentifier("172389567"));
    assertFalse(identifier.isValidIdentifier("junk"));
  }

  @Test
  public void testIsValidAuthorIdentifierGnd() {
    ExternalIdentifier identifier = ExternalIdentifier.valueOf("GND");
    assertTrue(identifier.isValidIdentifier("4079154-3"));
    assertFalse(identifier.isValidIdentifier("junk"));
  }

}
