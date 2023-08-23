package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.Test;

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
        try {
            DataverseUtil.checkMetadataLangauge(undefinedD, undefinedParent, emptyMLangSettingMap);
        } catch (BadRequestException e) {
            Assert.fail();
        }
        //Bad - one sent, parent doesn't have one
        try {
            DataverseUtil.checkMetadataLangauge(definedEnglishD, undefinedParent, emptyMLangSettingMap);
            Assert.fail();
        } catch (BadRequestException e) {
        }
        //Good - one sent, matches parent
        try {
            DataverseUtil.checkMetadataLangauge(definedEnglishD, definedParent, emptyMLangSettingMap);

        } catch (BadRequestException e) {
            Assert.fail();
        }
        //Bad - one sent, doesn't match parent
        try {
            DataverseUtil.checkMetadataLangauge(definedFrenchD, definedParent, emptyMLangSettingMap);
            Assert.fail();
        } catch (BadRequestException e) {
        }
        //With setting tests
      //Bad - one sent, parent doesn't have one
        try {
            DataverseUtil.checkMetadataLangauge(undefinedD, undefinedParent, mLangSettingMap);
            Assert.fail();
        } catch (BadRequestException e) {
        }
        //Good - sent, parent undefined, is allowed by setting
        try {
            DataverseUtil.checkMetadataLangauge(definedEnglishD, undefinedParent, mLangSettingMap);
        } catch (BadRequestException e) {
            Assert.fail();
        }
        //Bad  one sent, parent undefined, not allowed by setting
        try {
            DataverseUtil.checkMetadataLangauge(definedSpanishD, undefinedParent, mLangSettingMap);
            Assert.fail();
        } catch (BadRequestException e) {
        }
        //Bad - one sent, doesn't match parent
        try {
            DataverseUtil.checkMetadataLangauge(definedFrenchD, definedParent, mLangSettingMap);
            Assert.fail();
        } catch (BadRequestException e) {
        }
        //Bad - undefined sent, parent is defined
        try {
            DataverseUtil.checkMetadataLangauge(undefinedD, definedParent, mLangSettingMap);
            Assert.fail();
        } catch (BadRequestException e) {
        }
      //Good - sent, parent defined, they match
        try {
            DataverseUtil.checkMetadataLangauge(definedEnglishD, definedParent, mLangSettingMap);
        } catch (BadRequestException e) {
            Assert.fail();
        }
    }

}
