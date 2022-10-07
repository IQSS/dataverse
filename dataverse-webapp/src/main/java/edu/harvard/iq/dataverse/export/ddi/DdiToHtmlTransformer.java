package edu.harvard.iq.dataverse.export.ddi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.ejb.Stateless;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

@Stateless
public class DdiToHtmlTransformer {
    private static final Logger logger = LoggerFactory.getLogger(DdiToHtmlTransformer.class);

    // -------------------- LOGIC --------------------

    public void transform(InputStream input, Writer output) {
        try (InputStream transformFile = DdiToHtmlTransformer.class.getClassLoader()
                .getResourceAsStream("edu/harvard/iq/dataverse/transform/codebook2-0.xslt")) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document parsed = documentBuilder.parse(input);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(transformFile));
            transformer.transform(new DOMSource(parsed), new StreamResult(output));
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            logger.error("Exception encountered during DDI to HTML conversion", e);
            throw new RuntimeException(e);
        }
    }
}
