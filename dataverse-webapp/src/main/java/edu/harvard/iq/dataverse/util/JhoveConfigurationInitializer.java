package edu.harvard.iq.dataverse.util;

import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.apache.commons.lang3.StringUtils.replaceOnce;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Initializer of jhove configuration files.
 * 
 * Classes that uses jhove should assume that configuration file for jhove is
 * located in {@link #getConfigPath}.
 */
@Startup
@Singleton
public class JhoveConfigurationInitializer {
	
	static Path getConfigPath() {
		
		return pathOf("jhove.conf");
	}
	
	/**
	 * Copies jhove configuration and configuration xsd files from classpath into
	 * temp directory. In case of configuration file - it also replaces xsd location
	 * placeholder to the path in temp directory where it was copied.
	 * 
	 * Replacing of schema location is done, because jhove will validate given
	 * configuration file against it.
	 */
	@PostConstruct
	public void initializeJhoveConfig() {

		try {
			final Path xsdPath = copyXsd();
			copyConfigWith(xsdPath);
		} catch (final IOException e) {
			throw new RuntimeException("Unable to prepare jhove configuration files", e);
		}
	}

	private Path copyXsd() throws IOException {

		final Path target = pathOf("jhoveConfig.xsd");
		
		try(final InputStream in = getResourceAsStream("jhove/jhoveConfig.xsd")) {
			copy(in, target, REPLACE_EXISTING);
		}
		return target;
	}
	
	private void copyConfigWith(final Path xsd) throws IOException {
		
		String jhoveConf = resourceToString("jhove/jhove.conf", UTF_8,
				currentThread().getContextClassLoader());
		
		jhoveConf = replaceOnce(jhoveConf, "{jhove.config.xsd.path}", "file://" + xsd.toString());
		
		try(final Writer out = newBufferedWriter(getConfigPath())) {
			out.write(jhoveConf);
		}
	}
	
	private static Path pathOf(final String name) {
		
		return Paths.get(getProperty("java.io.tmpdir"), name);
	}

	private static InputStream getResourceAsStream(final String name) {
		
		return currentThread().getContextClassLoader().getResourceAsStream(name);
	}
}
