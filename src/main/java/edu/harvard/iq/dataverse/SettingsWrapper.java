/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.security.AccessController.getContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.ServletContext;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class SettingsWrapper implements java.io.Serializable {

    @EJB
    SettingsServiceBean settingService;

    private Map<String, String> settingsMap;
    
    private static final Logger logger = Logger.getLogger(SettingsWrapper.class.getCanonicalName());

  
    
    /**
     * Values that are considered as "true".
     * @see #isTrue(java.lang.String, boolean) 
     */
    private static final Set<String> TRUE_VALUES = Collections.unmodifiableSet(
            new TreeSet<>( Arrays.asList("1","yes", "true","allow")));

    public String get(String settingKey) {
        if (settingsMap == null) {
            initSettingsMap();
        }
        
        return settingsMap.get(settingKey);
    }
    
    public boolean isTrueForKey(String settingKey, boolean safeDefaultIfKeyNotFound) {
        if (settingsMap == null) {
            initSettingsMap();
        }
        
        String val = settingsMap.get(settingKey);;
        return ( val==null ) ? safeDefaultIfKeyNotFound : TRUE_VALUES.contains(val.trim().toLowerCase() );
    }

    private void initSettingsMap() {
        // initialize settings map
        settingsMap = new HashMap<>();
        for (Setting setting : settingService.listAll()) {
            settingsMap.put(setting.getName(), setting.getContent());
        }
    }

    private String guidesBaseUrl = null; 
    
    public String getGuidesBaseUrl() {
        if (guidesBaseUrl == null) {
            String saneDefault = "http://guides.dataverse.org";
        
            guidesBaseUrl = get(":GuidesBaseUrl");
            if (guidesBaseUrl == null) {
                guidesBaseUrl = saneDefault + "/en"; 
            } else {
                guidesBaseUrl = guidesBaseUrl + "/en";
            }
            // TODO: 
            // hard-coded "en"; will need to be configuratble once 
            // we have support for other languages. 
            // TODO: 
            // remove a duplicate of this method from SystemConfig
        }
        return guidesBaseUrl;
    }
    
 
    
     private static String readLocalResource(String path) {
        
        logger.fine(String.format("Data Frame Service: readLocalResource: reading local path \"%s\"", path));

        // Get stream
        InputStream resourceStream = SettingsWrapper.class.getResourceAsStream(path);
        String resourceAsString = "";

        // Try opening a buffered reader stream
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(resourceStream, "UTF-8"));

            String line = null;
            while ((line = rd.readLine()) != null) {
                resourceAsString = resourceAsString.concat(line + "\n");
            }
            resourceStream.close();
        } catch (IOException ex) {
            logger.warning(String.format("SettingsWrapper: (readLocalResource) resource stream from path \"%s\" was invalid", path));
        }

        // Return string
        return resourceAsString;
    }
    
    public String getMustacheMessage() throws IOException{

        // -----------------------------
        // Load the Data  from the database
        // -----------------------------
        String data = this.get(":GuidesData");

        logger.info("getMustacheMessage data loaded: " + data);
        // -----------------------------
        // Load the Template from the database       
        // -----------------------------
        String templateText = this.get(":GuidesTemplate");//"Go to <a href=\"{{guides}}\">{{guides}}</a>!";
        //String templateText = "Go to <a href=\"{{link}}\">{{subject}}</a>!";
        logger.info("getMustacheMessage db template: " + templateText);
        
        // Load from a file
        templateText = readLocalResource("navbar/guides.html");
                
        
        // -----------------------------
        // Convert the JSON Data to a Map
        // -----------------------------    
        Gson gson = new Gson();
        java.lang.reflect.Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> map = gson.fromJson(data, type);   

        // -----------------------------
        // Render the template
        // -----------------------------                
        //templateText = readResourceFile();
        logger.info("getMustacheMessage try file template: ??" + templateText );

        // Inline compiliation
        Handlebars handlebars = new Handlebars();
        com.github.jknack.handlebars.Template template = handlebars.compileInline(templateText);
               
        
        Context context = Context.newBuilder(map).build();
        return template.apply(context); 
        
    }
    
}

/*
--------------
Example data
--------------
{
    "guide_links": [
        {
            "name": "Ye User Guide",
            "link": "http://guides.dataverse.org/en/latest/user/"
        },
        {
            "name": "Developer Guide",
            "link": "http://guides.dataverse.org/en/latest/developers/"
        },
        {
            "name": "Installation Guide",
            "link": "http://guides.dataverse.org/en/latest/installation/"
        },
        {
            "name": "The Old API Guide",
            "link": "http://guides.dataverse.org/en/latest/api/"
        }

    ]
}

--------------
Example template
--------------
<ul class="dropdown-menu">                            
  {{#if guide_links}}  
      {{#each guide_links}}
        <li><a href="{{this.link}}" target="_blank">{{this.name}}</a></li>
      {{/each}}
  {{else}}
        <li>(no guides)</li>
  {{/if}}
</ul>

*/