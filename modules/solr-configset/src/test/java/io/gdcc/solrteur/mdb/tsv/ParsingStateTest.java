package io.gdcc.solrteur.mdb.tsv;

import io.gdcc.solrteur.mdb.tsv.Configuration;
import io.gdcc.solrteur.mdb.tsv.Block;
import io.gdcc.solrteur.mdb.tsv.ControlledVocabulary;
import io.gdcc.solrteur.mdb.tsv.Field;
import io.gdcc.solrteur.mdb.tsv.ParserException;
import io.gdcc.solrteur.mdb.tsv.ParsingState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ParsingStateTest {
    
    static Configuration config = Configuration.defaultConfig();

    static Stream<Arguments> failingStateTransitionExamples() {
        return Stream.of(
            Arguments.of(ParsingState.Init, null),
            Arguments.of(ParsingState.MetadataBlock, null),
            Arguments.of(ParsingState.Fields, null),
            Arguments.of(ParsingState.Vocabularies, null),
    
            Arguments.of(ParsingState.Init, ""),
            Arguments.of(ParsingState.MetadataBlock, ""),
            Arguments.of(ParsingState.Fields, ""),
            Arguments.of(ParsingState.Vocabularies, ""),
    
            Arguments.of(ParsingState.Init, "foobar"),
            Arguments.of(ParsingState.MetadataBlock, "foobar"),
            Arguments.of(ParsingState.Fields, "foobar"),
            Arguments.of(ParsingState.Vocabularies, "foobar"),
    
            Arguments.of(ParsingState.Init, config.triggerIndicator()),
            Arguments.of(ParsingState.Init, config.commentIndicator()),
            Arguments.of(ParsingState.Init, config.columnSeparator()),
            
            Arguments.of(ParsingState.Init, config.trigger(Field.KEYWORD)),
            Arguments.of(ParsingState.Init, config.trigger(ControlledVocabulary.KEYWORD)),
    
            Arguments.of(ParsingState.MetadataBlock, config.commentIndicator()),
            Arguments.of(ParsingState.MetadataBlock, config.trigger(ControlledVocabulary.KEYWORD)),
    
            Arguments.of(ParsingState.Fields, config.commentIndicator()),
            Arguments.of(ParsingState.Fields, config.trigger(Block.KEYWORD)),
    
            Arguments.of(ParsingState.Vocabularies, config.commentIndicator()),
            Arguments.of(ParsingState.Vocabularies, config.trigger(Block.KEYWORD)),
            Arguments.of(ParsingState.Vocabularies, config.trigger(Field.KEYWORD))
        );
    }
    
    @ParameterizedTest
    @MethodSource("failingStateTransitionExamples")
    void failingTransitions(final ParsingState source, final String triggerLine) throws ParserException {
        ParserException ex = assertThrows(ParserException.class, () -> source.transitionState(triggerLine, config));
    }
    
    static Stream<Arguments> successfulStateTransitionExamples() {
        return Stream.of(
            Arguments.of(ParsingState.Init, config.trigger(Block.KEYWORD), ParsingState.MetadataBlock),
            Arguments.of(ParsingState.MetadataBlock, config.trigger(Field.KEYWORD), ParsingState.Fields),
            Arguments.of(ParsingState.Fields, config.trigger(ControlledVocabulary.KEYWORD), ParsingState.Vocabularies)
        );
    }
    
    @ParameterizedTest
    @MethodSource("successfulStateTransitionExamples")
    void successfulTransitions(final ParsingState source, final String triggerLine, final ParsingState expected) throws ParserException {
        assertEquals(expected, source.transitionState(triggerLine, config));
    }

}