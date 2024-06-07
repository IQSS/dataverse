package edu.harvard.iq.dataverse.userdata;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthenticatedUserCsvWriterTest {

    private AuthenticatedUserCsvWriter csvWriter = new AuthenticatedUserCsvWriter();

    // -------------------- TESTS --------------------

    @Test
    public void write() throws IOException {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<AuthenticatedUser> users = Lists.newArrayList(
                defaultUser(1, "jo", "John", "Doe"),
                defaultUser(2, "ma", "Mary", "Sue",
                        u -> u.setLastLoginTime(null), u -> u.setLastApiUseTime(null)),
                defaultUser(3, "df", "Diana", "Flores",
                        u -> u.setSuperuser(true), u -> u.setNotificationsLanguage(Locale.FRENCH), u -> u.setAffiliation(null)),
                defaultUser(4, "ba", "Barb", "Wire",
                        u -> u.setEmailConfirmed(null), u -> u.setAffiliation(null), u -> u.getAuthenticatedUserLookup().setAuthenticationProviderId("saml")),
                defaultUser(5, "", "Mr.", "special\",\"char.txt",
                        u -> u.setCreatedTime(null), u -> u.setLastApiUseTime(null), u -> u.setLastLoginTime(null), u -> u.setEmail(""),
                        u -> u.setAffiliation(null), u -> u.setEmailConfirmed(null), u -> u.setNotificationsLanguage(null))
        );

        // when
        csvWriter.write(outputStream, users);

        // then
        String[] csv = outputStream.toString().split("\r\n");
        assertEquals(6, csv.length);
        assertEquals("ID,Username,Name,Email,Affiliation,Superuser,Authentication,Verification status,Notification language,Account creation,Last login,Last API use", csv[0]);
        assertEquals("1,@jo,John Doe,John.Doe@someU.edu,UnitTester,false,buildIn,Verified,en,2024-05-28T01:01:01Z,2024-05-28T02:02:02Z,2024-05-28T03:03:03Z", csv[1]);
        assertEquals("2,@ma,Mary Sue,Mary.Sue@someU.edu,UnitTester,false,buildIn,Verified,en,2024-05-28T01:01:01Z,,", csv[2]);
        assertEquals("3,@df,Diana Flores,Diana.Flores@someU.edu,,true,buildIn,Verified,fr,2024-05-28T01:01:01Z,2024-05-28T02:02:02Z,2024-05-28T03:03:03Z", csv[3]);
        assertEquals("4,@ba,Barb Wire,Barb.Wire@someU.edu,,false,saml,Not verified,en,2024-05-28T01:01:01Z,2024-05-28T02:02:02Z,2024-05-28T03:03:03Z", csv[4]);
        assertEquals("5,@,\"Mr. special\"\",\"\"char.txt\",,,false,buildIn,Not verified,,,,", csv[5]);
    }

    @Test
    public void write__empty_list() throws IOException {
        // when
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        csvWriter.write(outputStream, Collections.emptyList());

        // then
        String[] csv = outputStream.toString().split("\r\n");
        assertEquals(1, csv.length);
        assertEquals("ID,Username,Name,Email,Affiliation,Superuser,Authentication,Verification status,Notification language,Account creation,Last login,Last API use", csv[0]);
    }

    // -------------------- PRIVATE --------------------

    @SafeVarargs
    private static AuthenticatedUser defaultUser(int id, String username, String firstName, String lastname, Consumer<AuthenticatedUser>... overrides) {
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser(firstName, lastname);
        user.setId((long) id);
        user.setUserIdentifier(username);
        user.setAuthenticatedUserLookup(new AuthenticatedUserLookup());
        user.getAuthenticatedUserLookup().setAuthenticationProviderId("buildIn");
        user.getAuthenticatedUserLookup().setPersistentUserId(String.valueOf(id));
        user.setCreatedTime(Timestamp.valueOf("2024-05-28 01:01:01"));
        user.setLastLoginTime(Timestamp.valueOf("2024-05-28 02:02:02"));
        user.setLastApiUseTime(Timestamp.valueOf("2024-05-28 03:03:03"));
        user.setEmailConfirmed(Timestamp.valueOf("2024-05-28 04:04:04"));

        Option.of(overrides).forEach(o -> Arrays.stream(o).forEach(override -> override.accept(user)));

        return user;
    }
}
