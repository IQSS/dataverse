package io.gdcc.solrteur.mdb;

import io.gdcc.solrteur.mdb.tsv.Block;
import io.gdcc.solrteur.mdb.tsv.Configuration;
import io.gdcc.solrteur.mdb.tsv.Field;
import io.gdcc.solrteur.mdb.tsv.ParserException;
import io.gdcc.solrteur.mdb.tsv.ParsingState;

import javax.swing.text.html.parser.Parser;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataBlockTSVReader {
    
    private Configuration config;
    
    public MetadataBlockTSVReader() {
        this.config = Configuration.defaultConfig();
    }
    
    public MetadataBlockTSVReader(Configuration config) {
        this.config = config;
    }
    
    /**
     * Extract the metadata block definition from a single TSV file and return it.
     * @param lines
     * @return
     * @throws ParserException
     */
    public Block retrieveBlock(final List<String> lines) throws ParserException {
        // Assertions
        Objects.requireNonNull(lines, "You must provide a list of strings, it may be empty but not null");
        
        ParsingState state = ParsingState.Init;
        Block.BlockBuilder blockBuilder = null;
        ParserException parentException = new ParserException("Has errors:");
        int lineIndex = 0;
        
        for (String line : lines) {
            // Skip lines that are empty, blanks only or comments
            if (line.isBlank() || line.startsWith(config.commentIndicator())) {
                // Increment line counter before skipping to next line
                lineIndex++;
                continue;
            }
            
            // If the line starts with a new section trigger, analyse the state
            if (line.startsWith(config.triggerIndicator())) {
                state = state.transitionState(line, config);
                
                // Being here means we transitioned from one state to the next - otherwise there would have been an exception.
                switch (state) {
                    case MetadataBlock:
                        try {
                            blockBuilder = new Block.BlockBuilder(line, config);
                        } catch (ParserException e) {
                            // This is critical, as we cannot parse the following lines with a broken header
                            throw e.withLineNumber(lineIndex);
                        }
                        break;
                    case Fields:
                        Objects.requireNonNull(blockBuilder, "BlockBuilder not initialized, cannot build block");
    
                        // In case there had been parsing errors, stop here
                        if (parentException.hasSubExceptions()) {
                            throw parentException;
                        }
    
                        // We managed to complete parsing the block section, return the block now.
                        // The last line of the block section is the line before this one (which transitioned the state)
                        return blockBuilder.build(lineIndex-1);
                    default:
                        // Intentionally left blank, as the other sections are of no interest to us here.
                }
            } else {
                // Proceed analysis
                switch(state) {
                    case Init: throw new ParserException("Only comments, empty or blank lines allowed before block definition")
                        .withLineNumber(lineIndex);
                    case MetadataBlock:
                        Objects.requireNonNull(blockBuilder, "BlockBuilder not initialized, cannot parse");
                        try {
                            blockBuilder.parseAndValidateLine(line);
                        } catch (ParserException e) {
                            parentException.addSubException(e.withLineNumber(lineIndex));
                        }
                        break;
                    default:
                        throw new ParserException("We should never see this exception, as we looked for the block only")
                            .withLineNumber(lineIndex);
                }
            }
            
            // Increment line counter
            lineIndex++;
        }
        
        // The trigger switch did not kick in - only one explanation.
        throw new ParserException("Missing fields section.");
    }
    
    public List<Field> retrieveFields(final List<String> lines, final Set<Block> knownBlocks) throws ParserException {
        Objects.requireNonNull(lines, "You must provide a list of strings, it may be empty but not null");
        Objects.requireNonNull(knownBlocks, "You must provide a set of known blocks, it may be empty");
        
        // Read the block again, so we are at that stage and can continue with fields
        Block currentBlock = retrieveBlock(lines);
        ParsingState state = ParsingState.MetadataBlock;
        
        int lineIndex = currentBlock.getIndexLastLineofBlockSection()+1;
        List<String> linesAfterBlock = lines.stream()
            .skip(lineIndex)
            .collect(Collectors.toUnmodifiableList());
    
        Field.FieldsBuilder fieldsBuilder = null;
        ParserException parentException = new ParserException("Has errors:");
    
        for (String line : linesAfterBlock) {
            // Skip lines that are empty, blanks only or comments
            if (line.isBlank() || line.startsWith(config.commentIndicator())) {
                // Increment line counter
                lineIndex++;
                continue;
            }
        
            // If the line starts with a new section trigger, analyse the state
            if (line.startsWith(config.triggerIndicator())) {
                state = state.transitionState(line, config);
            
                // Being here means we transitioned from one state to the next - otherwise there would have been an exception.
                switch (state) {
                    case Fields:
                        try {
                            fieldsBuilder = new Field.FieldsBuilder(line, currentBlock.getName(), config);
                        } catch (ParserException e) {
                            // This is critical, as we cannot parse the following lines with a broken header
                            throw e.withLineNumber(lineIndex);
                        }
                        break;
                    case Vocabularies:
                        // We managed to get to the vocab section, meaning the fields are all done. Return fields!
                        Objects.requireNonNull(fieldsBuilder, "FieldsBuilder not initialized, cannot build fields");
                        
                        // In case there had been parsing errors, stop here
                        if (parentException.hasSubExceptions()) {
                            throw parentException;
                        }
                        
                        return fieldsBuilder.build();
                    default:
                        // Intentionally left blank, as the other sections are of no interest to us here.
                }
            } else {
                // Proceed analysis
                switch (state) {
                    case Fields:
                        Objects.requireNonNull(fieldsBuilder, "FieldsBuilder not initialized, cannot parse");
                        
                        try{
                            // TODO: Extend with checking if this field is for a different block (which is allowed by spec)
                            fieldsBuilder.parseAndValidateLine(lineIndex, line);
                        } catch (ParserException e) {
                            parentException.addSubException(e.withLineNumber(lineIndex));
                        }
                        
                        break;
                    default:
                        throw new ParserException("We should never see this exception, as we looked for the fields only")
                            .withLineNumber(lineIndex);
                }
            }
    
            // Increment line counter
            lineIndex++;
        }
    
        // The trigger switch did not kick in - only one explanation.
        throw new ParserException("Missing fields section.");
    }
    
}
