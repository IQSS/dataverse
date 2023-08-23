///usr/bin/env jbang "$0" "$@" ; exit $?
//
//SOURCES cmd/*.java
//SOURCES mdb/*.java
//SOURCES mdb/**/*.java
//
//DEPS info.picocli:picocli:4.6.3

package io.gdcc.solrteur;

import io.gdcc.solrteur.cmd.CompileSchema;
import io.gdcc.solrteur.cmd.ExtractConfigSet;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import io.gdcc.solrteur.cmd.CompileSolrConfig;

import java.nio.file.Path;

/**
 * This class is the main entry point into the different functions of handling different aspects of
 * the Dataverse Solr flavor.
 *
 * (The name "solrteur" is a remix of the German word "Solarteur" which means "solar technician".)
 */
@Command(name = solrteur.CLI_NAME,
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    version = solrteur.CLI_NAME+" "+ solrteur.CLI_VERSION,
    description = "Execute different tasks around Dataverse and Solr",
    subcommands = {
        ExtractConfigSet.class,
        CompileSolrConfig.class,
        CompileSchema.class
    },
    synopsisSubcommandLabel = "COMMAND")
public class solrteur {
    public final static String CLI_NAME = "solrteur";
    public final static String CLI_VERSION = "1.0";
    
    @Option(
        required = true,
        names = {"--solr-version", "-s"},
        paramLabel = "<x.y.z>",
        description = "Which version of Solr to use, e. g. 8.9 or 8.11.1")
    private String solrVersion;
    
    public String getSolrVersion() {
        return this.solrVersion;
    }
    
    @Option(required = true,
        names = {"--target", "-t"},
        paramLabel = "<dir>",
        description = "Path to a target directory")
    private Path targetDir;
    
    public Path getTargetDir() {
        return this.targetDir;
    }
    
    @Option(
        names = {"--quiet", "-q"},
        description = "Decrease verbosity"
    )
    static boolean quiet;
    
    /**
     * A wrapper for Throwables to create a checked exception that leads to aborting the execution
     */
    public static final class AbortScriptException extends Exception {
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
    public static final class Logger {
        static void log(String message) {
            System.out.println(message);
        }
        static void logError(String message) {
            System.err.println(message);
        }
        
        public static void info(String message) {
            if (!quiet) {
                log(message);
            }
        }
        public static void info(AbortScriptException ex) {
            if (!quiet) {
                log(ex.getMessage());
                log(ex.getCause().getMessage());
            }
        }
    
        public static void warn(String message) {
            logError(message);
        }
        
        public static void warn(AbortScriptException ex) {
            logError(ex.getMessage());
            logError(ex.getCause().getMessage());
        }
    }
    
    public static void main(String... args) {
        int exitCode = new CommandLine(new solrteur()).execute(args);
        System.exit(exitCode);
    }
}
