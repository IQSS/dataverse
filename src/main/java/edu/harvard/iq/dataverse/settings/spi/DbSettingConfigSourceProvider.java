package edu.harvard.iq.dataverse.settings.spi;

import edu.harvard.iq.dataverse.settings.source.DbSettingConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import java.util.Arrays;

public class DbSettingConfigSourceProvider implements ConfigSourceProvider {
    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return Arrays.asList(new DbSettingConfigSource());
    }
}