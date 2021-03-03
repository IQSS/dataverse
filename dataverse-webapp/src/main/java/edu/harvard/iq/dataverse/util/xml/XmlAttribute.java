package edu.harvard.iq.dataverse.util.xml;

public class XmlAttribute {
    private String attributeName;
    private String attributeValue;

    // -------------------- GETTERS --------------------

    public String getAttributeName() {
        return attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }
    
    // -------------------- LOGIC --------------------
    
    public static XmlAttribute of(String attributeName, String attributeValue) {
        XmlAttribute xmlAttribute = new XmlAttribute();
        xmlAttribute.attributeName = attributeName;
        xmlAttribute.attributeValue = attributeValue;
        return xmlAttribute;
    }
}