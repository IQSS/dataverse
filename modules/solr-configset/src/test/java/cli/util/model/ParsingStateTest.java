package cli.util.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ParsingStateTest {

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
    
            Arguments.of(ParsingState.Init, Constants.TRIGGER_INDICATOR),
            Arguments.of(ParsingState.Init, Constants.COMMENT_INDICATOR),
            Arguments.of(ParsingState.Init, Constants.COLUMN_SEPARATOR),
            
            Arguments.of(ParsingState.Init, Field.TRIGGER),
            Arguments.of(ParsingState.Init, ControlledVocabulary.TRIGGER),
    
            Arguments.of(ParsingState.MetadataBlock, Constants.COMMENT_INDICATOR),
            Arguments.of(ParsingState.MetadataBlock, ControlledVocabulary.TRIGGER),
    
            Arguments.of(ParsingState.Fields, Constants.COMMENT_INDICATOR),
            Arguments.of(ParsingState.Fields, Block.TRIGGER),
    
            Arguments.of(ParsingState.Vocabularies, Constants.COMMENT_INDICATOR),
            Arguments.of(ParsingState.Vocabularies, Block.TRIGGER),
            Arguments.of(ParsingState.Vocabularies, Field.TRIGGER)
        );
    }
    
    @ParameterizedTest
    @MethodSource("failingStateTransitionExamples")
    void failingTransitions(ParsingState source, String triggerLine) throws ParserException {
        ParserException ex = assertThrows(ParserException.class, () -> source.transitionState(triggerLine));
    }
    
    static Stream<Arguments> successfulStateTransitionExamples() {
        return Stream.of(
            Arguments.of(ParsingState.Init, Block.TRIGGER, ParsingState.MetadataBlock),
            Arguments.of(ParsingState.MetadataBlock, Field.TRIGGER, ParsingState.Fields),
            Arguments.of(ParsingState.Fields, ControlledVocabulary.TRIGGER, ParsingState.Vocabularies)
        );
    }
    
    @ParameterizedTest
    @MethodSource("successfulStateTransitionExamples")
    void successfulTransitions(ParsingState source, String triggerLine, ParsingState expected) throws ParserException {
        assertEquals(expected, source.transitionState(triggerLine));
    }

}