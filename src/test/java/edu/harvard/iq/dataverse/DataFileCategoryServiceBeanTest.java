package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author adaybujeda
 */
@ExtendWith(MockitoExtension.class)
public class DataFileCategoryServiceBeanTest {

    @Mock
    private SettingsServiceBean settingsServiceBean;
    @InjectMocks
    private DataFileCategoryServiceBean target;

    @Test
    public void getFileCategories_should_return_default_file_categories_in_expected_order_when_no_override_configured() {
        Mockito.when(settingsServiceBean.get(DataFileCategoryServiceBean.FILE_CATEGORIES_KEY)).thenReturn(null);

        List<String> result = target.getFileCategories();

        MatcherAssert.assertThat(result.size(), Matchers.is(3));
        MatcherAssert.assertThat(result.get(0), Matchers.is("Documentation"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("Data"));
        MatcherAssert.assertThat(result.get(2), Matchers.is("Code"));
    }

    @Test
    public void getFileCategories_should_return_default_file_categories_in_expected_order_when_empty_override_is_configured() {
        Mockito.when(settingsServiceBean.get(DataFileCategoryServiceBean.FILE_CATEGORIES_KEY)).thenReturn("  ");

        List<String> result = target.getFileCategories();

        MatcherAssert.assertThat(result.size(), Matchers.is(3));
        MatcherAssert.assertThat(result.get(0), Matchers.is("Documentation"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("Data"));
        MatcherAssert.assertThat(result.get(2), Matchers.is("Code"));
    }

    @Test
    public void getFileCategories_should_return_override_file_categories_from_settings_service() {
        setup_override("Override01, Override02");

        List<String> result = target.getFileCategories();

        MatcherAssert.assertThat(result.size(), Matchers.is(2));
        MatcherAssert.assertThat(result.get(0), Matchers.is("Override01"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("Override02"));
    }

    @Test
    public void getFileCategories_should_trim_override_values() {
        setup_override(" Test Value 01  , Test Value 02  ");

        List<String> result = target.getFileCategories();

        MatcherAssert.assertThat(result.size(), Matchers.is(2));
        MatcherAssert.assertThat(result.get(0), Matchers.is("Test Value 01"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("Test Value 02"));
    }

    @Test
    public void getFileCategories_should_ignore_empty_override_values() {
        setup_override(",value01,,value02,,value03,,");

        List<String> result = target.getFileCategories();

        MatcherAssert.assertThat(result.size(), Matchers.is(3));
        MatcherAssert.assertThat(result.get(0), Matchers.is("value01"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("value02"));
        MatcherAssert.assertThat(result.get(2), Matchers.is("value03"));
    }

    @Test
    public void mergeDatasetFileCategories_should_handle_null_datafile_categories() {
        Mockito.when(settingsServiceBean.get(DataFileCategoryServiceBean.FILE_CATEGORIES_KEY)).thenReturn(null);

        List<String> result = target.mergeDatasetFileCategories(null);

        MatcherAssert.assertThat(result.size(), Matchers.is(3));
        MatcherAssert.assertThat(result.get(0), Matchers.is("Documentation"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("Data"));
        MatcherAssert.assertThat(result.get(2), Matchers.is("Code"));
    }

    @Test
    public void mergeDatasetFileCategories_should_add_dataset_values_first_then_default_categories() {
        Mockito.when(settingsServiceBean.get(DataFileCategoryServiceBean.FILE_CATEGORIES_KEY)).thenReturn(null);

        List<String> result = target.mergeDatasetFileCategories(setup_data_file_categories("dataset01", "dataset02"));

        MatcherAssert.assertThat(result.size(), Matchers.is(5));
        MatcherAssert.assertThat(result.get(0), Matchers.is("dataset01"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("dataset02"));
        MatcherAssert.assertThat(result.get(2), Matchers.is("Documentation"));
        MatcherAssert.assertThat(result.get(3), Matchers.is("Data"));
        MatcherAssert.assertThat(result.get(4), Matchers.is("Code"));
    }

    @Test
    public void mergeDatasetFileCategories_should_add_dataset_values_first_then_override_categories() {
        setup_override("override01, override02");

        List<String> result = target.mergeDatasetFileCategories(setup_data_file_categories("dataset01", "dataset02"));

        MatcherAssert.assertThat(result.size(), Matchers.is(4));
        MatcherAssert.assertThat(result.get(0), Matchers.is("dataset01"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("dataset02"));
        MatcherAssert.assertThat(result.get(2), Matchers.is("override01"));
        MatcherAssert.assertThat(result.get(3), Matchers.is("override02"));
    }

    @Test
    public void mergeDatasetFileCategories_should_ignore_duplicates() {
        Mockito.when(settingsServiceBean.get(DataFileCategoryServiceBean.FILE_CATEGORIES_KEY)).thenReturn(null);

        List<String> result = target.mergeDatasetFileCategories(setup_data_file_categories("Code", "Data", "Custom"));

        MatcherAssert.assertThat(result.size(), Matchers.is(4));
        MatcherAssert.assertThat(result.get(0), Matchers.is("Code"));
        MatcherAssert.assertThat(result.get(1), Matchers.is("Data"));
        MatcherAssert.assertThat(result.get(2), Matchers.is("Custom"));
        MatcherAssert.assertThat(result.get(3), Matchers.is("Documentation"));
    }

    private void setup_override(String overrideValue) {
        String currentLang = BundleUtil.getCurrentLocale().getLanguage();

        Mockito.when(settingsServiceBean.get(DataFileCategoryServiceBean.FILE_CATEGORIES_KEY)).thenReturn(overrideValue);
        Mockito.when(settingsServiceBean.get(DataFileCategoryServiceBean.FILE_CATEGORIES_KEY, currentLang, overrideValue)).thenReturn(overrideValue);
    }

    private List<DataFileCategory> setup_data_file_categories(String... names) {
        return Arrays.stream(names).map(name -> {
            DataFileCategory dataFileCategory = new DataFileCategory();
            dataFileCategory.setName(name);
            return dataFileCategory;
        }).collect(Collectors.toList());
    }

}