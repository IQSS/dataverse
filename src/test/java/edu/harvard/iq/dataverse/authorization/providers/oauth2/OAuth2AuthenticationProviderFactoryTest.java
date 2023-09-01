package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author michael
 */
public class OAuth2AuthenticationProviderFactoryTest extends OAuth2AuthenticationProviderFactory {
    
    public OAuth2AuthenticationProviderFactoryTest() {
    }

    @Test
    public void testDictionaryParting() {
        Map<String,String> expected = new HashMap<>();
        expected.put("n1", "v1");
        expected.put("n2", "v2a v2b v2c");
        expected.put("n3", "v3a\nv3b\nv3c");
        
        assertEquals( expected, parseFactoryData("n1:v1|n2:v2a v2b v2c|n3:v3a\nv3b\nv3c"));
        assertEquals( expected, parseFactoryData(" n1: v1 | n2:v2a v2b v2c | n3: v3a\nv3b\nv3c"));
        assertEquals( expected, parseFactoryData(" n1: v1 |\nn2:v2a v2b v2c \n| n3: v3a\nv3b\nv3c\n"));
        
                
    }
    
}
