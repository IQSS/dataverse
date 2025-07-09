/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.customization.CustomizationConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.ejb.EJB;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;

/**
 *
 * @author skraffmi
 */
@WebServlet(name = "CustomizationFilesServlet", urlPatterns = {"/CustomizationFilesServlet"})
public class CustomizationFilesServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(CustomizationFilesServlet.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;
            
            
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(request.getContentType());

        String customFileType = request.getParameter("customFileType");
        String filePath = getFilePath(customFileType);

        Path physicalPath = Paths.get(filePath);
        FileInputStream inputStream = null;
        BufferedReader in = null;
        try {
            File fileIn = physicalPath.toFile();
            if (fileIn != null) {
                Tika tika = new Tika();
                try {
                    String mimeType = tika.detect(fileIn);
                    response.setContentType(mimeType);
                } catch (Exception e) {
                    String currentDirectory = System.getProperty("user.dir");
                    response.setHeader("X-cur-dir", currentDirectory);
                    logger.info("Error getting MIME Type for " + filePath + " : " + e.getMessage());
                }
                inputStream = new FileInputStream(fileIn);

                in = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                StringBuilder responseData = new StringBuilder();
                String currentDirectory = System.getProperty("user.dir");
                try (PrintWriter out = response.getWriter()) {
                    
                    while ((line = in.readLine()) != null) {
                        responseData.append(line);
                        out.println(line);
                    }
                }

                inputStream.close();


            } else {
                /*
                   If the file doesn't exist or it is unreadable we don't care
                */
            }

        } catch (Exception e) {
                /*
                   If the file doesn't exist or it is unreadable we don't care
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

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
