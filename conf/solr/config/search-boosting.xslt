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
    
    <!-- Add boosting config XML inside of selected element -->
    <xsl:template match="/config/requestHandler[@name='/select'][@class='solr.SearchHandler']/lst[@name='defaults']">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:copy-of select="node()"/>
            <!--
                 This boosting configuration has been
                 first introduced in 2015, see https://github.com/IQSS/dataverse/issues/1928#issuecomment-91651853,
                 been re-introduced in 2018 for Solr 7.2.1 update, see https://github.com/IQSS/dataverse/issues/4158,
                 and finally evolved to the current state later in 2018 https://github.com/IQSS/dataverse/issues/4938
                 (merged with https://github.com/IQSS/dataverse/commit/3843e5366845d55c327cdb252dd9b4e4125b9b88)
                 
                 Since then, this has not been touched again (2021-12-21).
                 
                 You can test this XSLT via http://xsltransform.net/gWmuPtv - remember to copy a current base solrconfig.xml into it.
            -->
            <str name="defType">edismax</str>
            <float name="tie">0.075</float>
            <str name="qf">
                dvName^400
                authorName^180
                dvSubject^190
                dvDescription^180
                dvAffiliation^170
                title^130
                subject^120
                keyword^110
                topicClassValue^100
                dsDescriptionValue^90
                authorAffiliation^80
                publicationCitation^60
                producerName^50
                fileName^30
                fileDescription^30
                variableLabel^20
                variableName^10
                _text_^1.0
            </str>
            <str name="pf">
                dvName^200
                authorName^100
                dvSubject^100
                dvDescription^100
                dvAffiliation^100
                title^75
                subject^75
                keyword^75
                topicClassValue^75
                dsDescriptionValue^75
                authorAffiliation^75
                publicationCitation^75
                producerName^75
            </str>
            <!-- Even though this number is huge it only seems to apply a boost of ~1.5x to final result -MAD 4.9.3-->
            <str name="bq">
                isHarvested:false^25000
            </str>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>