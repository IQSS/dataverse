<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    
    <!-- First copy all existing XML over -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- Add <schemaFactory> using the static schema.xml right before the first <updateProcessor> -->
    <!-- Test this XSLT via http://xsltransform.net/jyH9Xwx/1 -->
    <xsl:template match="/config/updateProcessor[1]">
        <xsl:if test="not(preceding-sibling::schemaFactory)">
            <schemaFactory class="ClassicIndexSchemaFactory"/>
        </xsl:if>
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>