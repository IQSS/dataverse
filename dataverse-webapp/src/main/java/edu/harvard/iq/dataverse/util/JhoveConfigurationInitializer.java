package edu.harvard.iq.dataverse.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Initializer of jhove configuration files.
 * 
 * Classes that uses jhove should assume that
 * configuration file for jhove is located in
 * {@link #JHOVE_CONFIG_PATH}.
 */
@Startup
@Singleton
public class JhoveConfigurationInitializer {

    private static final String JHOVE_CONFIG_CLASSPATH = "jhove/jhove.conf";
    private static final String JHOVE_CONFIG_XSD_CLASSPATH = "jhove/jhoveConfig.xsd";

    public static final String JHOVE_CONFIG_PATH = System.getProperty("java.io.tmpdir") + "/jhove.conf";
    public static final String JHOVE_CONFIG_XSD_PATH = System.getProperty("java.io.tmpdir") + "/jhoveConfig.xsd";

    /**
     * Copies jhove configuration and configuration xsd files from
     * classpath into temp directory. In case of configuration
     * file - it also replaces xsd location placeholder to
     * the path in temp directory where it was copied.
     * 
     * Replacing of schema location is done, because jhove will
     * validate given configuration file against it.
     */
    @PostConstruct
    public void initializeJhoveConfig() {
        try {
            Files.copy(Paths.get(getJhoveConfigXsdFile()), Paths.get(JHOVE_CONFIG_XSD_PATH), StandardCopyOption.REPLACE_EXISTING);
        
            String jhoveConf = FileUtils.readFileToString(new File(getJhoveConfigFile()), StandardCharsets.UTF_8);
            jhoveConf = StringUtils.replaceOnce(jhoveConf, "{jhove.config.xsd.path}", "file://" + JHOVE_CONFIG_XSD_PATH);
            FileUtils.writeStringToFile(new File(JHOVE_CONFIG_PATH), jhoveConf, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to prepare jhove configuration files", e);
        }
    }
    
    // -------------------- PRIVATE --------------------
    
    private String getJhoveConfigFile() {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(JHOVE_CONFIG_CLASSPATH).getPath();
    }
    
    private String getJhoveConfigXsdFile() {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(JHOVE_CONFIG_XSD_CLASSPATH).getPath();
    }
}
