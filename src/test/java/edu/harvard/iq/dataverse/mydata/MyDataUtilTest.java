package edu.harvard.iq.dataverse.mydata;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@RunWith(Theories.class)
public class MyDataUtilTest {

    @DataPoints
    public static String[] userIdentifier = { 
        "@nzaugg", "nzaugg@", "nzaugg", "123nzaugg", "", " ", null, "@",  "n" };

    @Theory
    public void testFormatUserIdentifierAsAssigneeIdentifierNull(String userIdentifier) {
        assumeTrue(userIdentifier == null);
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierAsAssigneeIdentifier(userIdentifier);
        assertTrue(formattedUserIdentifier ==  null);
    }

    @Theory
    public void testFormatUserIdentifierAsAssigneeIdentifierOneCharString(String userIdentifier) {
        assumeTrue(userIdentifier != null);
        assumeTrue(userIdentifier.startsWith("@"));
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierAsAssigneeIdentifier(userIdentifier);
        assertTrue(formattedUserIdentifier.equals(userIdentifier));
    }

    @Theory
    public void testFormatUserIdentifierAsAssigneeIdentifier(String userIdentifier) {
        assumeTrue(userIdentifier != null);
        assumeTrue(!userIdentifier.startsWith("@"));
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierAsAssigneeIdentifier(userIdentifier);
        assertTrue(formattedUserIdentifier.equals("@" + userIdentifier));
    }

    @Theory
    public void testFormatUserIdentifierForMyDataFormNull(String userIdentifier) {
        assumeTrue(userIdentifier == null);
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierForMyDataForm(userIdentifier);
        assertTrue(formattedUserIdentifier ==  null);
    }

    @Theory
    public void testFormatUserIdentifierForMyDataFormOneCharString(String userIdentifier) {
        assumeTrue(userIdentifier != null);
        assumeTrue(userIdentifier.startsWith("@"));
        assumeTrue(userIdentifier.length() == 1);
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierForMyDataForm(userIdentifier);
        assertTrue(formattedUserIdentifier ==  null);
    }

    @Theory
    public void testFormatUserIdentifierForMyDataFormLongerString(String userIdentifier) {
        assumeTrue(userIdentifier != null);
        assumeTrue(userIdentifier.startsWith("@"));
        assumeTrue(userIdentifier.length() > 1);
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierForMyDataForm(userIdentifier);
        assertTrue(formattedUserIdentifier.equals(userIdentifier.substring(1)));
    }

    @Theory
    public void testFormatUserIdentifierForMyDataForm(String userIdentifier) {
        assumeTrue(userIdentifier != null);
        assumeTrue(!userIdentifier.startsWith("@"));
        String formattedUserIdentifier = MyDataUtil.formatUserIdentifierForMyDataForm(userIdentifier);
        assertTrue(formattedUserIdentifier.equals(userIdentifier));
    }

}