package cli.cmd;

import cli.solrteur;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

import cli.solrteur.AbortScriptException;
import cli.solrteur.Logger;

@Command(
    name = "extract-zip",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    showDefaultValues = true,
    sortOptions = false,
    description = "Extract default configset from Solr ZIP distribution%n")
public class ExtractConfigSet implements Callable<Integer> {
    
    @ParentCommand
    private solrteur cliParent;
    
    @Option(required = true,
        names = {"--zip"},
        paramLabel = "<file>",
        description = "Path to local Solr ZIP distribution file")
    private Path solrZipFile;
    
    @Option(required = false,
        names = {"--zip-subpath"},
        paramLabel = "<path>",
        description = "Relative path within ZIP to _default configset",
        defaultValue = "solr-{{ solr.version }}/server/solr/configsets/_default")
    private String solrConfigSetZipPath;
    
    /**
     * Business logic routine, calling all the execution steps.
     * @return The exit code
     */
    @Override
    public Integer call() throws Exception {
        replaceVariables();
        extractConfigSet();
        return CommandLine.ExitCode.OK;
    }
    
    private void replaceVariables() throws AbortScriptException {
        this.solrConfigSetZipPath = this.solrConfigSetZipPath.replaceAll("\\Q{{ solr.version }}\\E", cliParent.getSolrVersion());
    }
    
    private void extractConfigSet() throws AbortScriptException {
        // Wrap the file system in a try-with-resources statement
        // to auto-close it when finished and prevent a memory leak
        try (FileSystem zipFileSystem = FileSystems.newFileSystem(solrZipFile, null)) {
            Path zipSource = zipFileSystem.getPath(solrConfigSetZipPath);
            
            // TODO: should we delete the target before copying the new content? (Usually this shouldn't change, but better safe than sorry?)
            
            Logger.info("Extracting " + solrConfigSetZipPath + " from " + solrZipFile + " into " + cliParent.getTargetDir());
            Files.walkFileTree(zipSource, new SimpleFileVisitor<>() {
                // Copy the directory structure (skip existing with the same name)
                @Override
                public FileVisitResult preVisitDirectory(Path zippedDir, BasicFileAttributes attrs) throws IOException {
                    // Remove the leading path part from the ZIP file structure, as we don't want it in target
                    String strippedZipPath = zippedDir.toString().substring(solrConfigSetZipPath.length());
                    Path targetDir = Path.of(cliParent.getTargetDir().toString(), strippedZipPath);
                    
                    try {
                        Logger.info(solrZipFile + ":" + zippedDir + " -> " + targetDir);
                        Files.copy(zippedDir, targetDir, StandardCopyOption.COPY_ATTRIBUTES);
                    } catch (FileAlreadyExistsException e) {
                        // intentional ignore - simply reuse the existing directory
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                // Copy & replace files already present (which makes any run idempotent by deleting the former run)
                @Override
                public FileVisitResult visitFile(Path zippedFile, BasicFileAttributes attrs) throws IOException {
                    String strippedZipPath = zippedFile.toString().substring(solrConfigSetZipPath.length());
                    Path targetFile = Path.of(cliParent.getTargetDir().toString(), strippedZipPath);
    
                    Logger.info(solrZipFile + ":" + zippedFile + " -> " + targetFile);
                    Files.copy(zippedFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch(IOException e) {
            throw new AbortScriptException("Extracting from ZIP file "+solrZipFile+" failed", e);
        }
    }
}
