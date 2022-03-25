package edu.harvard.iq.dataverse.authorization.providers.saml;

import com.onelogin.saml2.Auth;
import com.onelogin.saml2.settings.Saml2Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SamlUserDataFactoryTest {

    @Mock
    private Auth auth;

    @Mock
    private Saml2Settings settings;

    @Test
    void create() {
        // given
        when(auth.getNameId()).thenReturn("testNameId");
        when(settings.getIdpEntityId()).thenReturn("idpEntityId");
        when(auth.getSettings()).thenReturn(settings);
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("cn", Collections.singletonList("firstName"));
        attributes.put("sn", Collections.singletonList("surname"));
        attributes.put("mail", Collections.singletonList("secondEmail; email ,thirdEmail;"));
        when(auth.getAttributes()).thenReturn(attributes);

        // when
        SamlUserData data = SamlUserDataFactory.create(auth);

        // then
        assertThat(data)
                .extracting(SamlUserData::getCompositeId, SamlUserData::getName,
                        SamlUserData::getSurname, SamlUserData::getEmail)
                .containsExactly("idpEntityId|testNameId", "firstName",
                        "surname", "email");
    }
}