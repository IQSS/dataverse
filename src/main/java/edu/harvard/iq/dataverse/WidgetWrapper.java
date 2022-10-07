/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class WidgetWrapper implements java.io.Serializable {

    private final static String WIDGET_PARAMETER = "widget";
    private final static char WIDGET_SEPARATOR = '@';
    
    private Boolean widgetView;
    private String widgetHome;
    private String widgetScope;

    private boolean initWidget() {
        // first check for widgetScope; if not found use alias (if null then this is not a dataverse widget)
        if (widgetView == null) {
            String widgetParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(WIDGET_PARAMETER);
            // you are in widget view ONLY if this param is supplied AND you have the separator 
            widgetView = widgetParam != null && widgetParam.indexOf(WIDGET_SEPARATOR) != -1;
                     
            if (widgetView) {
                widgetScope = widgetParam.substring(0, widgetParam.indexOf(WIDGET_SEPARATOR));
                widgetHome = widgetParam.substring(widgetParam.indexOf(WIDGET_SEPARATOR) + 1);
            }
        }
        if (!widgetView) {
            // Prevent non-widgets from being embedded in iframes.
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            response.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
        }
        return widgetView;
    }

    public boolean isWidgetView() {
        return initWidget();
    }

    public boolean isWidgetTarget(DvObject dvo) {
        if (isWidgetView()) {
                       
            while (dvo != null) {
                if (dvo instanceof DataFile) {
                    if ("datafile".equals(widgetScope)) {
                        //todo: add logic for when we add file widgets
                    }
                } else if (dvo instanceof Dataset) {
                    switch (widgetScope) {
                        case "dataverse": 
                            break; // keep looping
                        case "dataset":
                            if (((Dataset) dvo).getGlobalId().asString().equals(widgetHome)) {
                                return true;
                            }   break;
                        default:
                            return false; // scope is for lower type dvObject
                    }
                } else if (dvo instanceof Dataverse) {
                    if ("dataverse".equals(widgetScope)) {
                        if (((Dataverse) dvo).getAlias().equals(widgetHome)) {
                            return true;
                        }
                    } else {
                        return false; // scope is for lower type dvObject
                    }
                }
                
                dvo = dvo.getOwner();                
            }
        }

        return false;
    }

    public String wrapURL(String URL) {
        return URL + (isWidgetView() ? getParamSeparator(URL) + WIDGET_PARAMETER + "=" + widgetScope + WIDGET_SEPARATOR + widgetHome: "");
    }

    private String getParamSeparator(String URL) {
        return (URL.contains("?") ? "&" : "?");
    }

}
