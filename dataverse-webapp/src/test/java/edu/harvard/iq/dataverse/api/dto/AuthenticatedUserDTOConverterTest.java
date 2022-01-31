package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserDTOConverterTest {

    @Test
    void convert() {
        // given
        AuthenticatedUser user = new AuthenticatedUser();
        user.setId(1L);
        user.setUserIdentifier("user-id");
        user.setFirstName("first-name");
        user.setLastName("last-name");
        user.setEmail("email@email.com");
        user.setSuperuser(false);
        user.setAffiliation("affiliation");
        user.setPosition("position");

        AuthenticatedUserLookup lookup
                = new AuthenticatedUserLookup("persistent-user-id", "authentication-provider-id", user);
        user.setAuthenticatedUserLookup(lookup);

        Timestamp timestamp = new Timestamp(0);
        user.setEmailConfirmed(timestamp);
        user.setCreatedTime(timestamp);
        user.setLastLoginTime(timestamp);
        user.setLastApiUseTime(timestamp);

        // when
        AuthenticatedUserDTO converted = new AuthenticatedUserDTO.Converter().convert(user);

        // then
        assertThat(converted)
                .extracting(AuthenticatedUserDTO::getId, AuthenticatedUserDTO::getIdentifier, AuthenticatedUserDTO::getDisplayName,
                        AuthenticatedUserDTO::getFirstName, AuthenticatedUserDTO::getLastName, AuthenticatedUserDTO::getEmail,
                        AuthenticatedUserDTO::getSuperuser, AuthenticatedUserDTO::getAffiliation, AuthenticatedUserDTO::getPosition,
                        AuthenticatedUserDTO::getPersistentUserId, AuthenticatedUserDTO::getAuthenticationProviderId)
                .containsExactly(1L, "@user-id", "first-name last-name",
                        "first-name", "last-name", "email@email.com",
                        false, "affiliation", "position",
                        "persistent-user-id", "authentication-provider-id");
        assertThat(converted)
                .extracting(AuthenticatedUserDTO::getEmailLastConfirmed, AuthenticatedUserDTO::getCreatedTime,
                AuthenticatedUserDTO::getLastLoginTime, AuthenticatedUserDTO::getLastApiUseTime)
                .containsExactly("1970-01-01T00:00:00Z", "1970-01-01T00:00:00Z",
                        "1970-01-01T00:00:00Z", "1970-01-01T00:00:00Z");
    }
}