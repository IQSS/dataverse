///usr/bin/env jbang "$0" "$@" ; exit $?
//
//SOURCES cmd/*.java
//
//DEPS info.picocli:picocli:4.6.3

package cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import cli.cmd.CompileSolrConfig;

/**
 * This class is the main entry point into the different functions of handling different aspects of
 * the Dataverse Solr flavor.
 *
 * (The name "solrteur" is a remix of the German word "Solarteur" which means "solar technician".)
 */
@Command(name = solrteur.CLI_NAME,
    mixinStandardHelpOptions = true,
    version = solrteur.CLI_NAME+" "+ solrteur.CLI_VERSION,
    description = "Execute different tasks around Dataverse and Solr",
    subcommands = {
        CompileSolrConfig.class
    })
public class solrteur {
    public final static String CLI_NAME = "solrteur";
    public final static String CLI_VERSION = "1.0";
    
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
        static void log(AbortScriptException ex) {
            System.out.println(ex.getMessage());
            ex.getCause().printStackTrace();
        }
        
        public static void info(String message) {
            if (!quiet) {
                log(message);
            }
        }
        public static void info(AbortScriptException ex) {
            if (!quiet) {
                log(ex);
            }
        }
    }
    
    public static void main(String... args) {
        int exitCode = new CommandLine(new solrteur()).execute(args);
        System.exit(exitCode);
    }
}
