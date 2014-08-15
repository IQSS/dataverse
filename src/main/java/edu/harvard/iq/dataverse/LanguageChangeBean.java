/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map; 
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author FudanLibrary
 */
@Named
public class LanguageChangeBean {
    private String currentLocale;
 
         // 本应用支持的语言Map
         private Map<String, Locale> supportLocals;
 
         public LanguageChangeBean() {
                   supportLocals = new HashMap<String, Locale>();
                   supportLocals.put("zh_CN", Locale.CHINA);
                   supportLocals.put("zh_TW", Locale.TAIWAN);
                   supportLocals.put("en", Locale.ENGLISH);
 
                   this.currentLocale = FacesContext.getCurrentInstance().getViewRoot()
                                     .getLocale().toString();
         }
 
         public String getCurrentLocale() {
                   return currentLocale;
         }
 
         public void setCurrentLocale(String currentLocale) {
                   this.currentLocale = currentLocale;
         }
 
         // 改变当前语言区域的事件处理方法
         public void changeLocale(ValueChangeEvent event) {
                   String currentLocale = (String) event.getNewValue();
                   // 设置当前的语言区域
                   Locale myLocale = this.supportLocals.get(currentLocale);
                   FacesContext.getCurrentInstance().getViewRoot().setLocale(myLocale);
 
                   // 把自定义语言存放到Session中
                   HttpServletRequest request = (HttpServletRequest) FacesContext
                                     .getCurrentInstance().getExternalContext().getRequest();
                   request.getSession().setAttribute("myLocale", myLocale);
         }
}
