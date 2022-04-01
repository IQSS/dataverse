///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS net.sf.saxon:Saxon-HE:10.6
package cli.cmd;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(name = "CompileSolrConfig",
        mixinStandardHelpOptions = true,
        version = "CompileSolrConfig 0.1",
        description = "CompileSolrConfig made with jbang")
public class CompileSolrConfig implements Callable<Integer> {
    
    /**
     * A wrapper for Throwables to create a checked exception that leads to aborting the execution
     */
    static final class AbortScriptException extends Throwable {
        private AbortScriptException() {}
        public AbortScriptException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
    
    /**
     * Static inner logging wrapper for convenience.
     * This is here because we don't want to add more clutter of a logging framework
     * to the Maven output where we use this from.
     */
    static final class Logger {
        public static void log(String message) {
            System.out.println(message);
        }
        public static void log(AbortScriptException ex) {
            System.out.println(ex.getMessage());
            ex.getCause().printStackTrace();
        }
    }
    
    @Option(required = true,
        names = {"--solr-version"},
        description = "The release version of Solr (must match with the ZIP files content)")
    private String solrVersion;
    
    @Option(required = true,
        names = {"--solr-zipfile"},
        description = "Path to local Solr ZIP distribution file")
    private Path solrZipFile;
    
    @Option(required = false,
        names = {"--solr-configset-zippath"},
        description = "Path within ZIP to _default configset",
        defaultValue = "solr-{{ solr.version }}/server/solr/configsets/_default")
    private String solrConfigSetZipPath;
    
    @Option(required = true,
        names = {"--solr-configset-targetdir"},
        description = "Path to the new configset directory")
    private String solrConfigSetTargetDir;
    
    @Option(required = true,
        names = {"--solr-schemaxml-sourcepath"},
        description = "Path to the schema.xml to include")
    private String solrSchemaXmlSourcePath;
    
    @Option(required = true,
        names = {"--solr-config-xsltdir"},
        description = "Path to the directory with XSLTs to adapt solrconfig.xml")
    private String solrConfigXSLTDir;
    
    public static void main(String... args) {
        int exitCode = new CommandLine(new CompileSolrConfig()).execute(args);
        System.exit(exitCode);
    }
    
    /**
     * Business logic routine, calling all the execution steps.
     * @return The exit code
     */
    @Override
    public Integer call() throws Exception { // TODO: remove Exception signature
        try {
            replaceVariables();
            extractConfigSet();
            replaceSchemaXML();
            applySolrConfigXSLT();
        } catch (AbortScriptException e) {
            Logger.log(e);
            // this might be nicely refactored with exit codes stored within the exception
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }
    
    private void replaceVariables() throws AbortScriptException {
        this.solrConfigSetZipPath = this.solrConfigSetZipPath.replaceAll("\\Q{{ solr.version }}\\E", solrVersion);
    }
    
    private void extractConfigSet() throws AbortScriptException {
        // Wrap the file system in a try-with-resources statement
        // to auto-close it when finished and prevent a memory leak
        try (FileSystem zipFileSystem = FileSystems.newFileSystem(solrZipFile, null)) {
            Path zipSource = zipFileSystem.getPath(solrConfigSetZipPath);
            
            // TODO: should we delete the target before copying the new content? (Usually this shouldn't change, but better safe than sorry?)
            
            Logger.log("Starting to extract Solr _default config set...");
            Files.walkFileTree(zipSource, new SimpleFileVisitor<>() {
                // Copy the directory structure (skip existing with the same name)
                @Override
                public FileVisitResult preVisitDirectory(Path zippedDir, BasicFileAttributes attrs) throws IOException {
                    // Remove the leading path part from the ZIP file structure, as we don't want it in target
                    String strippedZipPath = zippedDir.toString().substring(solrConfigSetZipPath.length());
                    Path targetDir = Path.of(solrConfigSetTargetDir, strippedZipPath);
                    
                    try {
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
                    Path targetFile = Path.of(solrConfigSetTargetDir, strippedZipPath);
                    
                    Files.copy(zippedFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch(IOException e) {
            throw new AbortScriptException("Extracting from ZIP file "+solrZipFile+" failed", e);
        }
    }
    
    private void replaceSchemaXML() throws AbortScriptException {
        Logger.log("Starting to replace the Solr schema...");
        
        // Delete "managed-schema" file
        try {
            Path managedSchema = Path.of(solrConfigSetTargetDir, "conf", "managed-schema");
            Files.deleteIfExists(managedSchema);
        } catch (IOException e) {
            throw new AbortScriptException("Could not delete managed-schema", e);
        }
            
        // Copy schema.xml in place
        try {
            Path sourceSchema = Path.of(solrSchemaXmlSourcePath);
            Path targetSchema = Path.of(solrConfigSetTargetDir, "conf", "schema.xml");
            Files.copy(sourceSchema, targetSchema, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new AbortScriptException("Could not copy schema.xml", e);
        }
    }
    
    private void applySolrConfigXSLT() throws AbortScriptException {
        Logger.log("Starting to transform solrconfig.xml...");
        
        final Path solrConfig = Path.of(solrConfigSetTargetDir, "conf", "solrconfig.xml");
        final Path xsltDir = Path.of(solrConfigXSLTDir);
    
        // Find all the XSLT files
        final List<Path> xsltFiles;
        try (Stream<Path> walk =  Files.walk(xsltDir, 2)) {
            xsltFiles = walk.filter(path -> path.toFile().isFile())
                            .filter(path -> path.toString().endsWith("xslt"))
                            .sorted()
                            .collect(Collectors.toList());
        } catch (IOException e) {
            throw new AbortScriptException("Could not walk over XSLT files at " + xsltDir, e);
        }
        
        // Log found XSLT files
        Logger.log("Found XSLT files in " + solrConfigXSLTDir + ":");
        for (Path xsltFile : xsltFiles) {
            Logger.log(xsltFile.toString().substring(xsltDir.toString().length()+1));
        }
        
        // Setup the XSLT processor
        final Processor processor = new Processor(false);
        final XsltCompiler compiler = processor.newXsltCompiler();
        
        try {
    
            // First iteration uses initial solrconfig.xml as input source
            StreamSource source = new StreamSource(Files.newInputStream(solrConfig));
    
            // For every XSLT, we need to do the transformation and rotate the input source afterwards,
            // so we apply the next transformation to the already transformed content.
            for (Path xsltFile : xsltFiles) {
                final XsltExecutable stylesheet = compiler.compile(new StreamSource(Files.newInputStream(xsltFile)));
    
                // Prepare a fresh temporary file (which will be deleted when we read it back for the next iteration)
                final Path tmpFile = Files.createTempFile(null, null);
                Serializer out = processor.newSerializer(Files.newOutputStream(tmpFile));
    
                // Actual transformation happens
                Xslt30Transformer transformer = stylesheet.load30();
                transformer.transform(source, out);
    
                // Read back the transformed config and rotate the source. The opening option makes the old temp file
                // go away after it has been read.
                source = new StreamSource(Files.newInputStream(tmpFile, StandardOpenOption.DELETE_ON_CLOSE));
            }
    
            // The final transformation still reads back the final result, so we need to push InputStream content somewhere
            Files.copy(source.getInputStream(),
                solrConfig,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AbortScriptException("Could not complete solrconfig.xml compilation", e);
        } catch (SaxonApiException e) {
            throw new AbortScriptException("XML transformation failed", e);
        }
    }
}
