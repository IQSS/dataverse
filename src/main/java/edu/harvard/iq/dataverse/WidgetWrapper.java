/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 *
 * @author gdurand
 */

@ViewScoped
@Named
public class WidgetWrapper implements java.io.Serializable {
    
    private Boolean widgetView;
    private String widgetScope;

    public boolean isWidgetView() {
        if (widgetView == null) {
            String widgetParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("widgetScope");
            widgetView = widgetParam != null;
        }
        return widgetView;
    }
    
    public String getWidgetScope() {
        // first check for widgetScope; if not found use alias (if null then this is not a dataverse widget)
        if (widgetScope == null) {
            String widgetScopeParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("widgetScope");
            if (widgetScopeParam != null) {
                widgetScope = widgetScopeParam;
            } else {
                String aliasParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("alias");
                widgetScope = aliasParam;
            }
        }
        return widgetScope;
    }

    
    public String wrapURL(String URL) {
        return URL + (isWidgetView() ? getParamSeparator(URL) + "widgetScope=" + getWidgetScope() : "");
    }
    
    private String getParamSeparator(String URL) {
        return (URL.contains("?") ? "&" : "?");
    }
    
    public boolean inWidgetScope(Dataverse dv) {
        if (isWidgetView()) {
            while (dv.getOwner() != null) {
                if (dv.getAlias().equals(getWidgetScope())) {
                   return true;
                }
                dv= dv.getOwner();
            }
        }
        
        return false;
    }    
    
}