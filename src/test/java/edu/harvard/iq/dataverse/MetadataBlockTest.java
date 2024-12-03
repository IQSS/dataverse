package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetadataBlockTest {

    private static final String PROPERTIES_FILE_NAME = "metadataBlockTest";

    @Test
    public void equals_should_be_based_on_id_only() {
        Long id = MocksFactory.nextId();
        MetadataBlock metadataBlock1 = new MetadataBlock();
        metadataBlock1.setId(id);
        metadataBlock1.setName(UUID.randomUUID().toString());
        MetadataBlock metadataBlock2 = new MetadataBlock();
        metadataBlock2.setId(id);
        metadataBlock1.setName(UUID.randomUUID().toString());

        MatcherAssert.assertThat(metadataBlock1.equals(metadataBlock2), Matchers.is(true));

        metadataBlock1 = new MetadataBlock();
        metadataBlock1.setId(MocksFactory.nextId());
        metadataBlock1.setName("EQUAL");
        metadataBlock2 = new MetadataBlock();
        metadataBlock2.setId(MocksFactory.nextId());
        metadataBlock1.setName("EQUAL");

        MatcherAssert.assertThat(metadataBlock1.equals(metadataBlock2), Matchers.is(false));
    }

    @Test
    public void getLocaleDisplayName_should_default_value_from_displayName_when_bundle_not_found() {
        MetadataBlock target = Mockito.spy(new MetadataBlock());
        target.setName(UUID.randomUUID().toString());
        target.setDisplayName(UUID.randomUUID().toString());

        //Value when no resource file found with metadata block name
        MatcherAssert.assertThat(target.getLocaleDisplayName(), Matchers.is(target.getDisplayName()));
        Mockito.verify(target).getLocaleValue("metadatablock.displayName");
    }

    @Test
    public void getLocaleDisplayName_should_get_value_from_properties_based_on_name() {
        MetadataBlock target = Mockito.spy(new MetadataBlock());
        target.setName(PROPERTIES_FILE_NAME);
        target.setDisplayName(UUID.randomUUID().toString());

        // Values is coming from the metadataBlockTest.properties file
        MatcherAssert.assertThat(target.getLocaleDisplayName(), Matchers.is("property_value_for_displayName"));
        Mockito.verify(target).getLocaleValue("metadatablock.displayName");
    }

    @Test
    public void getLocaleDisplayFacet_should_default_value_from_displayName_when_bundle_not_found() {
        MetadataBlock target = Mockito.spy(new MetadataBlock());
        target.setName(UUID.randomUUID().toString());
        target.setDisplayName(UUID.randomUUID().toString());

        MatcherAssert.assertThat(target.getLocaleDisplayFacet(), Matchers.is(target.getDisplayName()));
        Mockito.verify(target).getLocaleValue("metadatablock.displayFacet");
    }

    @Test
    public void getLocaleDisplayFacet_should_get_value_from_properties_based_on_name() {
        MetadataBlock target = Mockito.spy(new MetadataBlock());
        target.setName(PROPERTIES_FILE_NAME);
        target.setDisplayName(UUID.randomUUID().toString());

        // Values is coming from the metadataBlockTest.properties file
        MatcherAssert.assertThat(target.getLocaleDisplayFacet(), Matchers.is("property_value_for_displayFacet"));
        Mockito.verify(target).getLocaleValue("metadatablock.displayFacet");
    }

    @Test
    public void testLanguageBundle() throws Exception {
        String fileName = "scripts/api/data/metadatablocks/citation.tsv";
        Set<String> languages = BundleUtil.getResourceBundle("citation").keySet();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            String bundleKey = "";
            int missingCount = 0;
            AtomicInteger unusedCount = new AtomicInteger();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#controlledVocabulary")) {
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("\tlanguage")) {
                            String[] values = line.split("\t");
                            String key = values[2];
                            bundleKey = StringUtils.stripAccents("controlledvocabulary.language." + key.toLowerCase().replace(" ", "_"));
                            if (languages.contains(bundleKey)) {
                                languages.remove(bundleKey);
                            } else {
                                missingCount++;
                                System.out.println("Missing key:" + key.toLowerCase().replace(" ", "_") + " " + bundleKey);
                            }
                        }
                    }
                    languages.forEach(l -> {
                        if (l.startsWith("controlledvocabulary.language")) {
                            unusedCount.getAndIncrement();
                            System.out.println("Unused key:" + l);
                        }
                    });
                    System.out.println("Missing count:"+missingCount);
                    System.out.println("Unused Count:" + unusedCount.get());
                }
            }
            assertEquals(0, missingCount);
            assertEquals(0, unusedCount.get());
        }
    }
}
