package edu.harvard.iq.dataverse.mydata;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MyDataUtilTest {

    static List<String> userIdentifier() {
        return List.of("@nzaugg", "nzaugg@", "nzaugg", "123nzaugg", " ", "@", "n");
    }

    @ParameterizedTest
    @NullSource
    void testFormatUserIdentifierAsAssigneeIdentifierNull(String userIdentifier) {
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierAsAssigneeIdentifier(userIdentifier);
        assertNull(formattedUserIdentifier);
    }
    
    @ParameterizedTest
    @MethodSource("userIdentifier")
    void testFormatUserIdentifierAsAssigneeIdentifierOneCharString(String userIdentifier) {
        assumeTrue(userIdentifier.startsWith("@"));
        
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierAsAssigneeIdentifier(userIdentifier);
        assertEquals(formattedUserIdentifier, userIdentifier);
    }
    
    @ParameterizedTest
    @MethodSource("userIdentifier")
    void testFormatUserIdentifierAsAssigneeIdentifier(String userIdentifier) {
        assumeTrue(!userIdentifier.startsWith("@"));
        
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierAsAssigneeIdentifier(userIdentifier);
        assertEquals(formattedUserIdentifier, "@" + userIdentifier);
    }
    
    @ParameterizedTest
    @NullSource
    void testFormatUserIdentifierForMyDataFormNull(String userIdentifier) {
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierForMyDataForm(userIdentifier);
        assertNull(formattedUserIdentifier);
    }
    
    @ParameterizedTest
    @MethodSource("userIdentifier")
    void testFormatUserIdentifierForMyDataFormOneCharString(String userIdentifier) {
        assumeTrue(userIdentifier.startsWith("@"));
        assumeTrue(userIdentifier.length() == 1);
        
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierForMyDataForm(userIdentifier);
        assertNull(formattedUserIdentifier);
    }
    
    @ParameterizedTest
    @MethodSource("userIdentifier")
    void testFormatUserIdentifierForMyDataFormLongerString(String userIdentifier) {
        assumeTrue(userIdentifier.startsWith("@"));
        assumeTrue(userIdentifier.length() > 1);
        
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierForMyDataForm(userIdentifier);
        assertEquals(formattedUserIdentifier, userIdentifier.substring(1));
    }
    
    @ParameterizedTest
    @MethodSource("userIdentifier")
    @EmptySource
    void testFormatUserIdentifierForMyDataForm(String userIdentifier) {
        assumeTrue(!userIdentifier.startsWith("@"));
        
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierForMyDataForm(userIdentifier);
        assertEquals(formattedUserIdentifier, userIdentifier);
    }

}