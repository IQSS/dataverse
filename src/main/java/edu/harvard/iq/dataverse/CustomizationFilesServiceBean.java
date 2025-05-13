package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.customization.CustomizationConstants;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
@Stateless
@Named
public class CustomizationFilesServiceBean {

    @EJB
    SettingsServiceBean settingsService;

    public void getContents(PrintWriter writer, String fileType) {
        Path physicalPath = Paths.get(getFilePath(fileType));
        FileInputStream inputStream = null;
        BufferedReader in = null;
        try {
            File fileIn = physicalPath.toFile();
            if (fileIn != null) {
                inputStream = new FileInputStream(fileIn);
                in = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder responseData = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    responseData.append(line);
                    writer.println(line);
                }
                inputStream.close();
            } else {
                /*
                   If the file doesn't exist or it is unreadable we don't care
                */
            }

        } catch (Exception e) {
                /*
                   If the file doesn't exist, or it is unreadable we don't care
                */
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(in);
        }

    }
    private String getFilePath(String fileTypeParam){

        String nonNullDefaultIfKeyNotFound = "";

        if (fileTypeParam.equals(CustomizationConstants.fileTypeHomePage)) {

            // Homepage
            return settingsService.getValueForKey(SettingsServiceBean.Key.HomePageCustomizationFile, nonNullDefaultIfKeyNotFound);

        } else if (fileTypeParam.equals(CustomizationConstants.fileTypeHeader)) {

            // Header
            return settingsService.getValueForKey(SettingsServiceBean.Key.HeaderCustomizationFile, nonNullDefaultIfKeyNotFound);

        } else if (fileTypeParam.equals(CustomizationConstants.fileTypeFooter)) {

            // Footer
            return settingsService.getValueForKey(SettingsServiceBean.Key.FooterCustomizationFile, nonNullDefaultIfKeyNotFound);

        } else if (fileTypeParam.equals(CustomizationConstants.fileTypeStyle)) {

            // Style (css)
            return settingsService.getValueForKey(SettingsServiceBean.Key.StyleCustomizationFile, nonNullDefaultIfKeyNotFound);

        } else if (fileTypeParam.equals(CustomizationConstants.fileTypeAnalytics)) {

            // Analytics - appears in head
            return settingsService.getValueForKey(SettingsServiceBean.Key.WebAnalyticsCode, nonNullDefaultIfKeyNotFound);

        } else if (fileTypeParam.equals(CustomizationConstants.fileTypeLogo)) {

            // Logo for installation - appears in header
            return settingsService.getValueForKey(SettingsServiceBean.Key.LogoCustomizationFile, nonNullDefaultIfKeyNotFound);
        }


        return "";
    }

}
