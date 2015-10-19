/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    public String get(String settingKey) {
        if (settingsMap == null) {
            // initialize settings map
            settingsMap = new HashMap<>();
            for (Setting setting : settingService.listAll()) {
                settingsMap.put(setting.getName(),setting.getContent());
            }
        }
        
        
        return settingsMap.get(settingKey);
    }



}

