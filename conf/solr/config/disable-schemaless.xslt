<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output omit-xml-declaration="yes" indent="yes" encoding="UTF-8"/>
    <xsl:strip-space elements="*"/>
    
    <!-- First copy all existing XML over -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    
    <!--
        Disable schemaless mode as per https://solr.apache.org/guide/8_8/schemaless-mode.html#enable-field-class-guessing
        by switching the default use to "false" for the processor chain activating schemaless operation.
        
        The setting may also be configured via a Solr user property (see https://solr.apache.org/guide/8_8/schemaless-mode.html#disabling-automatic-field-guessing)
        or using "update.autoCreateFields=false" in core.properties.
        
        You can test this XSLT via http://xsltransform.net/eiZQFqt - remember to copy a current base solrconfig.xml into it.
    -->
    <xsl:template match="/config/updateRequestProcessorChain[@name='add-unknown-fields-to-the-schema']/@default">
        <xsl:attribute name="default">
            <xsl:value-of select="'${update.autoCreateFields:false}'"/>
        </xsl:attribute>
    </xsl:template>
</xsl:stylesheet>