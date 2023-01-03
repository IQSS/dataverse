package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class AbstractOAuth2AuthenticationProviderTest {
    
    AbstractOAuth2AuthenticationProvider idp;
    
    @BeforeEach
    void setUp() {
        this.idp = Mockito.mock(AbstractOAuth2AuthenticationProvider.class, Mockito.CALLS_REAL_METHODS);
    }
    
    /**
     * Ensure this is working as expected.
     */
    @Test
    void getOrderDefaultValue() {
        assertEquals(100, this.idp.getOrder());
    }
    
}