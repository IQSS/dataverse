package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OAuth2LoginBackingBeanTest {
    
    OAuth2LoginBackingBean loginBackingBean = new OAuth2LoginBackingBean();
    
    @Test
    void createStateFailNullIdp() {
        // given
        AbstractOAuth2AuthenticationProvider idp = null;
        Optional<String> page = Optional.empty();
        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            loginBackingBean.createState(idp, page);
        });
    }
}