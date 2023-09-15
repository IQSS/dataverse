package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.mocks.MocksFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;

public class DataverseUtilTest {

    @Test
    public void testGetSuggestedDataverseNameOnCreate() {
        System.out.println("getSuggestedDataverseNameOnCreate");
        assertEquals(null, DataverseUtil.getSuggestedDataverseNameOnCreate(null));
        assertEquals("Homer Simpson Dataverse", DataverseUtil
                .getSuggestedDataverseNameOnCreate(MocksFactory.makeAuthenticatedUser("Homer", "Simpson")));
    }

    @Test
    public void testCheckMetadataLanguageCases() {
        Map<String, String> emptyMLangSettingMap = new HashMap<String, String>();
        Map<String, String> mLangSettingMap = new HashMap<String, String>();
        mLangSettingMap.put("en", "English");
        mLangSettingMap.put("fr", "French");
        Dataverse undefinedParent = new Dataverse();
        undefinedParent.setMetadataLanguage(DvObjectContainer.UNDEFINED_METADATA_LANGUAGE_CODE);
        Dataset undefinedD = new Dataset();
        undefinedD.setMetadataLanguage(DvObjectContainer.UNDEFINED_METADATA_LANGUAGE_CODE);
        Dataverse definedParent = new Dataverse();
        definedParent.setMetadataLanguage("en");
        Dataset definedEnglishD = new Dataset();
        definedEnglishD.setMetadataLanguage("en");
        Dataset definedFrenchD = new Dataset();
        definedFrenchD.setMetadataLanguage("fr");
        Dataset definedSpanishD = new Dataset();
        definedSpanishD.setMetadataLanguage("es");
        
        // Not set tests:
        //Good - no mLang sent, parent doesn't have one
        assertDoesNotThrow(() -> DataverseUtil.checkMetadataLangauge(undefinedD, undefinedParent, emptyMLangSettingMap));
        //Bad - one sent, parent doesn't have one
        assertThrows(BadRequestException.class, () -> DataverseUtil.checkMetadataLangauge(definedEnglishD, undefinedParent, emptyMLangSettingMap));
        //Good - one sent, matches parent
        assertDoesNotThrow(() -> DataverseUtil.checkMetadataLangauge(definedEnglishD, definedParent, emptyMLangSettingMap));
        //Bad - one sent, doesn't match parent
        assertThrows(BadRequestException.class, () -> DataverseUtil.checkMetadataLangauge(definedFrenchD, definedParent, emptyMLangSettingMap));
        
        //With setting tests
        //Bad - one sent, parent doesn't have one
        assertThrows(BadRequestException.class, () -> DataverseUtil.checkMetadataLangauge(undefinedD, undefinedParent, mLangSettingMap));
        //Good - sent, parent undefined, is allowed by setting
        assertDoesNotThrow(() -> DataverseUtil.checkMetadataLangauge(definedEnglishD, undefinedParent, mLangSettingMap));
        //Bad  one sent, parent undefined, not allowed by setting
        assertThrows(BadRequestException.class, () -> DataverseUtil.checkMetadataLangauge(definedSpanishD, undefinedParent, mLangSettingMap));
        //Bad - one sent, doesn't match parent
        assertThrows(BadRequestException.class, () -> DataverseUtil.checkMetadataLangauge(definedFrenchD, definedParent, mLangSettingMap));
        //Bad - undefined sent, parent is defined
        assertThrows(BadRequestException.class, () -> DataverseUtil.checkMetadataLangauge(undefinedD, definedParent, mLangSettingMap));
        //Good - sent, parent defined, they match
        assertDoesNotThrow(() -> DataverseUtil.checkMetadataLangauge(definedEnglishD, definedParent, mLangSettingMap));
    }

}
