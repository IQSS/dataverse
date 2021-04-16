package edu.harvard.iq.dataverse.authorization.providers.shib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class ShibUtilTest {

    @RunWith(Parameterized.class)
    public static class ShibUtilParamTest {

        @Parameters
        public static Collection<String[]> data() {
            return Arrays.asList(new String[][] {
                { "John", "Harvard", "John", "Harvard", null },
                { "Guido", "van Rossum", "Guido", "van Rossum", null },
                { "Philip Seymour", "Hoffman", "Philip Seymour", "Hoffman", "Philip Seymour Hoffman" },
                { "Edward", "Cummings", "Edward;e e", "Cummings", null },
                { "Edward", "Cummings", "Edward;e e", "Cummings", "e e cummings" },
                { "Anthony", "Stark", "Tony;Anthony", "Stark", null },
                { "Anthony", "Stark", "Anthony;Tony", "Stark", null },
                { "Antoni", "Gaudí", "Antoni", "Gaudí i Cornet;Gaudí", null },
                { "Jane", "Doe", null, null, "Jane Doe" },
                /**
                * @todo Make findBestFirstAndLastName smart enough to know that the last name
                *       should be "Hoffman" rather than "Seymour".
                */
                { "Philip", "Seymour", null, null, "Philip Seymour Hoffman" },
                { null, null, null, null, "" }
            });
        }

        @Parameter
        public String expectedFirstName;

        @Parameter(1)
        public String expectedLastName;

        @Parameter(2)
        public String actualFirstName;

        @Parameter(3)
        public String actualLastName;

        @Parameter(4)
        public String actualDisplayName;

        @Test
        public void testFindBestFirstAndLastName() {

            // ShibUserNameFields expected1 = new ShibUserNameFields("John", "Harvard");
            ShibUserNameFields actualValues = ShibUtil.findBestFirstAndLastName(actualFirstName, actualLastName, actualDisplayName);
            assertEquals(expectedFirstName, actualValues.getFirstName());
            assertEquals(expectedLastName, actualValues.getLastName());
        }
    }

    public static class ShibUtilNoParamTest {

        private HttpServletRequest request = mock(HttpServletRequest.class);

        /**
         * Test of getDisplayNameFromDiscoFeed method, of class ShibUtil.
         */
        @Test
        public void testGetDisplayNameFromDiscoFeed() {
            // System.out.println("getDisplayNameFromDiscoFeed");

            String discoFeedExample = null;
            try {
                discoFeedExample = new String(Files.readAllBytes(Paths.get("src/main/webapp/resources/dev/sample-shib-identities.json")));
            } catch (IOException ex) {
                Logger.getLogger(ShibUtilTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            ShibUtil shibUtil = new ShibUtil();

            String testShib = shibUtil.getDisplayNameFromDiscoFeed("https://idp.testshib.org/idp/shibboleth", discoFeedExample);
            assertEquals("TestShib Test IdP", testShib);

            String harvardStage = shibUtil.getDisplayNameFromDiscoFeed("https://stage.fed.huit.harvard.edu/idp/shibboleth", discoFeedExample);
            assertEquals("Harvard Test IdP", harvardStage);

            String minimal = shibUtil.getDisplayNameFromDiscoFeed("https://minimal.com/shibboleth", discoFeedExample);
            assertEquals(null, minimal);

            String unknown = shibUtil.getDisplayNameFromDiscoFeed("https://nosuchdomain.com/idp/shibboleth", discoFeedExample);
            assertEquals(null, unknown);

            String searchForNull = shibUtil.getDisplayNameFromDiscoFeed(null, discoFeedExample);
            assertEquals(null, searchForNull);

            /**
             * @todo Is it an error to pass a null discoFeed?
             */
            // String nullDiscoFeed =
            // shibUtil.getDisplayNameFromDiscoFeed("https://idp.testshib.org/idp/shibboleth",
            // null);
            // assertEquals(null, nullDiscoFeed);
            /**
             * @todo Is it an error to pass junk (non-JSON) as a discoFeed?
             */
            // String unparseAbleDiscoFeed =
            // shibUtil.getDisplayNameFromDiscoFeed("https://idp.testshib.org/idp/shibboleth",
            // "unparseAbleAsJson");
            // assertEquals(null, unparseAbleDiscoFeed);
        }

        @Test
        public void testFindSingleValue() {
            assertEquals(null, ShibUtil.findSingleValue(null));
            assertEquals("foo", ShibUtil.findSingleValue("foo"));
            assertEquals("bar", ShibUtil.findSingleValue("foo;bar"));
        }

        @Test
        public void testGenerateFriendlyLookingUserIdentifer() {
            int lengthOfUuid = UUID.randomUUID().toString().length();
            assertEquals("uid1", ShibUtil.generateFriendlyLookingUserIdentifer("uid1", null));
            assertEquals(" leadingWhiteSpace", ShibUtil.generateFriendlyLookingUserIdentifer(" leadingWhiteSpace", null));
            assertEquals("uid1", ShibUtil.generateFriendlyLookingUserIdentifer("uid1", "email1@example.com"));
            assertEquals("email1", ShibUtil.generateFriendlyLookingUserIdentifer(null, "email1@example.com"));
            assertEquals(lengthOfUuid, ShibUtil.generateFriendlyLookingUserIdentifer(null, null).length());
            assertEquals(lengthOfUuid, ShibUtil.generateFriendlyLookingUserIdentifer(null, "").length());
            assertEquals(lengthOfUuid, ShibUtil.generateFriendlyLookingUserIdentifer("", null).length());
            assertEquals(lengthOfUuid, ShibUtil.generateFriendlyLookingUserIdentifer(null, "junkEmailAddress").length());
        }

        @Test
        public void testDevMutations() {
            ShibUtil.mutateRequestForDevConstantHarvard1(request);
            ShibUtil.mutateRequestForDevConstantHarvard2(request);
            ShibUtil.mutateRequestForDevConstantInvalidEmail(request);
            ShibUtil.mutateRequestForDevConstantMissingRequiredAttributes(request);
            ShibUtil.mutateRequestForDevConstantTestShib1(request);
            ShibUtil.mutateRequestForDevConstantTwoEmails(request);
            ShibUtil.printAttributes(request);
            ShibUtil.printAttributes(null);
        }

        @Test
        public void testGetRandomUserStatic() {
            Map<String, String> randomUser = ShibUtil.getRandomUserStatic();
            assertEquals(8, randomUser.get("firstName").length());
        }
    }

}
