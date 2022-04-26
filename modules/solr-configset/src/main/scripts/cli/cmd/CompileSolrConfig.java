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
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Option;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cli.solrteur.AbortScriptException;
import cli.solrteur.Logger;

@Command(name = "solrconfig",
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    showDefaultValues = true,
    sortOptions = false,
    description = "Compile the solrconfig.xml from a source and XSLT files%n")
public
class CompileSolrConfig implements Callable<Integer> {
    
    @ParentCommand
    private cli.solrteur cliParent;
    
    @Option(required = true,
        names = {"--xslts"},
        paramLabel = "<dir>",
        description = "Path to the directory with XSLTs to adapt solrconfig.xml")
    private String solrConfigXSLTDir;
    
    /**
     * Business logic routine, calling all the execution steps.
     * @return The exit code
     */
    @Override
    public Integer call() throws Exception {
        applySolrConfigXSLT();
        return CommandLine.ExitCode.OK;
    }
    
    private void applySolrConfigXSLT() throws AbortScriptException {
        Logger.info("Starting to transform solrconfig.xml...");
        
        final Path solrConfig = cliParent.getTargetDir().resolve(Path.of("conf", "solrconfig.xml"));
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
        Logger.info("Found XSLT files in " + solrConfigXSLTDir + ":");
        for (Path xsltFile : xsltFiles) {
            Logger.info(xsltFile.toString().substring(xsltDir.toString().length()+1));
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
