/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.customization;

import java.util.List;

/**
 *
 * @author rmp553
 */
public class CustomizationConstants {
 
    public static String fileTypeHomePage = "homePage";

    public static String fileTypeHeader = "header";

    public static String fileTypeFooter = "footer";

    public static String fileTypeStyle = "style";

    public static String fileTypeAnalytics = "analytics";

    public static String fileTypeLogo = "logo";

    public static List<String> validTypes = List.of(fileTypeHomePage, fileTypeHeader, fileTypeFooter, fileTypeStyle, fileTypeAnalytics, fileTypeLogo);
} // end CustomizationConstants
