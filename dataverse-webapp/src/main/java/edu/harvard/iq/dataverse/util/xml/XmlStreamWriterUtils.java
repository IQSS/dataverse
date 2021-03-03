package edu.harvard.iq.dataverse.util.xml;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.util.Arrays;
import java.util.List;

public class XmlStreamWriterUtils {

    // -------------------- LOGIC --------------------
    
    public static void writeAttribute(XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        if (StringUtils.isNotEmpty(value)) {
            xmlw.writeAttribute(name, value);
        }
    }
    
    public static void writeFullElement(XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        writeFullElementWithAttributes(xmlw, name, value);
    }

    public static void writeFullElementList(XMLStreamWriter xmlw, String name, List<String> values) throws XMLStreamException {
        if (CollectionUtils.isNotEmpty(values)) {
            for (String value : values) {
                writeFullElement(xmlw, name, value);
            }
        }
    }
    
    public static void writeFullAttributesOnlyElement(XMLStreamWriter xmlw, String name, XmlAttribute ...attributes) throws XMLStreamException {
        List<XmlAttribute> attributesList = Arrays.asList(attributes);
        if (attributesList.stream().allMatch(attr -> StringUtils.isEmpty(attr.getAttributeValue()))) {
            return;
        }

        xmlw.writeStartElement(name);
        for (XmlAttribute attribute: attributesList) {
            if (StringUtils.isNotEmpty(attribute.getAttributeValue())) {
                xmlw.writeAttribute(attribute.getAttributeName(), attribute.getAttributeValue());
            }
        }
        xmlw.writeEndElement();
    }
    
    public static void writeFullElementWithAttributes(XMLStreamWriter xmlw, String name, String value, XmlAttribute ...attributes) throws XMLStreamException {
        if (StringUtils.isNotEmpty(value)) {
            xmlw.writeStartElement(name);
            for (XmlAttribute attribute: Arrays.asList(attributes)) {
                if (StringUtils.isNotEmpty(attribute.getAttributeValue())) {
                    xmlw.writeAttribute(attribute.getAttributeName(), attribute.getAttributeValue());
                }
            }
            xmlw.writeCharacters(value);
            xmlw.writeEndElement();
        }
    }

}
