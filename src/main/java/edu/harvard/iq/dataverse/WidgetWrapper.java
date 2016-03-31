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

    public Boolean getWidgetView() {
        if (widgetView == null) {
            String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("widgetView");
            setWidgetView(param!=null && param.equals("true"));
        }
        return widgetView;
    }

    public void setWidgetView(Boolean widgetView) {
        this.widgetView = widgetView;
    }
    
}