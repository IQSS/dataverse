package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.customization.CustomizationConstants;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author skraffmi
 */
@WebServlet(name = "CustomizationFilesServlet", urlPatterns = {"/CustomizationFilesServlet"})
public class CustomizationFilesServlet extends HttpServlet {

    @Inject
    SettingsServiceBean settingsService;

    @Inject
    DataverseSession dataverseSession;

    private static final Logger logger = Logger.getLogger(CustomizationFilesServlet.class.getSimpleName());

    private static final Map<String, Key> SETTINGS_MAPPING = Initializer.createSettingsMapping();

    private static final Set<Key> LOCALIZED_FILES_KEYS = Initializer.createSetOfLocalizedFilesKeys();

    private static final String EN_LOCALE_CODE = Locale.ENGLISH.getLanguage();

    // -------------------- LOGIC --------------------

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "CustomizationFilesServlet – for serving customizable parts of the page";
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=UTF-8");
        String customFileType = request.getParameter("customFileType");

        Optional<File> file = findFile(customFileType);
        if (!file.isPresent()) {
            return;
        }

        File fileIn = file.get();
        try (FileInputStream inputStream = new FileInputStream(fileIn);
             BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
             PrintWriter out = response.getWriter()) {
            in.lines()
                    .forEach(out::println);
        } catch (Exception e) {
            // We don't want to send trash to logs, but nevertheless some info should be present on demand
            logger.log(Level.FINEST, e, () -> "Servlet encountered exception: ");
        }
    }

    // -------------------- PRIVATE --------------------

    private Optional<File> findFile(String customFileType) {
        return getPaths(customFileType).stream()
                .map(Path::toFile)
                .filter(f -> f.exists() && f.isFile())
                .findFirst();
    }

    private List<Path> getPaths(String customFileType) {
        Key key = SETTINGS_MAPPING.get(customFileType);
        String basePath = getFilePath(key);
        List<Path> paths = new ArrayList<>();
        if (LOCALIZED_FILES_KEYS.contains(key)) {
            createLocalizedPathToFile(basePath)
                    .ifPresent(paths::add);
        }
        paths.add(Paths.get(basePath));
        return paths;
    }

    private String getFilePath(Key key) {
        return key != null ? settingsService.getValueForKey(key) : StringUtils.EMPTY;
    }

    private Optional<Path> createLocalizedPathToFile(String basePath) {
        return Optional.ofNullable(basePath)
                .filter(p -> p.contains("."))
                .map(p -> Paths.get(interpolateLocaleCodeIntoPathToFile(p, obtainLocaleCode())));
    }

    private String interpolateLocaleCodeIntoPathToFile(String basePath, String localeCode) {
        int extensionDotIndex = basePath.lastIndexOf(".");
        String localeInfix = EN_LOCALE_CODE.equals(localeCode) ? StringUtils.EMPTY : "_" + localeCode;
        return basePath.substring(0, extensionDotIndex) + localeInfix + basePath.substring(extensionDotIndex);
    }

    private String obtainLocaleCode() {
        return Optional.ofNullable(dataverseSession)
                .map(DataverseSession::getLocaleCode)
                .orElse(EN_LOCALE_CODE);
    }

    // -------------------- INNER CLASSES  --------------------

    private static class Initializer {
        static Map<String, Key> createSettingsMapping() {
            HashMap<String, Key> mapping = new HashMap<>();
            mapping.put(CustomizationConstants.fileTypeHomePage, Key.HomePageCustomizationFile); // Homepage
            mapping.put(CustomizationConstants.fileTypeHeader, Key.HeaderCustomizationFile); // Header
            mapping.put(CustomizationConstants.fileTypeFooter, Key.FooterCustomizationFile); // Footer
            mapping.put(CustomizationConstants.fileTypeStyle, Key.StyleCustomizationFile); // Style (css)
            mapping.put(CustomizationConstants.fileTypeAnalytics, Key.WebAnalyticsCode); // Analytics - appears in head
            mapping.put(CustomizationConstants.fileTypeLogo, Key.LogoCustomizationFile); // Logo for installation - appears in header
            return Collections.unmodifiableMap(mapping);
        }

        static Set<Key> createSetOfLocalizedFilesKeys() {
            return Collections.singleton(Key.FooterCustomizationFile);
        }
    }
}
