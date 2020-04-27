package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;

public class VocabSelectInputFieldRenderer implements InputFieldRenderer {

    /**
     * Sort vocabulary values list in localized labels order 
     */
	private boolean sortByLocalisedStringsOrder = false;

	public VocabSelectInputFieldRenderer() {
	}
	
	
	public VocabSelectInputFieldRenderer(boolean sortByLocalisedStringsOrder) {
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
     * This implementation always returns {@code true}
     */
    @Override
    public boolean renderInTwoColumns() {
        return true;
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
