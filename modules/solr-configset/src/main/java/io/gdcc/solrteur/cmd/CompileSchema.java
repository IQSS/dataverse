package io.gdcc.solrteur.cmd;

import io.gdcc.solrteur.mdb.tsv.Block;
import io.gdcc.solrteur.mdb.MetadataBlockTSVReader;
import io.gdcc.solrteur.mdb.tsv.Field;
import io.gdcc.solrteur.mdb.tsv.ParserException;
import io.gdcc.solrteur.solrteur;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(
    name = "schema",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    showDefaultValues = true,
    sortOptions = false,
    description = "Compile the schema.xml from a template and add metadata fields from blocks%n")
public class CompileSchema implements Callable<Integer> {
    
    @CommandLine.ParentCommand
    private solrteur cliParent;
    
    @CommandLine.Option(required = true,
        names = {"--base"},
        paramLabel = "<file>",
        description = "Path to the base schema.xml, to be enriched with fields from blocks")
    private String baseSchemaPath;
    
    @CommandLine.Option(required = true,
        names = {"--tsvs"},
        paramLabel = "<dir/file>",
        description = "Repeatable argument to directories and/or TSV files with metadata blocks.")
    private Path[] mdbTsvPaths;
    
    @CommandLine.Option(required = false,
        names = {"--mfb"},
        paramLabel = "<string>",
        description = "String to indicate a mark after which all <field> should be inserted.",
        defaultValue = "SCHEMA-FIELDS::BEGIN")
    private String markFieldsBegin;
    
    @CommandLine.Option(required = false,
        names = {"--mfe"},
        paramLabel = "<string>",
        description = "String to indicate a mark where insertion of all <field> shall end.",
        defaultValue = "SCHEMA-FIELDS::END")
    private String markFieldsEnd;
    
    @CommandLine.Option(required = false,
        names = {"--mcfb"},
        paramLabel = "<string>",
        description = "String to indicate a mark after which all <copyField> should be inserted.",
        defaultValue = "SCHEMA-COPY-FIELDS::BEGIN")
    private String markCopyFieldsBegin;
    
    @CommandLine.Option(required = false,
        names = {"--mcfe"},
        paramLabel = "<string>",
        description = "String to indicate a mark where insertion of all <copyField> shall end.",
        defaultValue = "SCHEMA-COPY-FIELDS::END")
    private String markCopyFieldsEnd;
    
    /**
     * Business logic routine, calling all the execution steps.
     * @return The exit code
     */
    @Override
    public Integer call() throws Exception {
        getFieldsFromBlocks();
        
        return CommandLine.ExitCode.OK;
    }
    
    private void getFieldsFromBlocks() throws solrteur.AbortScriptException {
    
        MetadataBlockTSVReader reader = new MetadataBlockTSVReader();
        
        // Walk all locations and add files to a large list
        List<Path> allTsvFiles = new ArrayList<>();
        for (Path path : mdbTsvPaths) {
            if (Files.isDirectory(path)) {
                allTsvFiles.addAll(findTsvFiles(path));
            } else if (Files.isReadable(path) && Files.isRegularFile(path)) {
                allTsvFiles.add(path);
            }
        }
    
        // Much nicer in output if sorted
        Collections.sort(allTsvFiles);
        
        // Iterate the files and read the blocks to have a set of 'em (necessary for cross-checking fields)
        Map<Block, Path> blockPathMap = new HashMap<>();
        boolean hadErrors = false;
        for (Path path : allTsvFiles) {
            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                Block block = reader.retrieveBlock(lines);
                
                if (blockPathMap.containsKey(block)) {
                    throw new ParserException("Metadata block " + block.getName() + "already present in " + blockPathMap.get(block) + "!");
                }
                blockPathMap.put(block, path);
                
            } catch (ParserException e) {
                // Log a warning but continue parsing with next file to get done as much as possible.
                hadErrors = true;
                logErrors(path, e);
            } catch (IOException e) {
                // Log a warning but continue parsing with next file to get done as much as possible.
                hadErrors = true;
                solrteur.Logger.warn(new solrteur.AbortScriptException("Could not read "+path, e));
            }
        }
    
        // Abort here if there were errors
        if (hadErrors) {
            throw new solrteur.AbortScriptException("Experienced parsing errors, fix your block definitions first to continue", null);
        }
        
        // If all blocks could be read and are valid, let's extract the fields
        Map<Block, List<Field>> blockFieldsMap = new HashMap<>();
        Set<Block> blocks = blockPathMap.keySet();
    
        for (Map.Entry<Block, Path> mdb : blockPathMap.entrySet()) {
            Block block = mdb.getKey();
            Path path = mdb.getValue();

            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                // First store all retrieved fields, we check on uniqueness later
                blockFieldsMap.put(block, reader.retrieveFields(lines, blocks));
            } catch (ParserException e) {
                // Log a warning but continue parsing with next file to get done as much as possible.
                hadErrors = true;
                logErrors(path, e);
            } catch (IOException e) {
                // Log a warning but continue parsing with next file to get done as much as possible.
                hadErrors = true;
                solrteur.Logger.warn(new solrteur.AbortScriptException("Could not read "+path, e));
            }
        }
    
        // Abort here if there were errors
        if (hadErrors) {
            throw new solrteur.AbortScriptException("Experienced parsing errors, fix your field definitions first to continue", null);
        }
        
        // We need to check uniqueness of fields across blocks
        Map<Field,Block> fieldBlockMap = new HashMap<>();
        for (Map.Entry<Block,List<Field>> entry : blockFieldsMap.entrySet()) {
            Block block = entry.getKey();
            System.out.println("BLOCK: " + block.getName());
            
            for (Field field : entry.getValue()) {
                System.out.println("    FIELD: " + field.getName());
                
                if (fieldBlockMap.containsKey(field)) {
                    hadErrors = true;
                    solrteur.Logger.warn("Duplicate field '" + field.getName() + "' in block '" + block.getName() +
                        "' from '" + blockPathMap.get(block) + "': already defined by block '" +
                        fieldBlockMap.get(field).getName() + "' from '" + blockPathMap.get(fieldBlockMap.get(field)) + "'");
                } else {
                    fieldBlockMap.put(field, block);
                }
            }
        }
    
        // Abort here if there were errors
        if (hadErrors) {
            throw new solrteur.AbortScriptException("Stopping analysis", null);
        }
    
    }
    private void injectFields() {}
    private void injectCopyFields() {}
    
    private List<Path> findTsvFiles(Path dir) throws solrteur.AbortScriptException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        
        try (Stream<Path> walk =  Files.walk(dir, 2)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(Files::isReadable)
                .filter(path -> path.toString().endsWith(".tsv"))
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new solrteur.AbortScriptException("Could not walk over TSV files at " + dir, e);
        }
    }
    
    private void logErrors(final Path path, final ParserException e) {
        String fileName = path.getFileName().toString();
        logPE(fileName, "", "", e);
    }
    
    private void logPE(final String fileName, final String lineNumber, final String indent, final ParserException e) {
        String ln = lineNumber.isEmpty() ? e.getLineNumber() : lineNumber;
        
        solrteur.Logger.warn(fileName + ln + ": " + indent + e.getMessage());
        for (ParserException pe : e.getSubExceptions()) {
            logPE(fileName, ln, indent + "  ", pe);
        }
    }
}
