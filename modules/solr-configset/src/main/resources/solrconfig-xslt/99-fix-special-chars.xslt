<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    
    <!-- First copy all existing XML over -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="str[@name='hl.bs.chars']/text()">
        <xsl:text disable-output-escaping="yes"><![CDATA[.,!? &#9;&#10;&#13;]]></xsl:text>
    </xsl:template>
</xsl:stylesheet>