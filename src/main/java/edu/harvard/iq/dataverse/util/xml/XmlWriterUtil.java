package edu.harvard.iq.dataverse.util.xml;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;

public class XmlWriterUtil {

    public static void writeFullElementList(XMLStreamWriter xmlw, String name, List<String> values) throws XMLStreamException {
        // For the simplest Elements we can
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                xmlw.writeStartElement(name);
                xmlw.writeCharacters(value);
                xmlw.writeEndElement(); // labl
            }
        }
    }

    public static void writeI18NElementList(XMLStreamWriter xmlw, String name, List<String> values,
            String fieldTypeName, String fieldTypeClass, String metadataBlockName, String lang)
            throws XMLStreamException {

        if (values != null && !values.isEmpty()) {
            Locale defaultLocale = Locale.getDefault();
            for (String value : values) {
                if (fieldTypeClass.equals("controlledVocabulary")) {
                    String localeVal = ControlledVocabularyValue.getLocaleStrValue(value, fieldTypeName, metadataBlockName, defaultLocale, false);
                    if (localeVal != null) {

                        value = localeVal;
                        writeFullElement(xmlw, name, value, defaultLocale.getLanguage());
                    } else {
                        writeFullElement(xmlw, name, value);
                    }
                } else {
                    writeFullElement(xmlw, name, value);
                }
            }
            if (lang != null && !defaultLocale.getLanguage().equals(lang)) {
                // Get values in dataset metadata language
                // Loop before testing fieldTypeClass to be ready for external CVV
                for (String value : values) {
                    if (fieldTypeClass.equals("controlledVocabulary")) {
                        String localeVal = ControlledVocabularyValue.getLocaleStrValue(value, fieldTypeName, metadataBlockName, new Locale(lang), false);
                        if (localeVal != null) {
                            writeFullElement(xmlw, name, localeVal, lang);
                        }
                    }
                }
            }
        }
    }

    public static void writeI18NElement(XMLStreamWriter xmlw, String name, DatasetVersionDTO version,
            String fieldTypeName, String lang) throws XMLStreamException {
        // Get the default value
        String val = dto2Primitive(version, fieldTypeName);
        Locale defaultLocale = Locale.getDefault();
        // Get the language-specific value for the default language
        // A null value is returned if this is not a CVV field
        String localeVal = dto2Primitive(version, fieldTypeName, defaultLocale);
        String requestedLocaleVal = null;
        if (lang != null && localeVal != null && !defaultLocale.getLanguage().equals(lang)) {
            // Also get the value in the requested locale/lang if that's not the default
            // lang.
            requestedLocaleVal = dto2Primitive(version, fieldTypeName, new Locale(lang));
        }
        // FWIW locale-specific vals will only be non-null for CVV values (at present)
        if (localeVal == null && requestedLocaleVal == null) {
            // Not CVV/no translations so print without lang tag
            writeFullElement(xmlw, name, val);
        } else {
            // Print in either/both languages if we have values
            if (localeVal != null) {
                // Print the value for the default locale with it's own lang tag
                writeFullElement(xmlw, name, localeVal, defaultLocale.getLanguage());
            }
            // Also print in the request lang (i.e. the metadata language for the dataset)
            // if a value exists, print it with a lang tag
            if (requestedLocaleVal != null) {
                writeFullElement(xmlw, name, requestedLocaleVal, lang);
            }
        }
    }

    public static String dto2Primitive(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            MetadataBlockDTO value = entry.getValue();
            for (FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    return fieldDTO.getSinglePrimitive();
                }
            }
        }
        return null;
    }

    public static String dto2Primitive(DatasetVersionDTO datasetVersionDTO, String datasetFieldTypeName, Locale locale) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            MetadataBlockDTO value = entry.getValue();
            for (FieldDTO fieldDTO : value.getFields()) {
                if (datasetFieldTypeName.equals(fieldDTO.getTypeName())) {
                    String rawVal = fieldDTO.getSinglePrimitive();
                    if (fieldDTO.isControlledVocabularyField()) {
                        return ControlledVocabularyValue.getLocaleStrValue(rawVal, datasetFieldTypeName, value.getName(),
                                locale, false);
                    }
                }
            }
        }
        return null;
    }

    public static void writeFullElement(XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        writeFullElement(xmlw, name, value, null);
    }

    public static void writeFullElement(XMLStreamWriter xmlw, String name, String value, String lang) throws XMLStreamException {
        // For the simplest Elements we can
        if (!StringUtils.isEmpty(value)) {
            xmlw.writeStartElement(name);
            if (DvObjectContainer.isMetadataLanguageSet(lang)) {
                writeAttribute(xmlw, "xml:lang", lang);
            }
            xmlw.writeCharacters(value);
            xmlw.writeEndElement(); // labl
        }
    }

    public static void writeAttribute(XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        if (!StringUtils.isEmpty(value)) {
            xmlw.writeAttribute(name, value);
        }
    }


    public static void writeFullElementWithAttributes(XMLStreamWriter xmlw, String name, Map<String, String> attributeMap, String value) throws XMLStreamException {
        if (!StringUtils.isEmpty(value)) {
            xmlw.writeStartElement(name);
            for (String key : attributeMap.keySet()) {
                writeAttribute(xmlw, key, attributeMap.get(key));
            }
            xmlw.writeCharacters(value);
            xmlw.writeEndElement(); // labl
        }
    }

    public static boolean writeOpenTagIfNeeded(XMLStreamWriter xmlw, String tag, boolean element_check) throws XMLStreamException {
        // check if the current tag isn't opened
        if (!element_check) {
            xmlw.writeStartElement(tag); // <value of tag>
        }
        return true;
    }
}
