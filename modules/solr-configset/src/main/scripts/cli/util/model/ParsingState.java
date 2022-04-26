package cli.util.model;

import cli.util.TsvBlockReader;

public enum ParsingState {
    Vocabularies(ControlledVocabulary.TRIGGER),
    Fields(Field.TRIGGER, Vocabularies),
    MetadataBlock(Block.TRIGGER, Fields),
    // This state is only used exactly once and should never be reached from input.
    // For safety, make the validation fail.
    Init(Constants.COMMENT_INDICATOR, MetadataBlock);
    
    private final String stateTrigger;
    private final ParsingState nextState;
    
    ParsingState(String trigger, ParsingState next) {
        this.stateTrigger = trigger;
        this.nextState = next;
    }
    
    /**
     * Create final state (no next step)
     * @param trigger
     */
    ParsingState(String trigger) {
        this.stateTrigger = trigger;
        this.nextState = this;
    }
    
    public boolean isAllowedFinalState() {
        return this == Fields || this == Vocabularies;
    }
    
    public ParsingState transitionState(String headerLine) throws ParserException {
        // if not null, not starting the same state again (no loops allowed) and starting the correct next state, return the next state
        if(headerLine != null && ! headerLine.startsWith(this.stateTrigger) &&
           headerLine.startsWith(this.nextState.stateTrigger)) {
            return this.nextState;
        }
        // otherwise throw a parsing exception
        throw new ParserException("Invalid header '" +
            (headerLine == null ? "null" : headerLine.substring(0, Math.min(25, headerLine.length()))) +
            "...' while in section '" + this.stateTrigger + "'");
    }
}
