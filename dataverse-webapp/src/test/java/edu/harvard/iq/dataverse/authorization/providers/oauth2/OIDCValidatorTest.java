package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OIDCValidatorTest {

    private OIDCValidator validator;

    private JWT token = Mockito.mock(JWT.class);

    @BeforeEach
    void setUp() {
        validator = new OIDCValidator();
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Validation of ID token should fail if inner validator is uninitialized")
    void validateIDToken__uninitializedValidator() throws OAuth2Exception {

        // when & then
        assertThatThrownBy(() -> validator.validateIDToken(token))
                .isInstanceOf(OAuth2Exception.class);
    }

    @Test
    @DisplayName("Validation of ID token should use internal validator's validation method")
    void validateIDToken() throws Exception {

        // given
        IDTokenValidator internalValidator = setAndGetInternalMockValidator();

        // when
        validator.validateIDToken(token);

        // then
        Mockito.verify(internalValidator, Mockito.only()).validate(token, null);
    }

    @Test
    @DisplayName("Validation of UserInfo should not fail when subjects from ID token and user info endpoint are equal")
    void validateUserInfoSubject() throws OAuth2Exception {

        // given
        String subjectValue = "subject";
        UserInfo userInfo = new UserInfo(new Subject(subjectValue));
        Subject subjectFromIdToken = new Subject(subjectValue);

        // when & then
        validator.validateUserInfoSubject(userInfo, subjectFromIdToken);
    }

    @Test
    @DisplayName("Validation of UserInfo should fail when subjects from ID token and user info endpoint are not equal")
    void validateUserInfoSubject__differingSubjects() throws OAuth2Exception {

        // given
        UserInfo userInfo = new UserInfo(new Subject("subject1"));
        Subject subjectFromIdToken = new Subject("subject2");

        // when & then
        assertThatThrownBy(() -> validator.validateUserInfoSubject(userInfo, subjectFromIdToken))
                .isInstanceOf(OAuth2Exception.class);
    }

    // -------------------- PRIVATE --------------------

    private IDTokenValidator setAndGetInternalMockValidator() throws Exception {
        IDTokenValidator mockValidator = Mockito.mock(IDTokenValidator.class);
        Field validatorField = OIDCValidator.class.getDeclaredField("validator");
        validatorField.setAccessible(true);
        validatorField.set(validator, mockValidator);
        validatorField.setAccessible(false);
        return mockValidator;
    }
}