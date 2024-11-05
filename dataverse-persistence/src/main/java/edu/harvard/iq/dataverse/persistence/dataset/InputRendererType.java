package edu.harvard.iq.dataverse.persistence.dataset;

/**
 * Enum specifying what component should be used
 * for rendering metadata field when user
 * edits metadata.
 * 
 * @author madryk
 */
public enum InputRendererType {

    /**
     * Input field will be rendered as simple input
     * with type text
     */
    TEXT,
    /**
     * Input field will be rendered as autocomplete component
     * with text input area
     */
    SUGGESTION_TEXT,
    /**
     * Input field will be rendered as textarea
     */
    TEXTBOX,
    /**
     * Input field will be rendered as textarea
     * with possibility to enter some html tags
     */
    HTML_MARKUP,
    /**
     * Input field will be rendered as select
     * one option from list when ({@link DatasetFieldType#isAllowMultiples()}
     * is false or list of checkboxes when
     * ({@link DatasetFieldType#isAllowMultiples()} is true.
     * Should be used only for field with controlled vocabulary.
     */
    VOCABULARY_SELECT,
    /**
     * Input field will be rendered as autocomplete component
     * with text input for backend filtering of values.
     * Supports drop down for selection.
     * Lazy loading at the end of scrolling with last element "Load more".
     * Supports multiple value.
     * Dedicated to work with controlled vocabulary values.
     */
    VOCABULARY_ENHANCED_SELECT,
    /**
     * Input field will be rendered as hidden
     * checkboxes with auto selected all
     * values defined in renderer options
     */
    HIDDEN_VOCABULARY
}
