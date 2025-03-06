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
      assertTrue(identifier.isValidIdentifier("000000012150090X"));
      assertTrue(identifier.isValidIdentifier("http://www.isni.org/isni/0000000121032683"));
      assertTrue(identifier.isValidIdentifier("http://www.isni.org/isni/000000012150090X"));
      assertFalse(identifier.isValidIdentifier("junk"));
      assertFalse(identifier.isValidIdentifier("000000012103268")); // Too short
      assertFalse(identifier.isValidIdentifier("00000001210326831")); // Too long
  
      // Test format command
      assertEquals("http://www.isni.org/isni/0000000121032683", identifier.format("0000000121032683"));
      assertEquals("http://www.isni.org/isni/0000000121032683", identifier.format("http://www.isni.org/isni/0000000121032683"));
  }
  
  @Test
  public void testIsValidAuthorIdentifierLcna() {
      ExternalIdentifier identifier = ExternalIdentifier.valueOf("LCNA");
      assertTrue(identifier.isValidIdentifier("n82058243"));
      assertTrue(identifier.isValidIdentifier("foobar123"));
      assertTrue(identifier.isValidIdentifier("http://id.loc.gov/authorities/names/n82058243"));
      assertFalse(identifier.isValidIdentifier("junk"));
      assertFalse(identifier.isValidIdentifier("123")); // Too short (assuming minimum length)
  
      // Test format command
      assertEquals("http://id.loc.gov/authorities/names/n82058243", identifier.format("n82058243"));
      assertEquals("http://id.loc.gov/authorities/names/n82058243", identifier.format("http://id.loc.gov/authorities/names/n82058243"));
  }
  
  @Test
  public void testIsValidAuthorIdentifierViaf() {
      ExternalIdentifier identifier = ExternalIdentifier.valueOf("VIAF");
      assertTrue(identifier.isValidIdentifier("172389567"));
      assertTrue(identifier.isValidIdentifier("https://viaf.org/viaf/172389567"));
      assertFalse(identifier.isValidIdentifier("junk"));
      
      assertEquals("https://viaf.org/viaf/172389567", identifier.format("172389567"));
      assertEquals("https://viaf.org/viaf/172389567", identifier.format("https://viaf.org/viaf/172389567"));

  }
  
  @Test
  public void testIsValidAuthorIdentifierGnd() {
      ExternalIdentifier identifier = ExternalIdentifier.valueOf("GND");
      assertTrue(identifier.isValidIdentifier("4079154-3"));
      assertTrue(identifier.isValidIdentifier("118540238"));
      assertTrue(identifier.isValidIdentifier("https://d-nb.info/gnd/4079154-3"));
      assertTrue(identifier.isValidIdentifier("https://d-nb.info/gnd/118540238"));
      assertFalse(identifier.isValidIdentifier("junk"));
      assertFalse(identifier.isValidIdentifier("123")); // Too short
      
      assertEquals("https://d-nb.info/gnd/4079154-3", identifier.format("4079154-3"));
      assertEquals("https://d-nb.info/gnd/4079154-3", identifier.format("https://d-nb.info/gnd/4079154-3"));

  }
  
  @Test
  public void testIsValidAuthorIdentifierResearcherId() {
      ExternalIdentifier identifier = ExternalIdentifier.valueOf("ResearcherID");
      assertTrue(identifier.isValidIdentifier("A-1234-5678"));
      assertTrue(identifier.isValidIdentifier("J-9876-2018"));
      assertTrue(identifier.isValidIdentifier("AAA-1234-2020"));
      assertTrue(identifier.isValidIdentifier("Z9999-2021"));
      assertTrue(identifier.isValidIdentifier("https://publons.com/researcher/A-1234-5678/"));
      assertTrue(identifier.isValidIdentifier("https://publons.com/researcher/J-9876-2018/"));
      assertFalse(identifier.isValidIdentifier("a-1234-5678")); // Lowercase start
      assertFalse(identifier.isValidIdentifier("A-1234-5678-")); // Ends with hyphen
      assertFalse(identifier.isValidIdentifier("-A-1234-5678")); // Starts with hyphen
      assertFalse(identifier.isValidIdentifier("junk"));
      
      assertEquals("https://publons.com/researcher/A-1234-5678/", identifier.format("A-1234-5678"));
      assertEquals("https://publons.com/researcher/A-1234-5678/", identifier.format("https://publons.com/researcher/A-1234-5678/"));

  }
  
  @Test
  public void testIsValidAuthorIdentifierScopusId() {
      ExternalIdentifier identifier = ExternalIdentifier.valueOf("ScopusID");
      assertTrue(identifier.isValidIdentifier("12345678"));
      assertTrue(identifier.isValidIdentifier("87654321"));
      assertTrue(identifier.isValidIdentifier("00000000"));
      assertTrue(identifier.isValidIdentifier("https://www.scopus.com/authid/detail.uri?authorId=12345678"));
      assertTrue(identifier.isValidIdentifier("https://www.scopus.com/authid/detail.uri?authorId=87654321"));
      assertFalse(identifier.isValidIdentifier("A12345678")); // Contains a letter
      assertFalse(identifier.isValidIdentifier("junk"));
      
      assertEquals("https://www.scopus.com/authid/detail.uri?authorId=12345678", identifier.format("12345678"));
      assertEquals("https://www.scopus.com/authid/detail.uri?authorId=12345678", identifier.format("https://www.scopus.com/authid/detail.uri?authorId=12345678"));

  }
  
  @Test
  public void testIsValidAuthorIdentifierDai() {
      ExternalIdentifier identifier = ExternalIdentifier.valueOf("DAI");
      assertTrue(identifier.isValidIdentifier("123456789"));
      assertTrue(identifier.isValidIdentifier("987654321X"));
      assertTrue(identifier.isValidIdentifier("info:eu-repo/dai/nl/123456789"));
      assertTrue(identifier.isValidIdentifier("info:eu-repo/dai/nl/987654321X"));
      assertFalse(identifier.isValidIdentifier("12345678")); // Too short
      assertFalse(identifier.isValidIdentifier("12345678901")); // Too long
      assertFalse(identifier.isValidIdentifier("A23456789")); // Contains a letter
      assertFalse(identifier.isValidIdentifier("junk"));
      
      assertEquals("info:eu-repo/dai/nl/123456789", identifier.format("123456789"));
      assertEquals("info:eu-repo/dai/nl/123456789", identifier.format("info:eu-repo/dai/nl/123456789"));
  }

}
