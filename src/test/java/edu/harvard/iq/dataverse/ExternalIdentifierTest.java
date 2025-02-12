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
      
      // Test ORCID with https://orcid.org/ prefix
      assertTrue(identifier.isValidIdentifier("https://orcid.org/0000-0002-1825-0097"));
      assertTrue(identifier.isValidIdentifier("https://orcid.org/0000-0002-1694-233X"));
      
      // Test format command
      assertEquals("https://orcid.org/0000-0002-1825-0097", identifier.format("0000-0002-1825-0097"));
      assertEquals("https://orcid.org/0000-0002-1694-233X", identifier.format("0000-0002-1694-233X"));
      assertEquals("https://orcid.org/0000-0002-1825-0097", identifier.format("https://orcid.org/0000-0002-1825-0097"));
  }

  @Test
  public void testIsValidAuthorIdentifierRor() {
      ExternalIdentifier identifier = ExternalIdentifier.valueOf("ROR");
      assertTrue(identifier.isValidIdentifier("01h6qyw18"));
      assertTrue(identifier.isValidIdentifier("02mhbdp49"));
      assertFalse(identifier.isValidIdentifier("01h6qyw1"));  // Too short
      assertFalse(identifier.isValidIdentifier("01h6qyw18a")); // Too long
      assertFalse(identifier.isValidIdentifier(" 01h6qyw18"));
      assertFalse(identifier.isValidIdentifier("01h6qyw18 "));
      assertFalse(identifier.isValidIdentifier("junk"));
      
      // Test ROR with https://ror.org/ prefix
      assertTrue(identifier.isValidIdentifier("https://ror.org/01h6qyw18"));
      assertTrue(identifier.isValidIdentifier("https://ror.org/02mhbdp49"));
      
      // Test format command
      assertEquals("https://ror.org/01h6qyw18", identifier.format("01h6qyw18"));
      assertEquals("https://ror.org/02mhbdp49", identifier.format("02mhbdp49"));
      assertEquals("https://ror.org/01h6qyw18", identifier.format("https://ror.org/01h6qyw18"));
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
