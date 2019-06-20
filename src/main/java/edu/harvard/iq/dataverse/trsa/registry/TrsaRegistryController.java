package edu.harvard.iq.dataverse.trsa.registry;

import edu.harvard.iq.dataverse.trsa.Trsa;
import edu.harvard.iq.dataverse.trsa.registry.util.JsfUtil;
import edu.harvard.iq.dataverse.trsa.registry.util.JsfUtil.PersistAction;

import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@Named("trsaRegistryController")
@SessionScoped
public class TrsaRegistryController implements Serializable {

    @EJB
    private edu.harvard.iq.dataverse.trsa.registry.TrsaRegistryFacade ejbFacade;
    private List<Trsa> items = null;
    private Trsa selected;

    public TrsaRegistryController() {
    }

    public Trsa getSelected() {
        return selected;
    }

    public void setSelected(Trsa selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private TrsaRegistryFacade getFacade() {
        return ejbFacade;
    }

    public Trsa prepareCreate() {
        selected = new Trsa();
        initializeEmbeddableKey();
        return selected;
    }

    public void create() {
        persist(PersistAction.CREATE, ResourceBundle.getBundle("/Bundle_trsa_registry").getString("TrsaRegistryCreated"));
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, ResourceBundle.getBundle("/Bundle_trsa_registry").getString("TrsaRegistryUpdated"));
    }

    public void destroy() {
        persist(PersistAction.DELETE, ResourceBundle.getBundle("/Bundle_trsa_registry").getString("TrsaRegistryDeleted"));
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<Trsa> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    getFacade().remove(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle_trsa_registry").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle_trsa_registry").getString("PersistenceErrorOccured"));
            }
        }
    }

    public Trsa getTrsaRegistry(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<Trsa> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<Trsa> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    @FacesConverter(forClass = Trsa.class)
    public static class TrsaRegistryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TrsaRegistryController controller = (TrsaRegistryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "trsaRegistryController");
            return controller.getTrsaRegistry(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Trsa) {
                Trsa o = (Trsa) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, 
                    "object {0} is of type {1}; expected type: {2}", 
                    new Object[]{object, object.getClass().getName(), Trsa.class.getName()});
                return null;
            }
        }

    }

}
