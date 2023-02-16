package io.gdcc.solrteur.mdb.tsv;

public enum ParsingState {
    Vocabularies(ControlledVocabulary.KEYWORD),
    Fields(Field.KEYWORD, Vocabularies),
    MetadataBlock(Block.KEYWORD, Fields),
    // This state is only used exactly once and should never be reached from input.
    // For safety, make the validation fail.
    Init(null, MetadataBlock);
    
    private final String stateKeyword;
    private final ParsingState nextState;
    
    ParsingState(String keyword, ParsingState next) {
        this.stateKeyword = keyword;
        this.nextState = next;
    }
    
    /**
     * Create final state (no next step)
     * @param trigger
     */
    ParsingState(String keyword) {
        this.stateKeyword = keyword;
        this.nextState = this;
    }
    
    public boolean isAllowedFinalState() {
        return this == Fields || this == Vocabularies;
    }
    
    /**
     * Return the next state if the given line is triggering the change correctly.
     * Wrong header lines will trigger exceptions.
     *
     * @param headerLine The line to analyse for switching to the next parsing state
     * @param config The parser configuration
     * @return The new state (will throw if line not valid)
     * @throws ParserException When the given line wasn't the expected one to transition the state
     */
    public ParsingState transitionState(String headerLine, Configuration config) throws ParserException {
        // if not null, not starting the same state again (no loops allowed) and starting the correct next state, return the next state
        if(headerLine != null &&
           ! headerLine.startsWith(config.trigger(this.stateKeyword)) &&
           headerLine.startsWith(config.trigger(this.nextState.stateKeyword))) {
            return this.nextState;
        }
        // otherwise, throw a parsing exception
        throw new ParserException("Found invalid header '" +
            (headerLine == null ? "null" : headerLine.substring(0, Math.min(25, headerLine.length()))) +
            "...' while " +
            (this.stateKeyword == null ? "initializing." : "in section '" + this.stateKeyword + "'."));
    }
}
