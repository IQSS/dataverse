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
    
    <!-- Add <schemaFactory> using the static schema.xml right before the first <updateProcessor> -->
    <!-- Test this XSLT via http://xsltransform.net/jyH9Xwx -->
    <xsl:template match="/config/updateProcessor[1]">
        <schemaFactory class="ClassicIndexSchemaFactory"/>
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>