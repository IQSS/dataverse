/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

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

    
    public String getMustacheMessage() throws IOException{
        
        //String data = "{\"subject\": \"World\", \"link\": \"http://www.dataverse.org\"}";
        String data = this.get(":GuidesData");
        String templateText = this.get(":GuidesTemplate");//"Go to <a href=\"{{guides}}\">{{guides}}</a>!";
        //String templateText = "Go to <a href=\"{{link}}\">{{subject}}</a>!";
        
        Handlebars handlebars = new Handlebars();
        Gson gson = new Gson();

        java.lang.reflect.Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> map = gson.fromJson(data, type);   
        //logger.info("guide links: " + map.size());

        com.github.jknack.handlebars.Template template = handlebars.compileInline(templateText);
        Context context = Context.newBuilder(map).build();

        //com.github.jknack.handlebars.Template template = handlebars.compileInline("<a href='javascript:alert(\"{{this}}\");'>{{this}}</a>");
        //String resultString = template.apply("guides.org");

        return template.apply(context); // + " (ok)";
        
        /*
        {
    "guide_links": [
        {
            "name": "User Guide",
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
            "name": "API Guide",
            "link": "http://guides.dataverse.org/en/latest/api/"
        }

    ]
}
        
        
        
        <ul class="people_list">
  {{#if guide_links}}
      {{#each guide_links}}
        <li><a href="{{this.link}}" target="_blank">{{this.name}}</a></li>
      {{/each}}
  {{else}}
        (no guides)
  {{/if}}
</ul>
        */
    }
    
}

