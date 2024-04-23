package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service to manage the default values for Dataset file categories and allow
 * to be overridden with a :FileCategories Settings configuration.
 *
 * @author adaybujeda
 */
@Stateless
public class DataFileCategoryServiceBean {

    public static final String FILE_CATEGORIES_KEY = ":FileCategories";

    @EJB
    private SettingsServiceBean settingsService;

    public List<String> mergeDatasetFileCategories(List<DataFileCategory> datasetFileCategories) {
        List<DataFileCategory> fileCategories = Optional.ofNullable(datasetFileCategories).orElse(Collections.emptyList());
        List<String> defaultFileCategories = getFileCategories();

        //avoid resizing
        List<String> mergedFileCategories = new ArrayList<>(defaultFileCategories.size() + fileCategories.size());

        for(DataFileCategory category: fileCategories) {
            mergedFileCategories.add(category.getName());
        }

        for(String defaultCategory: defaultFileCategories) {
            if (!mergedFileCategories.contains(defaultCategory)) {
                mergedFileCategories.add(defaultCategory);
            }
        }

        return mergedFileCategories;
    }

    public List<String> getFileCategories() {
        List<String> fileCategoriesOverride = getFileCategoriesOverride();
        return fileCategoriesOverride.isEmpty() ? getFileCategoriesDefault() : fileCategoriesOverride;
    }

    private List<String> getFileCategoriesDefault() {
        // "Documentation", "Data" and "Code" are the 3 default categories that we
        // present by default
        return Arrays.asList(
                BundleUtil.getStringFromBundle("dataset.category.documentation"),
                BundleUtil.getStringFromBundle("dataset.category.data"),
                BundleUtil.getStringFromBundle("dataset.category.code")
        );
    }

    private List<String> getFileCategoriesOverride() {
        String applicationLanguage = BundleUtil.getCurrentLocale().getLanguage();
        Optional<String> fileCategoriesOverride = Optional.ofNullable(settingsService.get(FILE_CATEGORIES_KEY));

        if (fileCategoriesOverride.isPresent()) {
            // There is an override, check if there is language specific value
            String overrideValue = settingsService.get(FILE_CATEGORIES_KEY, applicationLanguage, fileCategoriesOverride.get());

            return parseCategoriesString(overrideValue);
        }

        return Collections.emptyList();
    }

    private List<String> parseCategoriesString(String categoriesString) {
        if (categoriesString == null) {
            return Collections.emptyList();
        }

        String[] categories = categoriesString.split(",");
        return Arrays.stream(categories).map(item -> item.trim()).filter(item -> !item.isBlank()).collect(Collectors.toUnmodifiableList());
    }

}
