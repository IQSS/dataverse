package edu.harvard.iq.dataverse.export;

import org.junit.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static edu.harvard.iq.dataverse.export.ExportUtil.isPerson;
import static edu.harvard.iq.dataverse.export.ExportUtil.normalizeAccents;
import static org.junit.jupiter.api.Assertions.*;


public class ExportUtilTest {

    @ParameterizedTest
    @MethodSource("parametersForIsPersonTest")
    public void testIsPerson(String name, boolean expected) {
        Assert.assertEquals(expected, isPerson(name));
    }

    @Test
    public void testNormalizeAccents() {
        String accents = "È,É,Ê,Ë,Ę,Û,Ù,Ï,Î,À,Â,Ą,Ô,Ó,è,é,ê,ë,ę,û,ù,ï,î,à,â,ą,ô,ó,Ç,Ć,ç,ć,Ã,ã,Õ,õ,Ś,ś,Ż,ź,Ź,ź,Ń,ń,Ł,ł";
        String expected = "E,E,E,E,E,U,U,I,I,A,A,A,O,O,e,e,e,e,e,u,u,i,i,a,a,a,o,o,C,C,c,c,A,a,O,o,S,s,Z,z,Z,z,N,n,L,l";
        String accents2 = "çÇáéíóúýÁÉÍÓÚÝàèìòùÀÈÌÒÙãõñäëïöüÿÄËÏÖÜÃÕÑâêîôûÂÊÎÔÛŁł";
        String expected2 = "cCaeiouyAEIOUYaeiouAEIOUaonaeiouyAEIOUAONaeiouAEIOULl";

        assertEquals(expected, normalizeAccents(accents));
        assertEquals(expected2, normalizeAccents(accents2));
    }

    private static Stream<Arguments> parametersForIsPersonTest() {
        return Stream.of(
                Arguments.of("Jan Kowalski", true),
                Arguments.of("John Kowalsky", true),
                Arguments.of("Kowalski, Jan", true),
                Arguments.of("Kowalski, J.", true),
                Arguments.of("Kowalski, J.K.", true),
                Arguments.of("Kowalski, J.K.P.", true),
                Arguments.of("Jan Maria Kowalski", true),
                Arguments.of("Jan Maria Kowalski Rokita", true),
                Arguments.of("Xxx Kowalski", false),
                Arguments.of("Kowalski, Xxx", false),
                Arguments.of("Kowalski, j.", false),
                Arguments.of("Kowalski, J.x.", false),
                Arguments.of("Kowalski, J.5.", false),
                Arguments.of("Kowalski, J.k.P.", false),
                Arguments.of("Jan Maria Kowalski Rokita Nowak", false),
                Arguments.of("Jan", false),
                Arguments.of("Władyslaw Kowalski", true),
                Arguments.of("Wladysław Kowalski", true),
                Arguments.of("Wladyslaw Kowalski", true),
                Arguments.of("Łucja Nowak", true),
                Arguments.of("Lucja Nowak", true)
        );
    }

}
