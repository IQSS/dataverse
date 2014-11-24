<?xml version="1.0"?>
<!-- convert an XML tree into paths/like/this -->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text"/>
  <xsl:template match="node()">
    <xsl:for-each select="ancestor-or-self::*">
      <xsl:value-of select="concat('/',name(.))"/>
    </xsl:for-each>
    <!--line break -->
    <xsl:text>
</xsl:text>
    <xsl:apply-templates select="*"/>
  </xsl:template>
</xsl:transform>
