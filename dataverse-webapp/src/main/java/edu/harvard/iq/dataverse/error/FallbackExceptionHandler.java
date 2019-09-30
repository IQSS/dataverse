package edu.harvard.iq.dataverse.error;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;

import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class designed for handling exception that were not handled explicitly.
 * <p>
 * If request was async(Ajax) banner is going to be displayed, otherwise user is redirected to 500.xhtml.
 */
public class FallbackExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger logger = Logger.getLogger(FallbackExceptionHandler.class.getName());

    private ExceptionHandler exceptionHandler;

    public FallbackExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return exceptionHandler;
    }

    @Override
    public void handle() throws FacesException {
        final Iterator<ExceptionQueuedEvent> queue = getUnhandledExceptionQueuedEvents().iterator();

        if (queue.hasNext()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();

            if (facesContext.getPartialViewContext().isAjaxRequest()) {
                JsfHelper.addErrorMessage(null, BundleUtil.getStringFromBundle("error.general.message"), "");
            } else {
                Try.run(() -> facesContext.getExternalContext()
                        .responseSendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null))
                        .onFailure(throwable -> logger.log(Level.SEVERE, throwable.getMessage(), throwable));
            }
        }

        while (queue.hasNext()) {
            queue.remove();
        }
    }
}
