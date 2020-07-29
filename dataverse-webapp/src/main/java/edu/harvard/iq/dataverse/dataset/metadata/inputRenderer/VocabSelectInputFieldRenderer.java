package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

public class VocabSelectInputFieldRenderer implements InputFieldRenderer {

    private boolean renderInTwoColumns = true;

    /**
     * Sort vocabulary values list in localized labels order 
     */
    private boolean sortByLocalisedStringsOrder = false;

    // -------------------- CONSTRUCTORS --------------------

    public VocabSelectInputFieldRenderer(boolean renderInTwoColumns, boolean sortByLocalisedStringsOrder) {
        this.renderInTwoColumns = renderInTwoColumns;
        this.sortByLocalisedStringsOrder = sortByLocalisedStringsOrder;
    }

    // -------------------- GETTERS --------------------

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link InputRendererType#VOCABULARY_SELECT}
     */
    @Override
    public InputRendererType getType() {
        return InputRendererType.VOCABULARY_SELECT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns value provided in constructor
     */
    @Override
    public boolean renderInTwoColumns() {
        return renderInTwoColumns;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@code false}
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    public boolean isSortByLocalisedStringsOrder() {
        return sortByLocalisedStringsOrder;
    }
}
