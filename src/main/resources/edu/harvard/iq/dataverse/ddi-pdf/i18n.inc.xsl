<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:outline="http://worldbank.org/toolkit/cdrom/outline" exclude-result-prefixes="outline">
	<xsl:param name="language-code" select="'en'"/>
	<xsl:variable name="msg" select="document(concat('messages_',$language-code,'.properties.xml'))"/>
</xsl:stylesheet>
