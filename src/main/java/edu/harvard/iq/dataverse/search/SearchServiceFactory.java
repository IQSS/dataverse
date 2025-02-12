package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class SearchServiceFactory {

    private static final Logger logger = Logger.getLogger(SearchServiceFactory.class.getCanonicalName());

    @Inject
    private BeanManager beanManager;

    private Map<String, SearchService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        loadSearchServices();
    }

    private void loadSearchServices() {
        // Load built-in services
        Set<Bean<?>> beans = beanManager.getBeans(SearchService.class);
        for (Bean<?> bean : beans) {
            SearchService service = (SearchService) beanManager.getReference(bean, SearchService.class, beanManager.createCreationalContext(bean));
            serviceMap.put(service.getServiceName(), service);
            logger.log(Level.INFO, "Loaded built-in search service: {0}", service.getServiceName());
        }

        // Load external services
        Optional<String> searchServicePathSetting = JvmSettings.SEARCHSERVICES_DIRECTORY.lookupOptional(String.class);
        if (searchServicePathSetting.isPresent()) {
            Path servicesDir = Paths.get(searchServicePathSetting.get());
            List<URL> jarUrls = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(servicesDir, "*.jar")) {
                for (Path path : stream) {
                    jarUrls.add(new URL("jar:" + path.toUri().toURL() + "!/"));
                }
            } catch (IOException e) {
                logger.warning("Problem accessing external Search Services: " + e.getLocalizedMessage());
            }

            URLClassLoader cl = URLClassLoader.newInstance(jarUrls.toArray(new URL[0]), this.getClass().getClassLoader());
            ServiceLoader<SearchService> loader = ServiceLoader.load(SearchService.class, cl);

            for (SearchService service : loader) {
                serviceMap.put(service.getServiceName(), service);
                logger.log(Level.INFO, "Loaded external search service: {0}", service.getServiceName());
            }
        }
    }

    public SearchService getSearchService(String name) {
        SearchService service = serviceMap.get(name);
        if (service == null) {
            throw new IllegalArgumentException("Unknown search service: " + name);
        }
        return service;
    }
    
    public SearchService getDefaultSearchService() {
        return getSearchService(JvmSettings.DEFAULT_SEARCH_SERVICE.lookupOptional().orElse("solr"));
    }

    public Set<String> getAvailableServices() {
        return serviceMap.keySet();
    }
}