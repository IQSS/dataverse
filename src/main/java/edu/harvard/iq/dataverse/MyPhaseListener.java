/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;
import java.util.Locale; 
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author dataverse
 */
@SuppressWarnings("serial")
public class MyPhaseListener implements PhaseListener {
         /**
          * 开始某个阶段时会调用到的方法
          */
         public void beforePhase(PhaseEvent pe) {
         }
 
         /**
          * 结束某个阶段时会调用到的方法
          */
         public void afterPhase(PhaseEvent pe) {
                   System.out.println("this is Phase: " + pe.getPhaseId().toString());
                   // 设置自选语言环境
                   HttpServletRequest request = (HttpServletRequest) FacesContext
                                     .getCurrentInstance().getExternalContext().getRequest();
 
                   Locale myLocale = (Locale) request.getSession().getAttribute("myLocale");
                   System.out.println("current request locale is: "
                                     + FacesContext.getCurrentInstance().getViewRoot().getLocale()
                                                        .toString());
 
                   if (myLocale != null) {
                            FacesContext.getCurrentInstance().getViewRoot().setLocale(myLocale);
 
                            System.out.println("current Custom locale is: "
                                               + FacesContext.getCurrentInstance().getViewRoot()
                                                                 .getLocale().toString());
                   }
         }
 
         /**
          * 只监听"恢复视图阶段"
          */
         public PhaseId getPhaseId() {
                   return PhaseId.RESTORE_VIEW;
         }
}
