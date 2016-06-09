/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * API endpoint that returns JSON representations of particular database settings.
 * @author jackson
 */

@Path("system")
public class Sys extends AbstractApiBean{

    @EJB
    SettingsServiceBean settingsService;
    
        @GET
        @Path("settings/:DatasetPublishPopupCustomText")
    public Response getDatasetPublishPopupCustomText(){
           
        String setting = settingsService.get("DatasetPublishPopupCustomText");

        JsonObjectBuilder response = Json.createObjectBuilder();
        
        if(setting != null){
            return okResponse(response.add("DatasetPublishPopupCustomText", setting));
        } else {
            return notFound("Setting DatasetPublishPopupCustomText not found");
        }
    }
 
}