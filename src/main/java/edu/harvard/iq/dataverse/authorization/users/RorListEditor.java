package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds a user's ordered list of RORs while it's being edited in the UI (see
 * rorListEditor.xhtml). Nothing is saved until the page's save action passes
 * {@link #getRorIds()} to AuthenticationServiceBean.setRors().
 */
public class RorListEditor implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> rorIds;
    private String newRorId;
    private String errorMessage;

    public RorListEditor() {
        this(Collections.emptyList());
    }

    /**
     * @param rorIds the user's current ROR URLs, in order
     */
    public RorListEditor(List<String> rorIds) {
        this.rorIds = new ArrayList<>(rorIds);
    }

    /**
     * @return the ROR URLs in their current order; the first is the primary ROR
     */
    public List<String> getRorIds() {
        return Collections.unmodifiableList(rorIds);
    }

    public String getNewRorId() {
        return newRorId;
    }

    public void setNewRorId(String newRorId) {
        this.newRorId = newRorId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Action for the "Add" button.
     */
    public void add() {
        addNewRor();
    }

    /**
     * Appends whatever is in the "new ROR" field, if anything. Pages call this
     * on save too, so a ROR that was typed but not explicitly added isn't lost.
     *
     * @return false if there was input that couldn't be added (the error
     *         message is set); true otherwise
     */
    public boolean addNewRor() {
        errorMessage = null;
        if (newRorId == null || newRorId.isBlank()) {
            return true;
        }
        String normalized = AuthenticatedUserRor.normalizeRorId(newRorId);
        if (normalized == null) {
            errorMessage = BundleUtil.getStringFromBundle("user.rors.error.invalid", List.of(newRorId.trim()));
            return false;
        }
        if (rorIds.contains(normalized)) {
            errorMessage = BundleUtil.getStringFromBundle("user.rors.error.duplicate", List.of(normalized));
            return false;
        }
        rorIds.add(normalized);
        newRorId = null;
        return true;
    }

    public void remove(int index) {
        if (index >= 0 && index < rorIds.size()) {
            rorIds.remove(index);
        }
        errorMessage = null;
    }

    public void moveUp(int index) {
        if (index > 0 && index < rorIds.size()) {
            Collections.swap(rorIds, index, index - 1);
        }
        errorMessage = null;
    }

    public void moveDown(int index) {
        if (index >= 0 && index < rorIds.size() - 1) {
            Collections.swap(rorIds, index, index + 1);
        }
        errorMessage = null;
    }
}
