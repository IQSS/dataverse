package edu.harvard.iq.dataverse.export.citation;

import com.google.auto.service.AutoService;
import io.gdcc.spi.export.DatasetExportQuery;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.FileExportQuery;
import io.gdcc.spi.export.FileMetadataPredicates;
import io.gdcc.spi.export.MultiDatasetExporter;
import io.gdcc.spi.meta.annotations.DataversePlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@DataversePlugin
// TODO: The @AutoService may be removed at a later stage when we can load from main classpath what is annotated with
//       @DataversePlugin without conflicts. Leaving this in for now to continue developing.
@AutoService(Exporter.class)
public class BibTex implements Exporter, MultiDatasetExporter {
    
    @Override
    public String getFormatName() {
        return "bibtex";
    }
    
    @Override
    public String getDisplayName(Locale locale) {
        return "BibTex";
    }
    
    @Override
    public Boolean isHarvestable() {
        return false;
    }
    
    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }
    
    @Override
    public String getMediaType() {
        return "text/x-bibtex";
    }
    
    @Override
    public void exportDataset(ExportDataProvider provider, OutputStream outputStream) throws ExportException {
        exportMultiple(List.of(provider), outputStream);
    }
    
    @Override
    public void exportMultiple(List<ExportDataProvider> list, OutputStream outputStream) throws ExportException {
        try (
            OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(streamWriter)
        ) {
            for (ExportDataProvider provider : list) {
                convert(provider, writer);
                writer.flush();
            }
        } catch (IOException e) {
            throw new ExportException("Failed to export datasets as BibTeX", e);
        }
    }
    
    private void convert(ExportDataProvider provider, BufferedWriter writer) throws IOException {
        DatasetExportQuery query = DatasetExportQuery.builder()
            .fileQuery(FileExportQuery.builder().filePredicates(FileMetadataPredicates.SKIP_FILES).build())
            .build();
        Document dataciteXml = provider.getDataCiteXml(query);
        MetaData metadata = new MetaData(dataciteXml);
        convert(metadata, writer);
    }
    
    private static final String X_IDENTIFIER = "/resource/identifier[@identifierType='DOI']";
    private static final String X_PUBLICATION_YEAR = "/resource/publicationYear";
    private static final String X_RESOURCE_TYPE_GENERAL = "/resource/resourceType/@resourceTypeGeneral";
    private static final String X_TITLE = "/resource/titles/title[1]";
    private static final String X_PUBLISHER = "/resource/publisher";
    private static final String X_VERSION = "/resource/version";
    private static final String X_CREATORS = "/resource/creators/creator";
    
    private static final String X_CREATOR_NAME = "creatorName";
    private static final String X_GIVEN_NAME = "givenName";
    private static final String X_FAMILY_NAME = "familyName";
    
    private void convert(MetaData metadata, BufferedWriter writer) throws IOException {
        
        String year = metadata.string(X_PUBLICATION_YEAR);
        String resourceTypeGeneral = metadata.string(X_RESOURCE_TYPE_GENERAL);
        String doi = metadata.string(X_IDENTIFIER);
        
        // initial line with BibTex type and reference identifier
        writer.write("@%s{%s_%s,".formatted(
            mapToBibTexType(resourceTypeGeneral),
            pidToBibKey(doi),
            year == null || year.isBlank() ? Year.now() : year));
        writer.newLine();
        
        String title = metadata.string(X_TITLE);
        writeBibTexField(writer, "title", title == null || title.isBlank() ? null : "{" + escapePairedQuotes(title) + "}");
        
        List<String> authors = extractAuthors(metadata);
        writeBibTexField(writer, "author", authors.isEmpty() ? null : String.join(" and ", authors));
        
        String publisher = metadata.string(X_PUBLISHER);
        writeBibTexField(writer, "publisher", publisher);
        
        String version = metadata.string(X_VERSION);
        writeBibTexField(writer, "version", version);
        
        /* TODO:
            out.write("url = {");
            out.write(persistentId.asURL());
            out.write("}\r\n");
         */
        
        writeBibTexField(writer, "year", year);
        writeBibTexLastField(writer, "doi", doi);
        
        writer.write("}");
        writer.newLine();
    }
    
    private static String escapePairedQuotes(String string) {
        return string == null ? "" : string.replaceAll("\"([^\"]*)\"", "``$1''");
    }

    private static List<String> extractAuthors(MetaData metadata) {
        NodeList creatorNodes = metadata.nodes(X_CREATORS);
        List<String> authors = new ArrayList<>();
        
        for (int i = 0; i < creatorNodes.getLength(); i++) {
            Node creator = creatorNodes.item(i);
            
            String familyName = metadata.string(creator, X_FAMILY_NAME);
            String givenName = metadata.string(creator, X_GIVEN_NAME);
            String creatorName = metadata.string(creator, X_CREATOR_NAME);
            
            if (familyName != null && !familyName.isBlank() && givenName != null && !givenName.isBlank()) {
                authors.add(familyName + ", " + givenName);
            } else if (creatorName != null && !creatorName.isBlank()) {
                authors.add(creatorName);
            }
        }
        
        return authors;
    }
    
    private static String pidToBibKey(String pid) {
        if (pid == null || pid.isBlank()) {
            return "unknown";
        }
        return pid.replaceAll("\\W+", "_");
    }
    
    private static String mapToBibTexType(String resourceTypeGeneral) {
        if (resourceTypeGeneral == null || resourceTypeGeneral.isBlank()) {
            return "misc";
        }
        // TODO: this should be made configurable, so more mappings can be done
        return switch (resourceTypeGeneral) {
            case "Dataset" -> "dataset";
            case "Software" -> "software";
            default -> "misc";
        };
    }
    
    private static void writeBibTexField(BufferedWriter writer, String name, String value) throws IOException {
        if (value == null || value.isBlank()) {
            return;
        }
        writer.write("  " + name + " = {" + value + "},");
        writer.newLine();
    }
    
    private static void writeBibTexLastField(BufferedWriter writer, String name, String value) throws IOException {
        if (value == null || value.isBlank()) {
            return;
        }
        writer.write("  " + name + " = {" + value + "}");
        writer.newLine();
    }
    
    // TODO: This might be very useful to include in the Exporter SPI package, as it's a nice helper for everyone
    private static final class MetaData {
        
        private static final String DATACITE_NS = "http://datacite.org/schema/kernel-4";
        private final Document dataCiteXml;
        private final XPath xPath;
        
        MetaData(Document dataCiteXml) {
            this.dataCiteXml = dataCiteXml;
            this.xPath = createXPath();
        }
        
        static XPath createXPath() {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if ("d".equals(prefix)) {
                        return DATACITE_NS;
                    }
                    if ("xml".equals(prefix)) {
                        return XMLConstants.XML_NS_URI;
                    }
                    return XMLConstants.NULL_NS_URI;
                }
                
                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }
                
                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    return List.<String>of().iterator();
                }
            });
            return xPath;
        }
        
        String string(String expression) {
            return string(dataCiteXml, expression);
        }
        
        String string(Object item, String expression) {
            try {
                String value = (String) xPath.evaluate(expression, item, XPathConstants.STRING);
                return value == null ? null : value.trim();
            } catch (XPathExpressionException e) {
                throw new IllegalStateException("Invalid XPath expression: " + expression, e);
            }
        }
        
        NodeList nodes(String expression) {
            try {
                return (NodeList) xPath.evaluate(expression, dataCiteXml, XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                throw new IllegalStateException("Invalid XPath expression: " + expression, e);
            }
        }
    }
}
