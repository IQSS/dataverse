package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.common.BundleUtil;
import org.primefaces.PrimeFaces;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for common JSF tasks.
 *
 * @author michael
 */
public class JsfHelper {
    private static final Logger logger = Logger.getLogger(JsfHelper.class.getName());


    public static void addFlashSuccessMessage(String message) {
        addMessage("successMessage", new FacesMessage(BundleUtil.getStringFromBundle("messages.success"), message), true);
    }
    public static void addSuccessMessage(String message) {
        addMessage("successMessage", new FacesMessage(BundleUtil.getStringFromBundle("messages.success"), message), false);
    }


    public static void addFlashErrorMessage(String message) {
        addFlashErrorMessage(BundleUtil.getStringFromBundle("messages.error"), message);
    }
    public static void addFlashErrorMessage(String message, String detail) {
        addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, detail), true);
    }

    public static void addErrorMessage(String message) {
        addErrorMessage(BundleUtil.getStringFromBundle("messages.error"), message);
    }
    public static void addErrorMessage(String message, String detail) {
        PrimeFaces.current().ajax().addCallbackParam("hasErrorMessage", true);

        addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, detail), false);
    }


    public static void addFlashWarningMessage(String detail) {
        addFlashWarningMessage(BundleUtil.getStringFromBundle("messages.info"), detail);
    }
    public static void addFlashWarningMessage(String message, String detail) {
        addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, message, detail), true);
    }


    public static void addWarningMessage(String message, String detail) {
        addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, message, detail), false);
    }


    public static void addInfoMessage(String detail) {
        addInfoMessage(BundleUtil.getStringFromBundle("messages.info"), detail);
    }
    public static void addInfoMessage(String message, String detail) {
        addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, message, detail), false);
    }


    public static void addComponentErrorMessage(String componentId, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(componentId,
                                                     new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    public static HttpSession getCurrentSession() {
        return (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
    }

    public <T extends Enum<T>> T enumValue(String param, Class<T> enmClass, T defaultValue) {
        if (param == null) {
            return defaultValue;
        }
        param = param.trim();
        try {
            return Enum.valueOf(enmClass, param);
        } catch (IllegalArgumentException iar) {
            logger.log(Level.WARNING, "Illegal value for enum {0}: ''{1}''", new Object[]{enmClass.getName(), param});
            return defaultValue;
        }
    }

    public static Optional<UIComponent> findComponent(Optional<UIComponent> root, String id, BiPredicate<String, String> idChecker) {
        if (!root.isPresent()) {
            return Optional.empty();
        }

        UIComponent rootComponent = root.get();
        if (idChecker.test(rootComponent.getClientId(), id)) {
            return root;
        }
        List<UIComponent> children = rootComponent.getChildren();
        if (children == null || children.isEmpty()) {
            return Optional.empty();
        }
        for (UIComponent child : children) {
            Optional<UIComponent> result = findComponent(Optional.ofNullable(child), id, idChecker);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    // -------------------- PRIVATE --------------------

    private static void addMessage(String componentId, FacesMessage facesMessage, boolean isFlash) {
        if (isFlash) {
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
        }
        FacesContext.getCurrentInstance().addMessage(componentId, facesMessage);
    }
}
