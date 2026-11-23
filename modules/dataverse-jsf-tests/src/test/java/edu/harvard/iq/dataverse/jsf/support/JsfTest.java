package edu.harvard.iq.dataverse.jsf.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a JSF frontend test. Use it instead of {@code @Test}: the method runs
 * once per configured browser (chromium, firefox, webkit by default), each
 * time in a fresh, already-logged-in browser context that is traced, recorded
 * on video and screenshotted.
 *
 * The method may declare parameters of type {@link com.microsoft.playwright.Page},
 * {@link JsfFixtures} and {@link Cleanup}.
 *
 * <pre>
 * &#64;JsfTest
 * &#64;DisplayName("Restrict a file")
 * public void testRestrictFile(Page page, JsfFixtures fixtures) {
 *     DatasetRef dataset = fixtures.createDataset(new CreateDatasetOptions().setFiles(TestData.SAMPLE_TEXT));
 *     // page is now on the new dataset's page.
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(JsfTestExtension.class)
public @interface JsfTest {
}
