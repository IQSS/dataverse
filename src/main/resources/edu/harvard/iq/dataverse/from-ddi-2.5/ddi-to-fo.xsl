<?xml version="1.0" encoding="UTF-8"?>
<!--
	Overview:
	Transforms DDI-XML into XSL-FO to produce study documentation in PDF format
	Developed for DDI documents produced by the International Household Survey Network
	Microdata Managemenet Toolkit (http://www.surveynetwork.org/toolkit) and
	Central Survey Catalog (http://www.surveynetwork.org/surveys)
	
	Author: Pascal Heus (pascal.heus@gmail.com)
	Version: July 2006
	Platform: XSL 1.0, Apache FOP 0.20.5 (http://xmlgraphics.apache.org/fop)
	
	Updated for FOP 0.93 2010 - oistein.kristiansen@nsd.uib.no
	
	License:
	Copyright 2006 Pascal Heus (pascal.heus@gmail.com)
	
	This program is free software; you can redistribute it and/or modify it under the terms of the
	GNU Lesser General Public License as published by the Free Software Foundation; either version
	2.1 of the License, or (at your option) any later version.
	
	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
	without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
	See the GNU Lesser General Public License for more details.
	
	The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
	
	References:
	XSL-FO:
	http://www.w3.org/Style/XSL/
	http://www.w3schools.com/xslfo/xslfo_reference.asp
	http://www.xslfo.info/
	Apache FOP:
	http://xmlgraphics.apache.org/fop/
	XSL-FO Tutorials:
	http://www.renderx.com/tutorial.html
	http://www.antennahouse.com/XSLsample/XSLsample.htm
	String trimming:
	http://skew.org/xml
	
	History:
	200604: Added multilingual support and French translation
	200606: Added Spanish and new elements to match IHSN Template v1.2
	200607: Minor fixes and typos
	200607: Added option parameters to hide producers in cover page and questions in variables list page
	201003: Made it FOP 0.93 compatible
-->
<xsl:stylesheet version="1.0" xmlns:date="http://exslt.org/dates-and-times"
	xmlns:ddi="ddi:codebook:2_5" 
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:dcterms="http://purl.org/dc/terms/" xmlns:doc="http://www.icpsr.umich.edu/doc"
	xmlns:exsl="http://exslt.org/common" xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:math="http://exslt.org/math"
	xmlns:n1="ddi:codebook:2_5 https://ddialliance.org/Specification/DDI-Codebook/2.5/XMLSchema/codebook.xsd"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:str="http://exslt.org/strings"
	xmlns:xdt="http://www.w3.org/2005/xpath-datatypes" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" extension-element-prefixes="date exsl str">
	<xsl:output version="1.0" encoding="UTF-8" indent="no" omit-xml-declaration="no"
		media-type="text/html"/>
	<!-- Required by EXSLT date function -->
	<xsl:variable name="date:date-time" select="'2000-01-01T00:00:00Z'"/>
	<!--
		PARAMETERS
	-->
	<!--
		Report optional text
	-->
	<xsl:param name="report-acknowledgments"/>
	<xsl:param name="report-notes"/>
	<!--
		Full path to RDF file
	-->
	<xsl:param name="rdf-file"/>
	<!--
		Report date
	-->
	<xsl:variable name="exslt-date">
		<xsl:call-template name="date:date"/>
	</xsl:variable>
	<xsl:param name="report-date" select="$exslt-date"/>
	<!--
		Start page number, used by Overview (useful if running multi-survey reports)
	-->
	<xsl:param name="report-start-page-number" select="4"/>
	<!--
		The following parameters toggles page sequences on/off
	-->
	<xsl:param name="show-cover-page" select="1"/>
   <xsl:param name="show-toc" select="1"/>  
	<xsl:param name="show-cover-page-producer" select="1"/>
	<xsl:param name="show-metadata-info" select="1"/>
	<xsl:param name="show-metadata-production" select="1"/>
	<xsl:param name="show-overview" select="1"/>
	<xsl:param name="show-files-description" select="1"/>
	<xsl:param name="show-variables-list-question" select="1"/>
	<xsl:param name="show-variables-list-question-max-len" select="300"/>
	<xsl:param name="show-variables-list-layout">default-page</xsl:param>
	<xsl:param name="show-variables-description-categories" select="1"/>
	<xsl:param name="show-variables-description-categories-max" select="1000"/>
    
	<!-- documentation refer to a rdf file given as parameter which we dont have -->
	<xsl:param name="show-documentation" select="0"/>
	<xsl:param name="show-documentation-description" select="0"/>
	<xsl:param name="show-documentation-abstract" select="0"/>
	<xsl:param name="show-documentation-toc" select="0"/>
	<xsl:param name="show-documentation-subjects" select="0"/>
	<xsl:param name="page-format" select="us-letter"/>
	
	<!-- 
		Params from OutputServlet.java
	-->
   <xsl:param name="showVariableGroupsParam"/>
	<xsl:param name="numberOfVars" select="10000"/>
	<xsl:param name="numberOfGroups"/>
	<xsl:param name="subsetGroups"/>
	<xsl:param name="subsetVars"/>
	<xsl:param name="maxVars"/>
   <xsl:param name="allowHTML" select="0"/>
	
	<!--
		Fonts (use MSGothic for non-latin character sets)
	-->
	<xsl:param name="font-family">Times</xsl:param>
	
	<!-- LOAD MULTILINGUAL STRINGS -->
	<xsl:include href="ddi-pdf/i18n.inc.xslt"/>
	<!--<xsl:include href="ddi-pdf/i18n.inc.xslt"/>-->
	<!--
		Report title
	-->
	<xsl:param name="report-title" select=" 'Study Documentation' "/>
	<!--
		STYLES
	-->
	<xsl:variable name="cell-padding" select=" '3pt' "/>
	<xsl:variable name="default-border" select=" '0.5pt solid black' "/>
	<xsl:variable name="color-white" select=" '#ffffff' "/>
	<xsl:variable name="color-gray0" select=" '#f8f8f8' "/>
	<xsl:variable name="color-gray1" select=" '#f0f0f0' "/>
	<xsl:variable name="color-gray2" select=" '#e0e0e0' "/>
	<xsl:variable name="color-gray3" select=" '#d0d0d0' "/>
	<xsl:variable name="color-gray4" select=" '#c0c0c0' "/>
	<!--
		GLOBAL VARIABLES
	-->
	<!-- ddi-flavor
		The ddi-flavor variable is used to customize the stylesheet to different type of DDI document
		The default value is 'toolkit' which indicates a DDI document produced by the Microdata Management Toolkit or Nesstar Publichser 3.x
		Other supported values: ddp=World Bank Data Development Platform
	-->
	
	<!-- THIS SECTION IS ABOUT CONDITIONAL SETTINGS FOR THE CODEBOOK -->
	
   <!--	 To avoid empty pages; use a huge chunksize for subsets -->
   <xsl:variable name="chunkSize">
      <xsl:choose>
         <xsl:when test="($numberOfVars &gt; 0 )"><xsl:value-of select="1000"/></xsl:when>
         <xsl:otherwise>50</xsl:otherwise>
      </xsl:choose>
   </xsl:variable> 
	
	<!-- If totalt amount of variables or given subsetamount exceeds given max, then dont show extensive variable desc -->
	<xsl:variable name="show-variables-description">
		<xsl:choose>
			<xsl:when test="(count(/ddi:codeBook/ddi:dataDscr/ddi:var) &gt; $maxVars and $numberOfVars &lt; 1 )">0</xsl:when>
			<xsl:when test="($numberOfVars &gt; $maxVars)">0</xsl:when>
		   <xsl:when test="(count(/ddi:codeBook/ddi:dataDscr/ddi:var) = 0)">0</xsl:when>
			<xsl:otherwise>1</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
   
   <!-- Show variable groups only if there is any -->
   <xsl:variable name="showVariableGroups">
      <xsl:choose>
         <xsl:when test="$showVariableGroupsParam = 1 and count(/ddi:codeBook/ddi:dataDscr/ddi:varGrp) &gt; 0">1</xsl:when>
         <xsl:otherwise>0</xsl:otherwise>
      </xsl:choose>
   </xsl:variable>
   
	<!-- Show variable list if showing groups are disabled -->
	<xsl:variable name="show-variables-list">
		<xsl:choose>
			<xsl:when test="$showVariableGroups = 1">0</xsl:when>
			<xsl:otherwise>1</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	
	<xsl:variable name="ddi-flavor">
		<xsl:choose>
			<xsl:when
				test="count(/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:software[ contains( . , 'DDP' ) ])"
				>ddp</xsl:when>
			<xsl:when
				test="count(/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:software[ contains( . , 'Nesstar' ) ])"
				>toolkit</xsl:when>
			<xsl:when
				test="count(/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:software[ contains( . , 'Metadata Editor' ) ])"
				>toolkit</xsl:when>
			<xsl:otherwise>toolkit</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<!-- title -->
	<xsl:variable name="survey-title">
		<xsl:value-of
			select="normalize-space(/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:titl)"/>
		<!-- abbreviation is stored in the altTitl element -->
		<xsl:if test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:altTitl">
			(<xsl:value-of
				select="normalize-space(/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:altTitl)"
			/>)</xsl:if>
	</xsl:variable>
	<!-- geography-->
	<xsl:variable name="geography">
		<xsl:for-each select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:nation">
			<xsl:if test="position()>1">
				<xsl:text>, </xsl:text>
			</xsl:if>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:for-each>
	</xsl:variable>
	<!-- time -->
	<!-- year from is the first data collection mode element with a 'start' event -->
	<!-- ToDO: collDate isnt always present, should test and possibly use /ddi:timePrd[@date] -->
	<xsl:variable name="yearFrom"
		select="substring(/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate[@event='start'][1]/@date,1,4)"/>
	<!-- year to is the last data collection mode element with an 'end' event -->
	<xsl:variable name="yearToCount"
		select="count(/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate[@event='end'])"/>
	<xsl:variable name="yearTo"
		select="substring(/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate[@event='end'][$yearToCount]/@date,1,4)"/>
	<xsl:variable name="time">
		<xsl:if test="$yearFrom">
			<xsl:value-of select="$yearFrom"/>
			<xsl:if test="$yearTo &gt; $yearFrom">
				<xsl:text>-</xsl:text>
				<xsl:value-of select="$yearTo"/>
			</xsl:if>
		</xsl:if>
	</xsl:variable>
	
	<!-- If timeperiods returns empty, use timePrd instead ToDo: might not bed needed -->
	<xsl:variable name="timeProduced"
		select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:timePrd/@date"/>
	
	<!--
		variables for section existence
	-->
	<xsl:variable name="show-scope-and-coverage">
		<xsl:choose>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:notes">1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:keyword"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:geogCover"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:universe"
				>1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="show-producers-and-sponsors">
		<xsl:choose>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:producer"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:fundAg"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:othId"
				>1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="show-sampling">
		<xsl:choose>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='sampling']"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:deviat"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:respRate"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:weight"
				>1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="show-data-collection">
		<xsl:choose>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:timePrd"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='collection']"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='processing']"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='cleaning']"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collSitu"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:resInstru"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:dataCollector"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:actMin"
				>1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="show-data-processing-and-appraisal">
		<xsl:choose>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:cleanOps"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:EstSmpErr"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:dataAppr"
				>1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="show-accessibility">
		<xsl:choose>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:contact"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distrbtr"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:contact"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:confDec"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:conditions"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:citReq"
				>1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="show-rights-and-disclaimer">
		<xsl:choose>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:disclaimer"
				>1</xsl:when>
			<xsl:when test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:copyright"
				>1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<!--
		ROOT TEMPLATE
	-->
	<xsl:template match="/">
		<!-- FO ROOT -->
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<!--
				LAYOUT MASTER
			-->
			<fo:layout-master-set>
				<fo:simple-page-master master-name="default-page" page-height="11in"
					page-width="8.5in" margin-left="0.7in" margin-right="0.7in" margin-top="0.3in"
					margin-bottom="0.3in">
					<fo:region-body margin-top="0.5in" margin-bottom="0.5in"
						region-name="xsl-region-body"/>
					<fo:region-before extent="0.5in" region-name="xsl-region-before"/>
					<fo:region-after extent="0.5in" region-name="xsl-region-after"/>
				</fo:simple-page-master>
				<fo:simple-page-master master-name="landscape-page" page-height="8.5in"
					page-width="11in" margin-left="0.7in" margin-right="0.7in" margin-top="0.3in"
					margin-bottom="0.3in">
					<fo:region-body margin-top="0.5in" margin-bottom="0.5in"
						region-name="xsl-region-body"/>
					<fo:region-before extent="0.5in" region-name="xsl-region-before"/>
					<fo:region-after extent="0.5in" region-name="xsl-region-after"/>
				</fo:simple-page-master>
			</fo:layout-master-set>
			<!--
				OUTLINE / BOOKMARKS
			-->
			<fo:bookmark-tree>
				<xsl:if test="$show-cover-page = 1">
					<fo:bookmark internal-destination="cover-page">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Cover_Page']"/>
						</fo:bookmark-title>
					</fo:bookmark>
				</xsl:if>
				<xsl:if test="$show-metadata-info = 1">
					<fo:bookmark internal-destination="metadata-info">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Document_Information']"/>
						</fo:bookmark-title>
					</fo:bookmark>
				</xsl:if>
				<xsl:if test="$show-toc = 1">
					<fo:bookmark internal-destination="toc">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Table_of_Contents']"/>
						</fo:bookmark-title>
					</fo:bookmark>
				</xsl:if>
				<xsl:if test="$show-overview = 1">
					<!-- Overview bookmarks -->
					<fo:bookmark internal-destination="overview">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Overview']"/>
						</fo:bookmark-title>
						<xsl:if test="$show-scope-and-coverage = 1">
							<fo:bookmark internal-destination="scope-and-coverage">
								<fo:bookmark-title>
									<xsl:value-of select="$msg/*/entry[@key='Scope_and_Coverage']"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:if>
						<xsl:if test="$show-producers-and-sponsors = 1">
							<fo:bookmark internal-destination="producers-and-sponsors">
								<fo:bookmark-title>
									<xsl:value-of select="$msg/*/entry[@key='Producers_and_Sponsors']"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:if>
						<xsl:if test="$show-sampling = 1">
							<fo:bookmark internal-destination="sampling">
								<fo:bookmark-title>
									<xsl:value-of select="$msg/*/entry[@key='Sampling']"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:if>
						<xsl:if test="$show-data-collection = 1">
							<fo:bookmark internal-destination="data-collection">
								<fo:bookmark-title>
									<xsl:value-of select="$msg/*/entry[@key='Data_Collection']"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:if>
						<xsl:if test="$show-data-processing-and-appraisal = 1">
							<fo:bookmark internal-destination="data-processing-and-appraisal">
								<fo:bookmark-title>
									<xsl:value-of
										select="$msg/*/entry[@key='Data_Processing_and_Appraisal']"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:if>
						<xsl:if test="$show-accessibility= 1">
							<fo:bookmark internal-destination="accessibility">
								<fo:bookmark-title>
									<xsl:value-of select="$msg/*/entry[@key='Accessibility']"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:if>
						<xsl:if test="$show-rights-and-disclaimer = 1">
							<fo:bookmark internal-destination="rights-and-disclaimer">
								<fo:bookmark-title>
									<xsl:value-of select="$msg/*/entry[@key='Rights_and_Disclaimer']"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:if>
					</fo:bookmark>
				</xsl:if>
				<!-- file description bookmarks -->
				<xsl:if test="$show-files-description = 1">
					<fo:bookmark internal-destination="files-description">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Files_Description']"/>
						</fo:bookmark-title>
						<xsl:for-each select="/ddi:codeBook/ddi:fileDscr">
							<xsl:variable name="fileDscrId">
								<xsl:choose>
									<xsl:when test="@ID">
										<xsl:value-of select="@ID" />
									</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
								</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>		
							<fo:bookmark internal-destination="file-{$fileDscrId}">
								<fo:bookmark-title>
									<xsl:apply-templates select="ddi:fileTxt/ddi:fileName"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:for-each>
					</fo:bookmark>
				</xsl:if>
				<!-- Var groups bookmarks -->
				<xsl:if test="$showVariableGroups = 1">
					<fo:bookmark internal-destination="variables-groups">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Variables_Groups']"/>
						</fo:bookmark-title>
						<xsl:for-each select="/ddi:codeBook/ddi:dataDscr/ddi:varGrp">
							<xsl:if test="contains($subsetGroups,concat(',',@ID,',')) or string-length($subsetGroups)=0">
								<fo:bookmark internal-destination="vargrp-{@ID}">
									<fo:bookmark-title>
										<xsl:value-of select="normalize-space(ddi:labl)"/>
									</fo:bookmark-title>
								</fo:bookmark>
							</xsl:if>
						</xsl:for-each>
					</fo:bookmark>
				</xsl:if>
				<!-- variables list bookmarks -->
				<xsl:if test="$show-variables-list = 1">
					<fo:bookmark internal-destination="variables-list">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Variables_List']"/>
						</fo:bookmark-title>
						<xsl:for-each select="/ddi:codeBook/ddi:fileDscr">
							<xsl:variable name="fileDscrId">
								<xsl:choose>
									<xsl:when test="@ID">
										<xsl:value-of select="@ID" />
									</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
								</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>		
							<fo:bookmark internal-destination="varlist-{$fileDscrId}">
								<fo:bookmark-title>
									<xsl:apply-templates select="ddi:fileTxt/ddi:fileName"/>
								</fo:bookmark-title>
							</fo:bookmark>
						</xsl:for-each>
					</fo:bookmark>
				</xsl:if>
				<!-- variables description bookmarks -->
				<xsl:if test="$show-variables-description= 1">
					<fo:bookmark internal-destination="variables-description">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Variables_Description']"/>
						</fo:bookmark-title>
						<xsl:for-each select="/ddi:codeBook/ddi:fileDscr">
							<xsl:variable name="fileId">
								<xsl:choose>
									<xsl:when test="@ID">
										<xsl:value-of select="@ID" />
									</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
								</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>		
							<fo:bookmark internal-destination="vardesc-{$fileId}">
								<fo:bookmark-title>
									<xsl:apply-templates select="ddi:fileTxt/ddi:fileName"/>
								</fo:bookmark-title>
								<!--  xsl:variable name="fileId" select="@ID"/-->
								<xsl:for-each select="/ddi:codeBook/ddi:dataDscr/ddi:var[ddi:location/@fileid=$fileId]">
									<xsl:if test="contains($subsetVars,concat(',',@ID,',')) or string-length($subsetVars)=0 ">
										<fo:bookmark internal-destination="var-{@ID}">
											<fo:bookmark-title>
												<xsl:apply-templates select="@name"/>
												<xsl:if test="normalize-space(ddi:labl)">
													<xsl:text>: </xsl:text>
													<xsl:call-template name="trim">
														<xsl:with-param name="s" select="ddi:labl"/>
													</xsl:call-template>
												</xsl:if>
											</fo:bookmark-title>
										</fo:bookmark>
									</xsl:if>
								</xsl:for-each>
							</fo:bookmark>
						</xsl:for-each>
					</fo:bookmark>
				</xsl:if>
				<!-- documentation bookmark -->
				<xsl:if test="$show-documentation = 1 and normalize-space($rdf-file)">
					<fo:bookmark internal-destination="documentation">
						<fo:bookmark-title>
							<xsl:value-of select="$msg/*/entry[@key='Documentation']"/>
						</fo:bookmark-title>
					</fo:bookmark>
				</xsl:if>
			</fo:bookmark-tree>
			
			<!--
				PAGE SEQUENCE: COVER PAGE
			-->
			<xsl:if test="$show-cover-page = 1">
				<fo:page-sequence master-reference="default-page" font-family="{$font-family}"
					font-size="10pt">
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:block id="cover-page">
							<!-- geography -->
							<fo:block font-size="14pt" font-weight="bold" space-before="0.5in"
								text-align="center" space-after="0.2in">
								<xsl:value-of select="$geography"/>
							</fo:block>
							<!-- agency/ies -->
							<xsl:if test="$show-cover-page-producer = 1">
								<xsl:for-each
									select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty">
									<fo:block font-size="14pt" font-weight="bold"
										space-before="0.0in" text-align="center" space-after="0.0in">
										<xsl:call-template name="trim">
											<xsl:with-param name="s">
												<xsl:value-of select="."/>
											</xsl:with-param>
										</xsl:call-template>
										<xsl:if test="@affiliation">, <xsl:value-of
											select="@affiliation"/></xsl:if>
									</fo:block>
								</xsl:for-each>
							</xsl:if>
							<!-- title -->
							<fo:block font-size="18pt" font-weight="bold" space-before="0.5in"
								text-align="center" space-after="0.0in">
								<xsl:value-of
									select="normalize-space(/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:titl)"
								/>
							</fo:block>
							<!-- Report title -->
							<fo:block font-size="16pt" font-weight="bold" space-before="1.0in"
								text-align="center" space-after="0.0in">
								<xsl:value-of select="$report-title"/>
							</fo:block>
							<!-- Date -->
							<fo:block font-size="12pt" space-before="5.0in" text-align="center"
								space-after="0.1in">
								<fo:block>
									<xsl:call-template name="isodate-long">
										<xsl:with-param name="isodate" select="$report-date"/>
									</xsl:call-template>
								</fo:block>
							</fo:block>
						</fo:block>
					</fo:flow>
				</fo:page-sequence>
			</xsl:if>
			<!--
				PAGE SEQUENCE: METADATA INFORMATION
			-->
			<xsl:if test="$show-metadata-info = 1">
				<fo:page-sequence master-reference="default-page" font-family="{$font-family}"
					font-size="10pt">
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:block id="metadata-info"/>
						<xsl:if test="boolean($show-metadata-production)">
							<fo:block id="metadata-production" font-size="18pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Metadata_Production']"/>
							</fo:block>
							<fo:table table-layout="fixed" width="100%" space-before="0.0in"
								space-after="0.2in">
								<fo:table-column column-width="proportional-column-width(20)"/>
								<fo:table-column column-width="proportional-column-width(80)"/>
								<fo:table-body>
									<!-- Metadata Producer(s) -->
									<xsl:if
										test="/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:producer">
										<fo:table-row>
											<fo:table-cell font-weight="bold"
												border="{$default-border}" padding="{$cell-padding}">
												<fo:block>
													<xsl:value-of
														select="$msg/*/entry[@key='Metadata_Producers']"/>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell border="{$default-border}"
												padding="{$cell-padding}">
												<xsl:apply-templates
													select="/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:producer"
												/>
											</fo:table-cell>
										</fo:table-row>
									</xsl:if>
									<!-- Metadata Production Date -->
									<xsl:if
										test="/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:prodDate">
										<fo:table-row>
											<fo:table-cell font-weight="bold"
												border="{$default-border}" padding="{$cell-padding}">
												<fo:block>
													<xsl:value-of
														select="$msg/*/entry[@key='Production_Date']"/>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell border="{$default-border}"
												padding="{$cell-padding}">
												<fo:block>
													<xsl:call-template name="isodate-long">
														<xsl:with-param name="isodate"
															select="normalize-space(/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:prodDate)"
														/>
													</xsl:call-template>
												</fo:block>
											</fo:table-cell>
										</fo:table-row>
									</xsl:if>
									<!-- Metadata Version -->
									<xsl:if
										test="/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:verStmt/ddi:version">
										<fo:table-row>
											<fo:table-cell font-weight="bold"
												border="{$default-border}" padding="{$cell-padding}">
												<fo:block>
													<xsl:value-of
														select="$msg/*/entry[@key='Version']"/>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell border="{$default-border}"
												padding="{$cell-padding}">
												<xsl:apply-templates
													select="/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:verStmt/ddi:version"
												/>
											</fo:table-cell>
										</fo:table-row>
									</xsl:if>
									<!-- Metadata ID -->
									<xsl:if
										test="/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:titlStmt/ddi:IDNo">
										<fo:table-row>
											<fo:table-cell font-weight="bold"
												border="{$default-border}" padding="{$cell-padding}">
												<fo:block>
													<xsl:value-of
														select="$msg/*/entry[@key='Identification']"/>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell border="{$default-border}"
												padding="{$cell-padding}">
												<xsl:apply-templates
													select="/ddi:codeBook/ddi:docDscr/ddi:citation/ddi:titlStmt/ddi:IDNo"
												/>
											</fo:table-cell>
										</fo:table-row>
									</xsl:if>
									<fo:table-row>
										<fo:table-cell><fo:block> </fo:block></fo:table-cell>
									</fo:table-row>
								</fo:table-body>
							</fo:table>
						</xsl:if>
						<xsl:if test="normalize-space($report-acknowledgments)">
							<fo:block font-size="18pt" font-weight="bold" space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Acknowledgments']"/>
							</fo:block>
							<fo:block font-size="10pt" space-after="0.2in">
								<xsl:value-of select="$report-acknowledgments"/>
							</fo:block>
						</xsl:if>
						<xsl:if test="normalize-space($report-notes)">
							<fo:block font-size="18pt" font-weight="bold" space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Notes']"/>
							</fo:block>
							<fo:block font-size="10pt" space-after="0.2in">
								<xsl:value-of select="$report-notes"/>
							</fo:block>
						</xsl:if>
					</fo:flow>
				</fo:page-sequence>
			</xsl:if>
			<!--
				PAGE SEQUENCE: TOC
			-->
			<xsl:if test="$show-toc = 1">
				<fo:page-sequence master-reference="default-page" font-family="{$font-family}"
					font-size="10pt">
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:block id="toc" font-size="18pt" font-weight="bold" space-before="0.5in"
							text-align="center" space-after="0.1in">
							<xsl:value-of select="$msg/*/entry[@key='Table_of_Contents']"/>
						</fo:block>
						<fo:block margin-left="0.5in" margin-right="0.5in">
							<xsl:if test="$show-overview = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="overview"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Overview']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="overview"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-scope-and-coverage = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="scope-and-coverage"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Scope_and_Coverage']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="scope-and-coverage"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-producers-and-sponsors = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="producers-and-sponsors"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Producers_and_Sponsors']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="producers-and-sponsors"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-sampling = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="sampling"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Sampling']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="sampling"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-data-collection = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="data-collection"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Data_Collection']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="data-collection"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-data-processing-and-appraisal = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link
										internal-destination="data-processing-and-appraisal"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Data_Processing_and_Appraisal']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation
											ref-id="data-processing-and-appraisal"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-accessibility= 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="accessibility"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Accessibility']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="accessibility"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-rights-and-disclaimer = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="rights-and-disclaimer"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Rights_and_Disclaimer']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="rights-and-disclaimer"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-files-description = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="files-description"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Files_Description']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="files-description"/>
									</fo:basic-link>
									<xsl:for-each select="/ddi:codeBook/ddi:fileDscr">
											<xsl:variable name="fileDscrId">
												<xsl:choose>
													<xsl:when test="@ID">
														<xsl:value-of select="@ID" />
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
													</xsl:otherwise>
												</xsl:choose>
											</xsl:variable>		
										<fo:block margin-left="0.7in" font-size="10pt"
											text-align-last="justify">
											<fo:basic-link internal-destination="file-{$fileDscrId}"
												text-decoration="underline" color="blue">
												<xsl:apply-templates
													select="ddi:fileTxt/ddi:fileName"/>
												<fo:leader leader-pattern="dots"/>
												<fo:page-number-citation
													ref-id="file-{$fileDscrId}"/>
											</fo:basic-link>
										</fo:block>
									</xsl:for-each>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-variables-list = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="variables-list"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Variables_List']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="variables-list"/>
									</fo:basic-link>
									<xsl:for-each select="/ddi:codeBook/ddi:fileDscr">
											<xsl:variable name="varfileId">
												<xsl:choose>
													<xsl:when test="@ID">
														<xsl:value-of select="@ID" />
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
													</xsl:otherwise>
												</xsl:choose>
											</xsl:variable>		
										<fo:block margin-left="0.7in" font-size="10pt"
											text-align-last="justify">
											<fo:basic-link internal-destination="varlist-{$varfileId}"
												text-decoration="underline" color="blue">
												<xsl:apply-templates
													select="ddi:fileTxt/ddi:fileName"/>
												<fo:leader leader-pattern="dots"/>
												<fo:page-number-citation
													ref-id="varlist-{$varfileId}"/>
											</fo:basic-link>
										</fo:block>
									</xsl:for-each>
								</fo:block>
							</xsl:if>
							<xsl:if test="$showVariableGroups = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="variables-groups"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Variables_Groups']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="variables-groups"/>
									</fo:basic-link>
									<xsl:for-each select="/ddi:codeBook/ddi:dataDscr/ddi:varGrp">
										<!-- Show group if its part of subset OR no subset is defined -->
										<xsl:if test="contains($subsetGroups,concat(',',@ID,',')) or string-length($subsetGroups)=0">
											<fo:block margin-left="0.7in" font-size="10pt"
												text-align-last="justify">
												<fo:basic-link internal-destination="vargrp-{@ID}"
													text-decoration="underline" color="blue">
													<xsl:value-of select="normalize-space(ddi:labl)"/>
													<fo:leader leader-pattern="dots"/>
													<fo:page-number-citation ref-id="vargrp-{@ID}"/>
												</fo:basic-link>
											</fo:block>
										</xsl:if>
									</xsl:for-each>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-variables-description = 1">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="variables-description"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Variables_Description']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="variables-description"/>
									</fo:basic-link>
									<xsl:for-each select="/ddi:codeBook/ddi:fileDscr">
										<fo:block margin-left="0.7in" font-size="10pt"
											text-align-last="justify">
											<xsl:variable name="fileId">
												<xsl:choose>
													<xsl:when test="@ID">
														<xsl:value-of select="@ID" />
													</xsl:when>
													<xsl:otherwise>
														<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
													</xsl:otherwise>
												</xsl:choose>
											</xsl:variable>		
											<fo:basic-link internal-destination="vardesc-{$fileId}"
												text-decoration="underline" color="blue">
												<xsl:apply-templates
													select="ddi:fileTxt/ddi:fileName"/>
												<fo:leader leader-pattern="dots"/>
												<fo:page-number-citation
													ref-id="vardesc-{$fileId}"/>
											</fo:basic-link>
										</fo:block>
									</xsl:for-each>
								</fo:block>
							</xsl:if>
							<xsl:if test="$show-documentation">
								<fo:block font-size="10pt" text-align-last="justify">
									<fo:basic-link internal-destination="documentation"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Documentation']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation"/>
									</fo:basic-link>
								</fo:block>
							</xsl:if>
						</fo:block>
					</fo:flow>
				</fo:page-sequence>
			</xsl:if>
			<!--
				PAGE SEQUENCE: OVERVIEW
			-->
			<xsl:if test="$show-overview = 1">
				<fo:page-sequence master-reference="default-page"
					initial-page-number="{$report-start-page-number}" font-family="{$font-family}"
					font-size="10pt">
					<!-- Page Header -->
					<xsl:call-template name="header">
						<xsl:with-param name="section">
							<xsl:value-of select="$msg/*/entry[@key='Overview']"/>
						</xsl:with-param>
					</xsl:call-template>
					<!-- Page Footer-->
					<xsl:call-template name="footer"/>
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:table table-layout="fixed" width="100%">
							<fo:table-column column-width="proportional-column-width(20)"/>
							<fo:table-column column-width="proportional-column-width(80)"/>
							<fo:table-body>
								<!--
									TITLE HEADER
								-->
								<fo:table-row background-color="{$color-gray3}">
									<fo:table-cell number-columns-spanned="2"
										border="{$default-border}" padding="{$cell-padding}">
										
										<!-- Moving time and geo further down ToDo:remove this	-->
										<!-- Country and Year -->
										<!--<fo:block id="overview" font-size="12pt" font-weight="bold">-->
										
										<!--<xsl:value-of select="$geography"/>-->
										<!--<xsl:if test=" string-length($time)&gt;3"> (<xsl:value-of select="$time"/>)
											</xsl:if>-->
										<!--</fo:block>-->
										
										<!-- Survey title and abbreviation -->
										<fo:block font-size="14pt" font-weight="bold">
											<xsl:value-of select="$survey-title"/>
										</fo:block>
										<!-- translated/parallel title -->
										<xsl:if
											test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:parTitl">
											<fo:block font-size="12pt" font-weight="bold"
												font-style="italic">
												<xsl:value-of
													select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:parTitl"
												/>
											</fo:block>
										</xsl:if>
									</fo:table-cell>
								</fo:table-row>
								<!-- SEPARATOR -->
								<fo:table-row height="0.2in">
									<fo:table-cell number-columns-spanned="2">
										<fo:block/>
									</fo:table-cell>
								</fo:table-row>
								<!--<fo:table-row height="0.2in" border-top="{$default-border}"
									border-bottom="{$default-border}">
									<fo:table-cell number-columns-spanned="2">
									<fo:block/>
									</fo:table-cell>
									</fo:table-row>-->
								<!--
									OVERVIEW SECTION (Identification, Version and Overview )
								-->
								<fo:table-row background-color="{$color-gray1}"
									keep-with-next="always">
									<fo:table-cell number-columns-spanned="2"
										border="{$default-border}" padding="{$cell-padding}">
										<fo:block id="overview" font-size="12pt" font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Overview']"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
								<!--
									TYPE
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:serStmt/ddi:serName">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of select="$msg/*/entry[@key='Type']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:serStmt/ddi:serName"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									IDENTIFICATION
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:IDNo">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Identification']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:IDNo"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									VERSION
								-->
								<xsl:for-each
									select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:verStmt/ddi:version">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of select="$msg/*/entry[@key='Version']"
												/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<!-- production date & description -->
											<xsl:for-each
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:verStmt/ddi:version">
												<xsl:if test="@date">
													<fo:block><xsl:value-of
														select="$msg/*/entry[@key='Production_Date']"/>:
														<xsl:value-of select="@date"/>
													</fo:block>
												</xsl:if>
												<xsl:apply-templates select="."/>
											</xsl:for-each>
											<xsl:for-each
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:verStmt/ddi:notes">
												<fo:block text-decoration="underline">
													<xsl:value-of select="$msg/*/entry[@key='Notes']"
													/>
												</fo:block>
												<xsl:apply-templates select="."/>
											</xsl:for-each>
										</fo:table-cell>
									</fo:table-row>
								</xsl:for-each>
								<!--
									SERIES
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:serStmt/ddi:serInfo">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of select="$msg/*/entry[@key='Series']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:serStmt/ddi:serInfo"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									ABSTRACT
								-->
								<xsl:if test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:abstract">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of select="$msg/*/entry[@key='Abstract']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:abstract"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									KIND OF DATA
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:dataKind">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Kind_of_Data']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:dataKind"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									UNIT OF ANALYSIS
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:anlyUnit">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Unit_of_Analysis']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:anlyUnit"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- SEPARATOR -->
								<!--<fo:table-row height="0.2in" border-top="{$default-border}"
									border-bottom="{$default-border}">-->
								<fo:table-row height="0.2in">
									<fo:table-cell number-columns-spanned="2">
										<fo:block/>
									</fo:table-cell>
								</fo:table-row>
								<!--
									*** SCOPE & COVERAGE SECTION (Scope, Coverage) ***
								-->
								<xsl:if test="$show-scope-and-coverage = 1">
									<fo:table-row
										background-color="{$color-gray1}" keep-with-next="always">
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block id="scope-and-coverage" font-size="12pt" font-weight="bold">
												<xsl:value-of
													select="$msg/*/entry[@key='Scope_and_Coverage']"/>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									SCOPE
								-->
								<xsl:if test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:notes">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of select="$msg/*/entry[@key='Scope']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:notes"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									KEYWORDS
									-								 -->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:keyword">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of select="$msg/*/entry[@key='Keywords']"
												/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:for-each
													select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:keyword">
													<xsl:if test="position()>1">, </xsl:if>
													<xsl:value-of select="normalize-space(.)"/>
												</xsl:for-each>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									TOPIC CLASSIFICATIONS (Template v1.2, June 2006)
									-								 -->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:topcClas">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of select="$msg/*/entry[@key='Topics']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:for-each
													select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:topcClas">
													<xsl:if test="position()>1">, </xsl:if>
													<xsl:value-of select="normalize-space(.)"/>
												</xsl:for-each>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									GEO COVERAGE
								-->	
								<xsl:if test="string-length($time)&gt;3 or string-length($timeProduced)&gt;3">
									<fo:table-row>
										<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of select="$msg/*/entry[@key='Time_Periods']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
											<xsl:choose>
												<xsl:when test="string-length($time)&gt;3">
													<fo:block>
														<xsl:value-of select="$time"/>
													</fo:block>
												</xsl:when>
												<xsl:when test="string-length($timeProduced)&gt;3">
													<fo:block>
														<xsl:value-of select="$timeProduced"/>
													</fo:block>
												</xsl:when>
											</xsl:choose>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								
								<fo:table-row>
									<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
										<fo:block font-weight="bold" text-decoration="underline">
											<xsl:value-of select="$msg/*/entry[@key='Countries']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
										<fo:block>
											<xsl:value-of select="$geography"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
								
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:geogCover">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Geographic_Coverage']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:geogCover"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									UNIVERSE
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:universe">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of select="$msg/*/entry[@key='Universe']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:universe"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- SEPARATOR -->
								<!--<fo:table-row height="0.2in" border-top="{$default-border}"
									border-bottom="{$default-border}">-->
								<fo:table-row height="0.2in">
									<fo:table-cell number-columns-spanned="2">
										<fo:block/>
									</fo:table-cell>
								</fo:table-row>
								<!--
									*** PRODUCERS AND SPONSORS SECTION ***
								-->
								<xsl:if test="$show-producers-and-sponsors = 1">
									<fo:table-row
										background-color="{$color-gray1}" keep-with-next="always">
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block id="producers-and-sponsors" font-size="12pt" font-weight="bold">
												<xsl:value-of
													select="$msg/*/entry[@key='Producers_and_Sponsors']"
												/>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									PRIMARY INVESTIGATOR(S)
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Primary_Investigators']"
												/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									OTHER PRODUCER(S)
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:producer">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Other_Producers']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:producer"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									FUNDING
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:fundAg">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Funding_Agencies']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:fundAg"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									OTHER ACKNOWLEDGEMENTS
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:othId">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Other_Acknowledgments']"
												/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:othId"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- SEPARATOR -->
								<!--<fo:table-row height="0.2in" border-top="{$default-border}"
									border-bottom="{$default-border}">-->
								<fo:table-row height="0.2in">
									<fo:table-cell number-columns-spanned="2">
										<fo:block/>
									</fo:table-cell>
								</fo:table-row>
								<!--
									*** SAMPLING SECTION ***
								-->
								<xsl:if test="$show-sampling = 1">
									<fo:table-row background-color="{$color-gray1}">
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block id="sampling" font-size="12pt" font-weight="bold">
												<xsl:value-of select="$msg/*/entry[@key='Sampling']"
												/>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									SAMPLING PROCEDURE
								-->
								<xsl:choose>
									<xsl:when test=" $ddi-flavor='ddp' ">
										<!-- DDP: stores the sampling method in <sampProc> and comments in a <notes> element with subject='sampling' -->
										<xsl:if
											test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc">
											<fo:table-row>
												<fo:table-cell font-weight="bold"
													border="{$default-border}"
													padding="{$cell-padding}">
													<fo:block font-weight="bold">
														<xsl:value-of
															select="$msg/*/entry[@key='Sampling']"/>
													</fo:block>
												</fo:table-cell>
												<fo:table-cell border="{$default-border}"
													padding="{$cell-padding}">
													<xsl:apply-templates
														select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc"
													/>
												</fo:table-cell>
											</fo:table-row>
										</xsl:if>
										<xsl:if
											test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='sampling']">
											<fo:table-row>
												<fo:table-cell number-columns-spanned="2"
													border="{$default-border}"
													padding="{$cell-padding}">
													<fo:block font-weight="bold"
														text-decoration="underline">
														<xsl:value-of
															select="$msg/*/entry[@key='Sampling_Notes']"/>
													</fo:block>
													<xsl:apply-templates
														select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='sampling']"
													/>
												</fo:table-cell>
											</fo:table-row>
										</xsl:if>
									</xsl:when>
									<xsl:otherwise>
										<!-- Toolkit: the sampling information is free text in a the <sampProc> element -->
										<xsl:if
											test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc">
											<fo:table-row>
												<fo:table-cell number-columns-spanned="2"
													border="{$default-border}"
													padding="{$cell-padding}">
													<fo:block font-weight="bold"
														text-decoration="underline">
														<xsl:value-of
															select="$msg/*/entry[@key='Sampling_Procedure']"/>
													</fo:block>
													<xsl:apply-templates
														select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc"
													/>
												</fo:table-cell>
											</fo:table-row>
										</xsl:if>
									</xsl:otherwise>
								</xsl:choose>
								<!--
									DEVIATIONS FROM SAMPLE
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:deviat">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Deviations_from_Sample_Design']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:deviat"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									RESPONSE RATE
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:respRate">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Response_Rate']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:respRate"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									WEIGHTING
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:weight">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Weighting']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:weight"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- SEPARATOR -->
								<!--<fo:table-row height="0.2in" border-top="{$default-border}"
									border-bottom="{$default-border}">-->
								<fo:table-row height="0.2in">
									<fo:table-cell number-columns-spanned="2">
										<fo:block/>
									</fo:table-cell>
								</fo:table-row>
								<!--
									*** DATA COLLECTION SECTION ***
								-->
								<xsl:if test="$show-data-collection = 1">
									<fo:table-row
										background-color="{$color-gray1}" keep-with-next="always">
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block id="data-collection" font-size="12pt" font-weight="bold">
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Collection']"/>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									DATA COLLECTION DATES
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Collection_Dates']"
												/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									TIME PERIODS
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:timePrd">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Time_Periods']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:timePrd"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									DATA COLLECTION MODE
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Collection_Mode']"
												/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									DATA COLLECTION NOTES
								-->
								<!-- DDP Data Collection Notes -->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='collection']">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Collection_Notes']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='collection']"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- DDP Data Processing  Notes -->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='processing']">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Processing_Notes']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='collection']"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- DDP Data Cleaning  Notes -->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='cleaning']">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Cleaning_Notes']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes[@subject='collection']"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- Default -->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collSitu">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Collection_Notes']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collSitu"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									QUESTIONNAIRES
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:resInstru">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Questionnaires']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:resInstru"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									DATA COLLECTORS
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:dataCollector">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Collectors']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:dataCollector"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									SUPERVISION
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:actMin">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Supervision']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:actMin"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- SEPARATOR -->
								<!--<fo:table-row height="0.2in" border-top="{$default-border}"
									border-bottom="{$default-border}">-->
								<fo:table-row height="0.2in">
									<fo:table-cell number-columns-spanned="2">
										<fo:block/>
									</fo:table-cell>
								</fo:table-row>
								<!--
									*** DATA PROCESSING AND APPRAISAL SECTION ***
								-->
								<xsl:if test="$show-data-processing-and-appraisal = 1">
									<fo:table-row background-color="{$color-gray1}" keep-with-next="always">
										<fo:table-cell number-columns-spanned="2" border="{$default-border}" padding="{$cell-padding}">
											<fo:block id="data-processing-and-appraisal" font-size="12pt" font-weight="bold">
												<xsl:value-of select="$msg/*/entry[@key='Data_Processing_and_Appraisal']"/>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									DATA EDITING
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:cleanOps">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Data_Editing']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:cleanOps"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									OTHE PROCESSING (IHSN Template v1.2, June 2006)
								-->
								<xsl:if test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Other_Processing']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:notes"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									ESTIMATES OF SAMPLING ERROR
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:EstSmpErr">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Estimates_of_Sampling_Error']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:EstSmpErr"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									OTHER DATA APPRAISAL
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:dataAppr">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Other_Forms_of_Data_Appraisal']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:anlyInfo/ddi:dataAppr"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- SEPARATOR -->
								<!--<fo:table-row height="0.2in" border-top="{$default-border}"
									border-bottom="{$default-border}">-->
								<fo:table-row height="0.2in">
									<fo:table-cell number-columns-spanned="2">
										<fo:block/>
									</fo:table-cell>
								</fo:table-row>
								<!--
									*** ACCESSIBILITY SECTION ***
								-->
								<xsl:if test="$show-accessibility = 1">
									<fo:table-row background-color="{$color-gray1}" keep-with-next="always">
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block id="accessibility" font-size="12pt" font-weight="bold">
												<xsl:value-of
													select="$msg/*/entry[@key='Accessibility']"/>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									ACCESS AUTHORITY
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:contact">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Access_Authority']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:contact"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									CONTACTS
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:contact">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of select="$msg/*/entry[@key='Contacts']"
												/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:contact"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									DISTRIBUTOR
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distrbtr">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Distributors']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distrbtr"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									DEPOSITOR (DDP)
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:depositr">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Depositors']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:depositr"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									CONFIDENTIALITY
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:confDec">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Confidentiality']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:confDec"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									ACCESS CONDITIONS
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:conditions">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Access_Conditions']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:conditions"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									CITATION REQUIREMENTS
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:citReq">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Citation_Requirements']"
												/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:citReq"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!-- SEPARATOR -->
								<!--<fo:table-row height="0.2in" border-top="{$default-border}"
									border-bottom="{$default-border}">-->
								<fo:table-row height="0.2in">
									<fo:table-cell number-columns-spanned="2">
										<fo:block/>
									</fo:table-cell>
								</fo:table-row>
								<!--
									*** RIGHTS AND DISCLAIMER SECTION ***
								-->
								<xsl:if test="$show-rights-and-disclaimer = 1">
									<fo:table-row background-color="{$color-gray1}" keep-with-next="always">
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block id="rights-and-disclaimer" font-size="12pt" font-weight="bold">
												<xsl:value-of
													select="$msg/*/entry[@key='Rights_and_Disclaimer']"
												/>
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									DISCLAIMER
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:disclaimer">
									<fo:table-row>
										<fo:table-cell number-columns-spanned="2"
											border="{$default-border}" padding="{$cell-padding}">
											<fo:block font-weight="bold" text-decoration="underline">
												<xsl:value-of
													select="$msg/*/entry[@key='Disclaimer']"/>
											</fo:block>
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:disclaimer"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
								<!--
									COPYRIGHT
								-->
								<xsl:if
									test="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:copyright">
									<fo:table-row>
										<fo:table-cell font-weight="bold" border="{$default-border}"
											padding="{$cell-padding}">
											<fo:block>
												<xsl:value-of
													select="$msg/*/entry[@key='Copyright']"/>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell border="{$default-border}"
											padding="{$cell-padding}">
											<xsl:apply-templates
												select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:copyright"
											/>
										</fo:table-cell>
									</fo:table-row>
								</xsl:if>
							</fo:table-body>
						</fo:table>
					</fo:flow>
				</fo:page-sequence>
			</xsl:if>
			<!--
				PAGE SEQUENCE: FILES DESCRIPTION
			-->
			<xsl:if test="$show-files-description = 1">
				<fo:page-sequence master-reference="default-page" font-family="{$font-family}"
					font-size="10pt">
					<!-- Page Header -->
					<xsl:call-template name="header">
						<xsl:with-param name="section">
							<xsl:value-of select="$msg/*/entry[@key='Files_Description']"/>
						</xsl:with-param>
					</xsl:call-template>
					<!-- Page Footer-->
					<xsl:call-template name="footer"/>
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:block id="files-description" font-size="18pt" font-weight="bold"
							space-after="0.1in">
							<xsl:value-of select="$msg/*/entry[@key='Files_Description']"/>
						</fo:block>
						<!-- count -->
						<fo:block font-weight="bold">
							<xsl:value-of select="$msg/*/entry[@key='Dataset_contains']"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="count(/ddi:codeBook/ddi:fileDscr)"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="$msg/*/entry[@key='files']"/>
						</fo:block>
						<!-- FILES -->
						<xsl:apply-templates select="/ddi:codeBook/ddi:fileDscr"/>
					</fo:flow>
				</fo:page-sequence>
			</xsl:if>
			<!--
				PAGE SEQUENCE: VARIABLES LIST
			-->
			<xsl:if test="$show-variables-list = 1">
				<fo:page-sequence master-reference="{$show-variables-list-layout}"
					font-family="{$font-family}" font-size="10pt">
					<!-- Page Header -->
					<xsl:call-template name="header">
						<xsl:with-param name="section">
							<xsl:value-of select="$msg/*/entry[@key='Variables_List']"/>
						</xsl:with-param>
					</xsl:call-template>
					<!-- Page Footer-->
					<xsl:call-template name="footer"/>
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:block id="variables-list" font-size="18pt" font-weight="bold"
							space-after="0.1in">
							<xsl:value-of select="$msg/*/entry[@key='Variables_List']"/>
						</fo:block>
						<!--  count -->
						<fo:block font-weight="bold">
							<xsl:value-of select="$msg/*/entry[@key='Dataset_contains']"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="count(/ddi:codeBook/ddi:dataDscr/ddi:var)"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="$msg/*/entry[@key='variables']"/>
							<!--							<xsl:if test="string-length($subsetVars)!=0 "> <xsl:value-of select="$msg/*/entry[@key='ShowingSubset']"/></xsl:if>-->
						</fo:block>
						<!-- VARIABLES -->
						<xsl:apply-templates select="/ddi:codeBook/ddi:fileDscr"
							mode="variables-list"/>
					</fo:flow>
				</fo:page-sequence>
			</xsl:if>
			<!--
				PAGE SEQUENCE: VARIABLES GROUPS 
			-->
			<xsl:if test="$showVariableGroups = 1">
				<fo:page-sequence master-reference="default-page" font-family="{$font-family}"
					font-size="10pt">
					<!-- Page Header -->
					<xsl:call-template name="header">
						<xsl:with-param name="section">
							<xsl:value-of select="$msg/*/entry[@key='Variables_Groups']"/>
						</xsl:with-param>
					</xsl:call-template>
					<!-- Page Footer-->
					<xsl:call-template name="footer"/>
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:block id="variables-groups" font-size="18pt" font-weight="bold"
							space-after="0.1in">
							<xsl:value-of select="$msg/*/entry[@key='Variables_Groups']"/>
						</fo:block>
						<!-- count -->
						<fo:block font-weight="bold">
							<xsl:value-of select="$msg/*/entry[@key='Dataset_contains']"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="count(/ddi:codeBook/ddi:dataDscr/ddi:varGrp)"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="$msg/*/entry[@key='groups']"/>
							<xsl:if test="string-length($subsetVars)>0">
								<xsl:value-of select="$msg/*/entry[@key='ShowingSubset']"/>
								<xsl:value-of select="$numberOfGroups"/>
								<!--	<xsl:text> group(s)</xsl:text>-->
							</xsl:if>
						</fo:block>
						<!-- GROUPS -->
						<xsl:apply-templates select="/ddi:codeBook/ddi:dataDscr/ddi:varGrp"/>
					</fo:flow>
				</fo:page-sequence>
			</xsl:if>
			<!--
				PAGE SEQUENCE: VARIABLES DESCRIPTION
			-->
			<xsl:if test="$show-variables-description = 1">
				<fo:page-sequence master-reference="default-page" font-family="{$font-family}" font-size="10pt">
					<!-- Page Header -->
					<xsl:call-template name="header">
						<xsl:with-param name="section">
							<xsl:value-of select="$msg/*/entry[@key='Variables_Description']"/>
						</xsl:with-param>
					</xsl:call-template>
					<!-- Page Footer-->
					<xsl:call-template name="footer"/>
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:block id="variables-description" font-size="18pt" font-weight="bold"
							space-after="0.1in">
							<xsl:value-of select="$msg/*/entry[@key='Variables_Description']"/>
						</fo:block>
						<!--  count -->
						<fo:block font-weight="bold">
							<xsl:value-of select="$msg/*/entry[@key='Dataset_contains']"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="count(/ddi:codeBook/ddi:dataDscr/ddi:var)"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="$msg/*/entry[@key='variables']"/>
							<xsl:if test="string-length($subsetVars)>0">
								<xsl:value-of select="$msg/*/entry[@key='ShowingSubset']"/>
								<xsl:value-of select="$numberOfVars"/>
							</xsl:if>
						</fo:block>
					</fo:flow>
				</fo:page-sequence>
				<!-- VARIABLES -->
				<xsl:apply-templates select="/ddi:codeBook/ddi:fileDscr" mode="variables-description"/>
			</xsl:if>
			<!--
				PAGE SEQUENCE: DOCUMENTATION
			-->
			<xsl:if test="$show-documentation = 1 and normalize-space($rdf-file)">
				<fo:page-sequence master-reference="default-page" font-family="{$font-family}"
					font-size="10pt">
					<!-- Page Header -->
					<xsl:call-template name="header">
						<xsl:with-param name="section">
							<xsl:value-of select="$msg/*/entry[@key='Documentation']"/>
						</xsl:with-param>
					</xsl:call-template>
					<!-- Page Footer-->
					<xsl:call-template name="footer"/>
					<!-- Page Flow -->
					<fo:flow flow-name="xsl-region-body">
						<fo:block id="documentation" font-size="18pt" font-weight="bold"
							space-after="0.1in">
							<xsl:value-of select="$msg/*/entry[@key='Documentation']"/>
						</fo:block>
						<!--  get RDF document -->
						<xsl:variable name="rdf" select="document($rdf-file)"/>
						<!-- count documents by category -->
						<xsl:variable name="adm-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/adm]') ]) "/>
						<xsl:variable name="anl-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/anl]') ]) "/>
						<xsl:variable name="qst-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/qst]') ]) "/>
						<xsl:variable name="oth-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/oth]') ]) "/>
						<xsl:variable name="ref-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/ref]') ]) "/>
						<xsl:variable name="rep-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/rep]') ]) "/>
						<xsl:variable name="tec-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/tec]') ]) "/>
						<xsl:variable name="tbl-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[tbl') ]) "/>
						<xsl:variable name="prg-count"
							select="count($rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[prg') ]) "/>
						<xsl:variable name="unc-count"
							select="count($rdf/rdf:RDF/rdf:Description[not( contains(dc:type,'[doc') or contains(dc:type,'[tbl') or contains(dc:type,'[prg') ) ] )"/>
						<!--
							TOC
						-->
						<fo:block space-after="0.2in">
							<!-- report/analytrical -->
							<xsl:if test="$rep-count >0 or $anl-count > 0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-anl"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Reports_and_analytical_documents']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-anl"/>
									</fo:basic-link>
								</fo:block>
								<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes"
										select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/rep]')  or contains(dc:type,'[doc/anl]') ]"
									/>
								</xsl:call-template>
							</xsl:if>
							<!-- questionnaires -->
							<xsl:if test="$qst-count >0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-qst"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Questionnaires']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-qst"/>
									</fo:basic-link>
								</fo:block>
								<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes"
										select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/qst]')]"
									/>
								</xsl:call-template>
							</xsl:if>
							<!-- technical -->
							<xsl:if test="$tec-count >0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-tec"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Technical_documents']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-tec"/>
									</fo:basic-link>
								</fo:block>
								<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes"
										select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/tec]')]"
									/>
								</xsl:call-template>
							</xsl:if>
							<!-- administrative -->
							<xsl:if test="$adm-count >0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-adm"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Administrative_documents']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-adm"/>
									</fo:basic-link>
								</fo:block>
								<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes"
										select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/adm]')]"
									/>
								</xsl:call-template>
							</xsl:if>
							<!-- references -->
							<xsl:if test="$ref-count >0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-ref"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='References']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-ref"/>
									</fo:basic-link>
								</fo:block>
								<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes"
										select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/ref]')]"
									/>
								</xsl:call-template>
							</xsl:if>
							<!-- other -->
							<xsl:if test="$oth-count >0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-oth"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Other_documents']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-oth"/>
									</fo:basic-link>
								</fo:block>
								<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes"
										select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/oth]')]"
									/>
								</xsl:call-template>
							</xsl:if>
							<!-- statistical tables -->
							<xsl:if test="$tbl-count >0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-tbl"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Statistical_tables']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-tbl"/>
									</fo:basic-link>
								</fo:block>
								<!--
									<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes" select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[tbl')]"/>
									</xsl:call-template>
								-->
							</xsl:if>
							<!-- scripts and programs -->
							<xsl:if test="$prg-count >0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-prg"
										text-decoration="underline" color="blue">
										<xsl:value-of
											select="$msg/*/entry[@key='Scripts_and_programs']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-prg"/>
									</fo:basic-link>
								</fo:block>
								<!--
									<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes" select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[prg')]"/>
									</xsl:call-template>
								-->
							</xsl:if>
							<!-- other resources -->
							<xsl:if test="$unc-count >0">
								<fo:block font-size="8pt" text-align-last="justify"
									space-after="0.03in">
									<fo:basic-link internal-destination="documentation-unc"
										text-decoration="underline" color="blue">
										<xsl:value-of select="$msg/*/entry[@key='Other_resources']"/>
										<fo:leader leader-pattern="dots"/>
										<fo:page-number-citation ref-id="documentation-unc"/>
									</fo:basic-link>
								</fo:block>
								<xsl:call-template name="documentation-toc-section">
									<xsl:with-param name="nodes"
										select=" $rdf/rdf:RDF/rdf:Description[ not( contains(dc:type,'[doc') or contains(dc:type,'[tbl') or contains(dc:type,'[prg') )  ] "
									/>
								</xsl:call-template>
							</xsl:if>
						</fo:block>
						<!--
							DOCUMENTS
						-->
						<!-- report/analytrical -->
						<xsl:if test="$rep-count >0 or $anl-count > 0">
							<fo:block id="documentation-anl" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of
									select="$msg/*/entry[@key='Reports_and_analytical_documents']"/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/rep]')  or contains(dc:type,'[doc/anl]') ]"
							/>
						</xsl:if>
						<!-- questionnaires -->
						<xsl:if test="$qst-count > 0">
							<fo:block id="documentation-qst" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Questionnaires']"/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/qst]') ]"
							/>
						</xsl:if>
						<!-- technical -->
						<xsl:if test="$tec-count > 0">
							<fo:block id="documentation-tec" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Technical_documents']"/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/tec]') ]"
							/>
						</xsl:if>
						<!-- administrative -->
						<xsl:if test="$adm-count > 0">
							<fo:block id="documentation-adm" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Administrative_documents']"
								/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/adm]') ]"
							/>
						</xsl:if>
						<!-- references -->
						<xsl:if test="$ref-count > 0">
							<fo:block id="documentation-ref" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='References']"/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/ref]') ]"
							/>
						</xsl:if>
						<!-- other  -->
						<xsl:if test="$oth-count > 0">
							<fo:block id="documentation-oth" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Other_documents']"/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[doc/oth]') ]"
							/>
						</xsl:if>
						<!-- tables -->
						<xsl:if test="$tbl-count > 0">
							<fo:block id="documentation-tbl" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Statistical_Tables']"/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[tbl') ]"/>
						</xsl:if>
						<!-- scripts/programs -->
						<xsl:if test="$prg-count > 0">
							<fo:block id="documentation-prg" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Scripts_and_programs']"/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ contains(dc:type,'[prg') ]"/>
						</xsl:if>
						<!-- other resources -->
						<xsl:if test="$unc-count > 0">
							<fo:block id="documentation-unc" font-size="14pt" font-weight="bold"
								space-after="0.1in">
								<xsl:value-of select="$msg/*/entry[@key='Other_resources']"/>
							</fo:block>
							<xsl:apply-templates
								select="$rdf/rdf:RDF/rdf:Description[ not( contains(dc:type,'[doc') or contains(dc:type,'[tbl') or contains(dc:type,'[prg') )  ]"
							/>
						</xsl:if>
					</fo:flow>
				</fo:page-sequence>
			</xsl:if>
		</fo:root>
	</xsl:template>
	<!--
		Documentation TOC Section
	-->
	<xsl:template name="documentation-toc-section">
		<xsl:param name="nodes"/>
		<xsl:for-each select="$nodes">
			<fo:block margin-left="0.1in" font-size="8pt" text-align-last="justify"
				space-after="0.03in">
				<fo:basic-link internal-destination="{generate-id()}" text-decoration="underline"
					color="blue">
					<xsl:choose>
						<xsl:when test="normalize-space(dc:title)">
							<xsl:value-of select="normalize-space(dc:title)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>*** </xsl:text>
							<xsl:value-of select="$msg/*/entry[@key='Untitled']"/>
							<xsl:text> ****</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
					<fo:leader leader-pattern="dots"/>
					<fo:page-number-citation ref-id="{generate-id()}"/>
				</fo:basic-link>
			</fo:block>
		</xsl:for-each>
	</xsl:template>
	<!--
		ddi:AuthEnty
	-->
	<xsl:template match="ddi:AuthEnty">
		<fo:block>
			<xsl:call-template name="trim">
				<xsl:with-param name="s" select="."/>
			</xsl:call-template>
			<xsl:if test="@affiliation">, <xsl:value-of select="@affiliation"/></xsl:if>
		</fo:block>
	</xsl:template>
	<!--
		ddi:collDate
	-->
	<xsl:template match="ddi:collDate">
		<fo:block>
			<xsl:if test="@cycle">
				<xsl:value-of select="@cycle"/>
				<xsl:text>: </xsl:text>
			</xsl:if>
			<xsl:if test="@event">
				<xsl:value-of select="@event"/>
				<xsl:text> </xsl:text>
			</xsl:if>
			<xsl:value-of select="@date"/>
		</fo:block>
	</xsl:template>
	<!--
		ddi:contact
	-->
	<xsl:template match="ddi:contact">
		<fo:block>
			<xsl:value-of select="."/>
			<xsl:if test="@affiliation"> (<xsl:value-of select="@affiliation"/>) </xsl:if>
			<xsl:if test="@URI"> , <fo:basic-link external-destination="url('{@URI}')"
				text-decoration="underline" color="blue">
				<xsl:value-of select="@URI"/>
			</fo:basic-link>
			</xsl:if>
			<xsl:if test="@email"> , 
				<xsl:value-of select="@email"/>
			</xsl:if>
		</fo:block>
	</xsl:template>
	<!--
		ddi:dataCollector
	-->
	<xsl:template match="ddi:dataCollector">
		<fo:block>
			<xsl:call-template name="trim">
				<xsl:with-param name="s" select="."/>
			</xsl:call-template>
			<xsl:if test="@abbr"> (<xsl:value-of select="@abbr"/>) </xsl:if>
			<xsl:if test="@affiliation"> , <xsl:value-of select="@affiliation"/>
			</xsl:if>
		</fo:block>
	</xsl:template>
	<!--
		ddi:IDNo
	-->
	<xsl:template match="ddi:IDNo">
		<fo:block>
			<xsl:if test="@agency"><xsl:value-of select="@agency"/>: </xsl:if>
			<xsl:call-template name="trim">
				<xsl:with-param name="s" select="."/>
			</xsl:call-template>
		</fo:block>
	</xsl:template>
	<!--
		ddi:fileDsrc (default mode)
	-->
	<xsl:template match="ddi:fileDscr">
<xsl:variable name="fileId">
	<xsl:choose>
		<xsl:when test="@ID">
			<xsl:value-of select="@ID" />
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
		</xsl:otherwise>
	</xsl:choose>
</xsl:variable>		
		<fo:table id="file-{$fileId}" table-layout="fixed" width="100%"
			space-before="0.2in" space-after="0.2in">
			<fo:table-column column-width="proportional-column-width(20)"/>
			<fo:table-column column-width="proportional-column-width(80)"/>
			<fo:table-body>
				<!-- Filename -->
				<fo:table-row background-color="{$color-gray1}" keep-with-next="always">
					<fo:table-cell number-columns-spanned="2" border="{$default-border}"
						padding="{$cell-padding}">
						<fo:block font-size="12pt" font-weight="bold">
							<xsl:apply-templates select="ddi:fileTxt/ddi:fileName"/>
						</fo:block>
					</fo:table-cell>
				</fo:table-row>
				<!-- # Cases -->
				<xsl:if test="ddi:fileTxt/ddi:dimensns/ddi:caseQnty">
					<fo:table-row>
						<fo:table-cell font-weight="bold" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block># <xsl:value-of select="$msg/*/entry[@key='Cases']"
							/></fo:block>
						</fo:table-cell>
						<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
							<xsl:apply-templates select="ddi:fileTxt/ddi:dimensns/ddi:caseQnty"/>
						</fo:table-cell>
					</fo:table-row>
				</xsl:if>
				<!-- # Variables -->
				<xsl:if test="ddi:fileTxt/ddi:dimensns/ddi:varQnty">
					<fo:table-row>
						<fo:table-cell font-weight="bold" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block># <xsl:value-of select="$msg/*/entry[@key='Variables']"
							/></fo:block>
						</fo:table-cell>
						<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
							<xsl:apply-templates select="ddi:fileTxt/ddi:dimensns/ddi:varQnty"/>
						</fo:table-cell>
					</fo:table-row>
				</xsl:if>
				<!-- File Structure -->
				<xsl:if test="ddi:fileTxt/ddi:fileStrc">
					<fo:table-row>
						<fo:table-cell font-weight="bold" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block>
								<xsl:value-of select="$msg/*/entry[@key='File_Structure']"/>
							</fo:block>
						</fo:table-cell>
						<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
							<xsl:if test="ddi:fileTxt/ddi:fileStrc/@type">
								<fo:block><xsl:value-of select="$msg/*/entry[@key='Type']"/>:
									<xsl:value-of select="ddi:fileTxt/ddi:fileStrc/@type"/>
								</fo:block>
							</xsl:if>
							<xsl:if test="ddi:fileTxt/ddi:fileStrc/ddi:recGrp/@keyvar">
								<fo:block>
									<xsl:value-of select="$msg/*/entry[@key='Keys']"/>:&#160;
									<xsl:variable name="list"
										select="concat(ddi:fileTxt/ddi:fileStrc/ddi:recGrp/@keyvar,' ')"/>
									<!-- add a space at the end of the list for matching puspose -->
									<xsl:for-each
										select="/ddi:codeBook/ddi:dataDscr/ddi:var[contains($list,concat(@ID,' '))]">
										<!-- add a space to the variable ID to avoid partial match -->
										<xsl:if test="position()>1">,&#160;</xsl:if>
										<xsl:value-of select="./@name"/>
										<xsl:if test="normalize-space(./ddi:labl)">
											&#160;(<xsl:value-of
												select="normalize-space(./ddi:labl)"/>) </xsl:if>
									</xsl:for-each>
								</fo:block>
							</xsl:if>
						</fo:table-cell>
					</fo:table-row>
				</xsl:if>
				<!-- File Content -->
				<xsl:for-each select="ddi:fileTxt/ddi:fileCont">
					<fo:table-row>
						<fo:table-cell number-columns-spanned="2" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block font-weight="bold" text-decoration="underline">
								<xsl:value-of select="$msg/*/entry[@key='File_Content']"/>
							</fo:block>
							<xsl:apply-templates select="."/>
						</fo:table-cell>
					</fo:table-row>
				</xsl:for-each>
				<!-- File Producer -->
				<xsl:for-each select="ddi:fileTxt/ddi:filePlac">
					<fo:table-row>
						<fo:table-cell number-columns-spanned="2" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block font-weight="bold" text-decoration="underline">
								<xsl:value-of select="$msg/*/entry[@key='Producer']"/>
							</fo:block>
							<xsl:apply-templates select="."/>
						</fo:table-cell>
					</fo:table-row>
				</xsl:for-each>
				<!-- File Version -->
				<xsl:for-each select="ddi:fileTxt/ddi:verStmt">
					<fo:table-row>
						<fo:table-cell number-columns-spanned="2" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block font-weight="bold" text-decoration="underline">
								<xsl:value-of select="$msg/*/entry[@key='Version']"/>
							</fo:block>
							<xsl:apply-templates select="."/>
						</fo:table-cell>
					</fo:table-row>
				</xsl:for-each>
				<!-- File Processing Checks -->
				<xsl:for-each select="ddi:fileTxt/ddi:dataChck">
					<fo:table-row>
						<fo:table-cell number-columns-spanned="2" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block font-weight="bold" text-decoration="underline">
								<xsl:value-of select="$msg/*/entry[@key='Processing_Checks']"/>
							</fo:block>
							<xsl:apply-templates select="."/>
						</fo:table-cell>
					</fo:table-row>
				</xsl:for-each>
				<!-- File Missing Data -->
				<xsl:for-each select="ddi:fileTxt/ddi:dataMsng">
					<fo:table-row>
						<fo:table-cell number-columns-spanned="2" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block font-weight="bold" text-decoration="underline">
								<xsl:value-of select="$msg/*/entry[@key='Missing_Data']"/>
							</fo:block>
							<xsl:apply-templates select="."/>
						</fo:table-cell>
					</fo:table-row>
				</xsl:for-each>
				<!-- File Notes -->
				<xsl:for-each select="ddi:notes">
					<fo:table-row>
						<fo:table-cell number-columns-spanned="2" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block font-weight="bold" text-decoration="underline">
								<xsl:value-of select="$msg/*/entry[@key='Notes']"/>
							</fo:block>
							<xsl:apply-templates select="."/>
						</fo:table-cell>
					</fo:table-row>
				</xsl:for-each>
			</fo:table-body>
		</fo:table>
	</xsl:template>
	<!--
		ddi:fileDsrc (variables list mode)
	-->
	<xsl:template match="ddi:fileDscr" mode="variables-list">
	<xsl:variable name="fileId">
		<xsl:choose>
			<xsl:when test="@ID">
				<xsl:value-of select="@ID" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>		
		<!-- VARIABLES -->
		<fo:table id="varlist-{$fileId}" table-layout="fixed" width="100%" font-size="8pt" space-before="0.2in" space-after="0.2in">
			<!-- column width -->
			<xsl:call-template name="variables-table-col-width"/>
			<!-- table header -->
			<fo:table-header>
				<!-- file identification -->
				<fo:table-row text-align="center" vertical-align="top" keep-with-next="always">
					<fo:table-cell text-align="left" number-columns-spanned="8"
						border="{$default-border}" padding="{$cell-padding}">
						<fo:block font-size="12pt" font-weight="bold">
							<xsl:value-of select="$msg/*/entry[@key='File']"/>
							<xsl:text> </xsl:text>
							<xsl:apply-templates select="ddi:fileTxt/ddi:fileName"/>
							<!--<fox:continued-label><fo:inline font-size="8pt"><xsl:text> (</xsl:text><xsl:value-of select="$msg/*/entry[@key='cont']"/><xsl:text>)</xsl:text></fo:inline></fox:continued-label>-->
						</fo:block>
					</fo:table-cell>
				</fo:table-row>
				<!-- column header -->
				<xsl:call-template name="variables-table-col-header"/>
			</fo:table-header>
			<!-- table body -->
			<fo:table-body>
				<fo:table-row><fo:table-cell><fo:block> <!-- ToDo: --> </fo:block></fo:table-cell></fo:table-row>
				<xsl:apply-templates select="/ddi:codeBook/ddi:dataDscr/ddi:var[ddi:location/@fileid=$fileId]"
					mode="variables-list"/>
			</fo:table-body>
		</fo:table>
	</xsl:template>
	<!--
		ddi:fileDsrc (variables description mode)
	-->
	<xsl:template match="ddi:fileDscr" mode="variables-description">
		<xsl:variable name="fileId">
			<xsl:choose>
				<xsl:when test="@ID">
					<xsl:value-of select="@ID" />
				</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="ddi:fileTxt/ddi:fileName/@ID" />
			</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>		
		<xsl:variable name="fileName" select="ddi:fileTxt/ddi:fileName"/>
		
	   <xsl:for-each select="/ddi:codeBook/ddi:dataDscr/ddi:var[ddi:location/@fileid=$fileId][position() mod $chunkSize = 1]">	
	      <fo:page-sequence master-reference="default-page" font-family="{$font-family}" font-size="10pt">
	         <xsl:call-template name="footer"/>
	         <fo:flow flow-name="xsl-region-body">
               <!--	Write header if we're at the start of the file -->
	            <xsl:if test="position()=1">
	               <fo:table id="vardesc-{$fileId}" table-layout="fixed" width="100%" font-size="8pt">
	                  <!-- column width -->
	                  <fo:table-column column-width="proportional-column-width(100)"/>
	                  <!-- table header -->
	                  <fo:table-header space-after="0.2in">
	                     <!-- file identification -->
	                     <fo:table-row text-align="center" vertical-align="top">
	                        <fo:table-cell text-align="left" border="{$default-border}"
	                           padding="{$cell-padding}">
	                           <fo:block font-size="14pt" font-weight="bold">
	                              <xsl:value-of select="$msg/*/entry[@key='File']"/>
	                              <xsl:text> : </xsl:text>
	                              <xsl:apply-templates select="$fileName"/>
	                           </fo:block>
	                        </fo:table-cell>
	                     </fo:table-row>
	                  </fo:table-header>
	                  <!-- VARIABLES -->
	                  <fo:table-body>
                          <!-- needed in case of subset -->
	                     <fo:table-row>
	                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
	                     </fo:table-row>
	                     <xsl:apply-templates select=".|following-sibling::ddi:var[ddi:location/@fileid=$fileId][$chunkSize > position()]"/>
	                  </fo:table-body>
	               </fo:table>
	            </xsl:if>
	            <xsl:if test="position()>1">
	               <!-- VARIABLES -->
	               <fo:table table-layout="fixed" width="100%" font-size="8pt">
	                  <fo:table-body>
                          <!-- needed in case of subset -->
	                     <fo:table-row>
	                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
	                     </fo:table-row>
	                     <xsl:apply-templates select=".|following-sibling::ddi:var[ddi:location/@fileid=$fileId][$chunkSize > position()]"/>
	                  </fo:table-body>
	               </fo:table>
	            </xsl:if>
	         </fo:flow>
	      </fo:page-sequence>
	   </xsl:for-each>
	</xsl:template>
	<!--
		ddi:fileName
	-->
	<xsl:template match="ddi:fileName">
		<!-- this template removes the .NSDstat extension -->
		<xsl:variable name="filename" select="normalize-space(.)"/>
		<xsl:choose>
			<xsl:when test=" contains( $filename , '.NSDstat' )">
				<xsl:value-of select="substring($filename,1,string-length($filename)-8)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$filename"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--
		ddi:fundAg
	-->
	<xsl:template match="ddi:fundAg">
		<fo:block>
			<xsl:call-template name="trim">
				<xsl:with-param name="s" select="."/>
			</xsl:call-template>
			<xsl:if test="@abbr"> (<xsl:value-of select="@abbr"/>) </xsl:if>
			<xsl:if test="@role"> , <xsl:value-of select="@role"/>
			</xsl:if>
		</fo:block>
	</xsl:template>
	<!--
		ddi:othId
	-->
	<xsl:template match="ddi:othId">
		<fo:block>
			<xsl:call-template name="trim">
				<xsl:with-param name="s" select="ddi:p"/>
			</xsl:call-template>
			<xsl:if test="@role"> , <xsl:value-of select="@role"/>
			</xsl:if>
			<xsl:if test="@affiliation"> , <xsl:value-of select="@affiliation"/>
			</xsl:if>
		</fo:block>
	</xsl:template>
	<!--
		ddi:producer
	-->
	<xsl:template match="ddi:producer">
		<fo:block>
			<xsl:call-template name="trim">
				<xsl:with-param name="s" select="."/>
			</xsl:call-template>
			<xsl:if test="@abbr"> (<xsl:value-of select="@abbr"/>) </xsl:if>
			<xsl:if test="@affiliation"> , <xsl:value-of select="@affiliation"/>
			</xsl:if>
			<xsl:if test="@role"> , <xsl:value-of select="@role"/>
			</xsl:if>
		</fo:block>
	</xsl:template>
	<!--
		ddi:varGrp
	-->
	<xsl:template match="ddi:varGrp">
		<xsl:if test="contains($subsetGroups,concat(',',@ID,',')) or string-length($subsetGroups)=0">
			<fo:table id="vargrp-{@ID}" table-layout="fixed" width="100%" space-before="0.2in">
				<fo:table-column column-width="proportional-column-width(20)"/>
				<fo:table-column column-width="proportional-column-width(80)"/>
				<fo:table-body>
					<!-- Group name -->
					<fo:table-row>
						<fo:table-cell number-columns-spanned="2" border="{$default-border}"
							padding="{$cell-padding}">
							<fo:block font-size="12pt" font-weight="bold">
								<xsl:value-of select="$msg/*/entry[@key='Group']"/>
								<xsl:text> </xsl:text>
								<xsl:value-of select="normalize-space(ddi:labl)"/>
							</fo:block>
						</fo:table-cell>
					</fo:table-row>
					<!-- Text-->
					<xsl:for-each select="ddi:txt">
						<fo:table-row>
							<fo:table-cell number-columns-spanned="2" border="{$default-border}"
								padding="{$cell-padding}">
								<xsl:apply-templates select="."/>
							</fo:table-cell>
						</fo:table-row>
					</xsl:for-each>
					<!-- Definition -->
					<xsl:for-each select="ddi:defntn">
						<fo:table-row>
							<fo:table-cell number-columns-spanned="2" border="{$default-border}"
								padding="{$cell-padding}">
								<fo:block font-weight="bold" text-decoration="underline">
									<xsl:value-of select="$msg/*/entry[@key='Definition']"/>
								</fo:block>
								<xsl:apply-templates select="."/>
							</fo:table-cell>
						</fo:table-row>
					</xsl:for-each>
					<!-- Universe-->
					<xsl:for-each select="ddi:universe">
						<fo:table-row>
							<fo:table-cell number-columns-spanned="2" border="{$default-border}"
								padding="{$cell-padding}">
								<fo:block font-weight="bold" text-decoration="underline">
									<xsl:value-of select="$msg/*/entry[@key='Universe']"/>
								</fo:block>
								<xsl:apply-templates select="."/>
							</fo:table-cell>
						</fo:table-row>
					</xsl:for-each>
					<!-- Notes -->
					<xsl:for-each select="ddi:notes">
						<fo:table-row>
							<fo:table-cell number-columns-spanned="2" border="{$default-border}"
								padding="{$cell-padding}">
								<fo:block font-weight="bold" text-decoration="underline">
									<xsl:value-of select="$msg/*/entry[@key='Notes']"/>
								</fo:block>
								<xsl:apply-templates select="."/>
							</fo:table-cell>
						</fo:table-row>
					</xsl:for-each>
					<!-- subgroups -->
					<xsl:if test="./@varGrp">
						<fo:table-row>
							<fo:table-cell font-weight="bold" border="{$default-border}"
								padding="{$cell-padding}">
								<fo:block>
									<xsl:value-of select="$msg/*/entry[@key='Subgroups']"/>
								</fo:block>
							</fo:table-cell>
							<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
								<fo:block>
									<!-- loop over groups in codeBook that are in this sequence -->
									<xsl:variable name="list" select="concat(./@varGrp,' ')"/>
									<!-- add a space at the end of the list for matching purposes -->
									<xsl:for-each
										select="/ddi:codeBook/ddi:dataDscr/ddi:varGrp[contains($list,concat(@ID,' '))]">
										<!-- add a space to the ID to avoid partial match -->
										<xsl:if test="position()>1">,</xsl:if>
										<xsl:value-of select="./ddi:labl"/>
									</xsl:for-each>
								</fo:block>
							</fo:table-cell>
						</fo:table-row>
					</xsl:if>
				</fo:table-body>
			</fo:table>
			<!-- Look for variables in this group -->
			<xsl:if test="./@var">
				<!-- variables table -->
				<fo:table id="varlist-{@ID}" table-layout="fixed" width="100%" font-size="8pt"
					space-after="0.0in">
					<!-- column width -->
					<xsl:call-template name="variables-table-col-width"/>
					<!-- table header -->
					<fo:table-header>
						<!-- column header -->
						<xsl:call-template name="variables-table-col-header"/>
					</fo:table-header>
					<!-- table body -->
					<fo:table-body>
						<fo:table-row>
							<fo:table-cell>
								<fo:block>
									<!-- ToDo: -->
								</fo:block>
							</fo:table-cell>
						</fo:table-row>
						<xsl:variable name="list" select="concat(./@var,' ')"/>
						<!-- add a space at the end of the list for matching purposes -->
						<xsl:apply-templates
							select="/ddi:codeBook/ddi:dataDscr/ddi:var[ contains($list,concat(@ID,' ')) ]"
							mode="variables-list"/>
						<!-- add a space to the ID to avoid partial match -->
					</fo:table-body>
				</fo:table>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	<!--
		ddi:var (variables list)
	-->
	<xsl:template match="ddi:var" mode="variables-list">
		<!-- file ID parameter (use first file in @files if not specified) -->
		<xsl:param name="fileId" select="./@files"/>
		<xsl:if test="contains($subsetVars,concat(',',@ID,',')) or string-length($subsetVars)=0 ">
			<fo:table-row text-align="center" vertical-align="top">
				<xsl:choose>
					<xsl:when test="position() mod 2 = 0">
						<xsl:attribute name="background-color">
							<xsl:value-of select="$color-white"/>
						</xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="background-color">
							<xsl:value-of select="$color-white"/>
							<!--<xsl:value-of select="$color-gray1"/> todo: doesnt work with subset -->
						</xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
				<!-- Variable Position -->
				<fo:table-cell text-align="center" border="{$default-border}" padding="{$cell-padding}">
					<fo:block>
						<xsl:value-of select="position()"/>
					</fo:block>
					<!--
						[File <xsl:value-of select="$filePos"/>]
						[Var <xsl:value-of select="$varPos"/>]
					-->
				</fo:table-cell>
				<!-- Variable Name-->
				<fo:table-cell text-align="left" border="{$default-border}" padding="{$cell-padding}">
					<fo:block>
						<xsl:choose>
							<xsl:when test="$show-variables-list = 1">
								<fo:basic-link internal-destination="var-{@ID}"
									text-decoration="underline" color="blue">
									<xsl:if test="string-length(@name) > 10">
										<xsl:value-of select="substring(./@name,0,9)"/> .. </xsl:if>
									<xsl:if test="11 > string-length(@name)">
										<xsl:value-of select="./@name"/>
									</xsl:if>
								</fo:basic-link>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="./@name"/>
							</xsl:otherwise>
						</xsl:choose>
					</fo:block>
				</fo:table-cell>
				<!-- Variable Label -->
				<fo:table-cell text-align="left" border="{$default-border}" padding="{$cell-padding}">
					<fo:block>
						<xsl:choose>
							<xsl:when test="normalize-space(./ddi:labl)">
								<xsl:value-of select="normalize-space(./ddi:labl)"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:text>-</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</fo:block>
				</fo:table-cell>
				<!-- Variable Type -->
				<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
					<fo:block>
						<xsl:choose>
							<xsl:when test="normalize-space(@intrvl)">
								<xsl:choose>
									<xsl:when test="@intrvl='discrete'">
										<xsl:value-of select="$msg/*/entry[@key='discrete']"/>
									</xsl:when>
									<xsl:when test="@intrvl='contin'">
										<xsl:value-of select="$msg/*/entry[@key='continuous']"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$msg/*/entry[@key='Undetermined']"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:when>
							<xsl:otherwise>
								<xsl:text>-</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</fo:block>
				</fo:table-cell>
				<!-- Variable Format -->
				<fo:table-cell border="{$default-border}" padding="{$cell-padding}">
					<fo:block>
						<xsl:choose>
							<xsl:when test="normalize-space(ddi:varFormat/@type)">
								<xsl:value-of select="ddi:varFormat/@type"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:text>-</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
						<xsl:if test="normalize-space(ddi:location/@width)">
							<xsl:text>-</xsl:text>
							<xsl:value-of select="ddi:location/@width"/>
						</xsl:if>
						<xsl:if test="normalize-space(@dcml)">
							<xsl:text>.</xsl:text>
							<xsl:value-of select="@dcml"/>
						</xsl:if>
					</fo:block>
				</fo:table-cell>
				<!-- Variable Valid -->
				<fo:table-cell text-align="center" border="{$default-border}" padding="{$cell-padding}">
					<fo:block>
						<xsl:choose>
							<xsl:when test="count(ddi:sumStat[@type='vald'])>0">
								<xsl:for-each select="ddi:sumStat[@type='vald']">
									<xsl:if test="position()=1">
										<xsl:value-of select="."/>
									</xsl:if>
								</xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
								<xsl:text>-</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</fo:block>
				</fo:table-cell>
				<!-- Variable Invalid -->
				<fo:table-cell text-align="center" border="{$default-border}" padding="{$cell-padding}">
					<fo:block>
						<xsl:choose>
							<xsl:when test="count(ddi:sumStat[@type='invd'])>0">
								<xsl:for-each select="ddi:sumStat[@type='invd']">
									<xsl:if test="position()=1">
										<xsl:value-of select="."/>
									</xsl:if>
								</xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
								<xsl:text>-</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</fo:block>
				</fo:table-cell>
				<!-- Variable Literal Question -->
				<xsl:if test="$show-variables-list-question">
					<fo:table-cell text-align="left" border="{$default-border}"
						padding="{$cell-padding}">
						<fo:block>
							<xsl:choose>
								<xsl:when test="normalize-space(./ddi:qstn/ddi:qstnLit)">
									<xsl:variable name="qstnlistvar" select="normalize-space(./ddi:qstn/ddi:qstnLit)"/>
									<xsl:variable name="qstnlistvarLen" select="string-length($qstnlistvar)"/>
									<xsl:if test="$qstnlistvarLen &gt; $show-variables-list-question-max-len">
									<fo:basic-link internal-destination="var-{@ID}" text-decoration="underline" color="blue">
										<xsl:value-of select="./@name"/><xsl:text>&#160;</xsl:text>
										<xsl:value-of select="$msg/*/entry[@key='QuestionDetails']"/>
									</fo:basic-link>
									</xsl:if>
									<xsl:if test="$show-variables-list-question-max-len &gt;= $qstnlistvarLen">
										<xsl:value-of select="$qstnlistvar"/>
									</xsl:if>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>-</xsl:text>
								</xsl:otherwise>
							</xsl:choose>
						</fo:block>
					</fo:table-cell>
				</xsl:if>
			</fo:table-row>
		</xsl:if>
	</xsl:template>
	<!--
		ddi:var
	-->
	<xsl:template match="ddi:var">
		<!-- file ID parameter (use first file in @files if not specified) -->
		<!-- <xsl:param name="fileId" select="tokenize(./@files,' ')[1]"/> doesn't work, to check -->
		<xsl:param name="fileId" select="./@files"/>
		<!-- VARIABLE TABLE -->
		<xsl:if test="contains($subsetVars,concat(',',@ID,',')) or string-length($subsetVars)=0 ">
			<fo:table-row text-align="center" vertical-align="top">
				<fo:table-cell>
					<fo:table id="var-{@ID}" table-layout="fixed" width="100%" font-size="8pt"
						space-after="0.3in">
						<fo:table-column column-width="proportional-column-width(20)"/>
						<fo:table-column column-width="proportional-column-width(80)"/>
						<!-- HEADER -->
						<fo:table-header>
							<fo:table-row background-color="{$color-gray1}" text-align="center"
								vertical-align="top">
								<fo:table-cell number-columns-spanned="2" font-size="10pt"
									font-weight="bold" text-align="left" border="{$default-border}"
									padding="{$cell-padding}">
									<fo:block>
										<fo:inline font-size="8pt" font-weight="normal" vertical-align="text-top">#
											<xsl:value-of select="./@id"/>
											<xsl:text> </xsl:text></fo:inline>
										<xsl:value-of select="./@name"/>
										<xsl:if test="normalize-space(./ddi:labl)">
											<xsl:text>: </xsl:text>
											<xsl:value-of select="normalize-space(./ddi:labl)"/>
											<!--<fox:continued-label><fo:inline font-size="8pt"><xsl:text> (</xsl:text><xsl:value-of select="$msg/*/entry[@key='cont']"/><xsl:text>)</xsl:text></fo:inline></fox:continued-label>-->
										</xsl:if>
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-header>
						<!-- BODY -->
						<fo:table-body>
							<!-- Informations -->
							<fo:table-row text-align="center" vertical-align="top">
								<fo:table-cell font-weight="bold" text-align="left"
									border="{$default-border}" padding="{$cell-padding}">
									<fo:block>
										<xsl:value-of select="$msg/*/entry[@key='Information']"/>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell text-align="left" border="{$default-border}"
									padding="{$cell-padding}">
									<fo:block>
										<!-- type -->
										<xsl:if test="normalize-space(@intrvl)">
											<xsl:text> [</xsl:text><xsl:value-of
												select="$msg/*/entry[@key='Type']"/>= <xsl:choose>
													<xsl:when test="@intrvl='discrete'"><xsl:value-of
														select="$msg/*/entry[@key='discrete']"/>
													</xsl:when>
													<xsl:when test="@intrvl='contin'"><xsl:value-of
														select="$msg/*/entry[@key='continuous']"/>
													</xsl:when>
												</xsl:choose>
											<xsl:text>] </xsl:text>
										</xsl:if>
										<!-- format -->
										<xsl:for-each select="ddi:varFormat">
											<xsl:text> [</xsl:text><xsl:value-of
												select="$msg/*/entry[@key='Format']"/>=<xsl:value-of
													select="@type"/>
											<xsl:if test="normalize-space(ddi:location/@width)">
												<xsl:text>-</xsl:text>
												<xsl:value-of select="ddi:location/@width"/>
											</xsl:if>
											<xsl:if test="normalize-space(@dcml)">
												<xsl:text>.</xsl:text>
												<xsl:value-of select="@dcml"/>
											</xsl:if>
											<xsl:text>] </xsl:text>
										</xsl:for-each>
										<!-- range -->
										<xsl:for-each select="ddi:valrng/ddi:range">
											<xsl:text> [</xsl:text><xsl:value-of
												select="$msg/*/entry[@key='Range']"/>= <xsl:value-of
													select="@min"/>-<xsl:value-of select="@max"/>
											<xsl:text>] </xsl:text>
										</xsl:for-each>
										<!-- missing -->
										<xsl:text> [</xsl:text>
										<xsl:value-of select="$msg/*/entry[@key='Missing']"/>
										<xsl:text>=*</xsl:text>
										<xsl:for-each select="ddi:invalrng/ddi:item">
											<xsl:text>/</xsl:text>
											<xsl:value-of select="@VALUE"/>
										</xsl:for-each>
										<xsl:text>] </xsl:text>

										<!-- weight -->
										<!-- TODO
											<xsl:if test="normalize-space(@wgt-var)">
											<xsl:variable name="wgt-var" select="@wgt-var"/>
											<xsl:text> [</xsl:text><xsl:value-of select="$msg/*/entry[@key='Weight']"/>=<fo:basic-link internal-destination="var-{$wgt-var}" text-decoration="underline" color="blue"><xsl:value-of select="/ddi:codeBook/ddi:dataDscr/ddi:var[@ID=$wgt-var]/@name"/></fo:basic-link>
											<xsl:text>] </xsl:text>
											</xsl:if>
										-->
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- statistics -->
							<xsl:variable name="statistics"
								select="ddi:sumStat[contains('vald invd mean stdev',@type)]"/>
							<xsl:if test="$statistics">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell font-weight="bold" text-align="left"
										border="{$default-border}" padding="{$cell-padding}">
										<fo:block>
											<xsl:value-of select="$msg/*/entry[@key='Statistics']"/>
											<xsl:text> [</xsl:text>
											<xsl:value-of
												select="$msg/*/entry[@key='Abbrev_NotWeighted']"/>
											<xsl:text>/ </xsl:text>
											<xsl:value-of select="$msg/*/entry[@key='Abbrev_Weighted']"/>
											<xsl:text>]</xsl:text>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<!-- summary statistics -->
											<xsl:for-each select="$statistics[not(@wgtd)]">
												<xsl:variable name="type" select="@type"/>
												<xsl:variable name="label">
													<xsl:choose>
														<xsl:when test="@type='vald' ">
															<xsl:value-of select="$msg/*/entry[@key='Valid']"
															/>
														</xsl:when>
														<xsl:when test="@type='invd' ">
															<xsl:value-of
																select="$msg/*/entry[@key='Invalid']"/>
														</xsl:when>
														<xsl:when test="@type='mean' ">
															<xsl:value-of select="$msg/*/entry[@key='Mean']"/>
														</xsl:when>
														<xsl:when test="@type='stdev' ">
															<xsl:value-of select="$msg/*/entry[@key='StdDev']"
															/>
														</xsl:when>
														<xsl:otherwise>
															<xsl:value-of select="@type"/>
														</xsl:otherwise>
													</xsl:choose>
												</xsl:variable>
												<xsl:text> [</xsl:text>
												<xsl:value-of select="$label"/>
												<xsl:text>=</xsl:text>
												<xsl:value-of select="normalize-space(.)"/>
												<!-- weighted value -->
												<xsl:text> /</xsl:text>
												<xsl:choose>
													<xsl:when
														test="following-sibling::ddi:sumStat[1]/@type=$type and following-sibling::ddi:sumStat[1]/@wgtd">
														<xsl:value-of
															select="following-sibling::ddi:sumStat[1]"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:text>-</xsl:text>
													</xsl:otherwise>
												</xsl:choose>
												<xsl:text>] </xsl:text>
											</xsl:for-each>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- definition  -->
							<xsl:if test="normalize-space(./ddi:txt)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Definition']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:txt"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- universe  -->
							<xsl:if test="normalize-space(./ddi:universe)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Universe']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:universe"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- source -->
							<xsl:if test="normalize-space(./ddi:respUnit)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Source']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:respUnit"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- pre-question -->
							<xsl:if test="normalize-space(./ddi:qstn/ddi:preQTxt)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Pre-question']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:qstn/ddi:preQTxt"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- question -->
							<xsl:if test="normalize-space(./ddi:qstn/ddi:qstnLit)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Literal_question']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:qstn/ddi:qstnLit"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- post-question -->
							<xsl:if test="normalize-space(./ddi:qstn/ddi:postQTxt)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Post-question']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:qstn/ddi:postQTxt"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- interviewer instructions -->
							<xsl:if test="normalize-space(./ddi:qstn/ddi:ivuInstr)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of
												select="$msg/*/entry[@key='Interviewers_instructions']"
											/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:qstn/ddi:ivuInstr"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- imputation -->
							<xsl:if test="normalize-space(./ddi:imputation)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Imputation']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:imputation"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- Recoding -->
							<xsl:if test="normalize-space(./ddi:codInstr)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of
												select="$msg/*/entry[@key='Recoding_and_Derivation']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:codInstr"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- Security -->
							<xsl:if test="normalize-space(./ddi:security)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Security']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:security"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- Concepts -->
							<xsl:if test="normalize-space(./ddi:concept)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Concepts']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:for-each select="./ddi:concept">
												<xsl:if test="position()>1">, </xsl:if>
												<xsl:value-of select="normalize-space(.)"/>
											</xsl:for-each>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- Notes -->
							<xsl:if test="normalize-space(./ddi:notes)">
								<fo:table-row text-align="center" vertical-align="top">
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block font-weight="bold">
											<xsl:value-of select="$msg/*/entry[@key='Notes']"/>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell text-align="left" border="{$default-border}"
										padding="{$cell-padding}">
										<fo:block>
											<xsl:apply-templates select="./ddi:notes"/>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:if>
							<!-- Categories -->
							<xsl:if
								test="$show-variables-description-categories=1 and normalize-space(./ddi:catgry)">
								<xsl:variable name="category-count" select="count(ddi:catgry)"/>
								<fo:table-row text-align="center" vertical-align="top">
									<xsl:choose>
										<xsl:when
											test="number($show-variables-description-categories-max) >= $category-count">
											<!-- VAR CATEGORIES TABLE -->
											<fo:table-cell text-align="left" border="{$default-border}" number-columns-spanned="2" padding="{$cell-padding}">
												<!-- check if variable is weighted -->
												<xsl:variable name="is-weighted" select="count(ddi:catgry/ddi:catStat[@type='freq' and @wgtd='wgtd' ]) > 0"/>
												<!-- get the frequency statistics nodes -->
												<xsl:variable name="catgry-freq-nodes" select="ddi:catgry[not(@missing='Y')]/ddi:catStat[@type='freq']"/>
												<!-- compute frequency totals -->
												<xsl:variable name="catgry-sum-freq" select="sum($catgry-freq-nodes[ not(@wgtd='wgtd') ])"/>
												<xsl:variable name="catgry-sum-freq-wgtd" select="sum($catgry-freq-nodes[ @wgtd='wgtd'])"/>
												<!-- compute frequency maximum -->
												<xsl:variable name="catgry-max-freq">
													<xsl:call-template name="math:max">
														<xsl:with-param name="nodes"
															select="$catgry-freq-nodes[ not(@wgtd='wgtd') ]"/>
													</xsl:call-template>
												</xsl:variable>
												<xsl:variable name="catgry-max-freq-wgtd">
													<xsl:call-template name="math:max">
														<xsl:with-param name="nodes"
															select="$catgry-freq-nodes[@type='freq' and @wgtd='wgtd' ]"
														/>
													</xsl:call-template>
												</xsl:variable>
												<!-- render the table -->
												<fo:table id="var-{@ID}-cat" table-layout="fixed"
													width="100%" font-size="8pt">
													<fo:table-column
														column-width="proportional-column-width(12)"/>
													<xsl:choose>
														<xsl:when test="$is-weighted">
															<fo:table-column
																column-width="proportional-column-width(33)"/>
															<fo:table-column
																column-width="proportional-column-width(8)"/>
															<fo:table-column
																column-width="proportional-column-width(12)"/>
														</xsl:when>
														<xsl:otherwise>
															<fo:table-column
																column-width="proportional-column-width(45)"/>
															<fo:table-column
																column-width="proportional-column-width(8)"/>
														</xsl:otherwise>
													</xsl:choose>
													<xsl:variable name="bar-column-width" select="2.5"/>
													<fo:table-column
														column-width="{$bar-column-width}in"/>
													<!-- VAR CATEGORIES HEADER -->
													<fo:table-header>
														<fo:table-row background-color="{$color-gray1}"
															text-align="left" vertical-align="top">
															<fo:table-cell border="0.5pt solid white"
																padding="{$cell-padding}">
																<fo:block font-weight="bold">
																	<xsl:value-of select="$msg/*/entry[@key='Value']"/>
																	<!--<fox:continued-label><fo:inline font-size="8pt"> (<xsl:value-of select="$msg/*/entry[@key='cont']"/>)</fo:inline></fox:continued-label>-->
																</fo:block>
															</fo:table-cell>
															<fo:table-cell border="0.5pt solid white"
																padding="{$cell-padding}">
																<fo:block font-weight="bold">
																	<xsl:value-of select="$msg/*/entry[@key='Label']"
																	/>
																</fo:block>
															</fo:table-cell>
															<fo:table-cell border="0.4pt solid white"
																padding="{$cell-padding}" text-align="center">
																<fo:block font-weight="bold">
																	<xsl:value-of
																		select="$msg/*/entry[@key='Cases_Abbreviation']"/>
																</fo:block>
															</fo:table-cell>
															<xsl:if test="$is-weighted">
																<fo:table-cell border="0.4pt solid white"
																	padding="{$cell-padding}" text-align="center">
																	<fo:block font-weight="bold">
																		<xsl:value-of
																			select="$msg/*/entry[@key='Weighted']"/>
																	</fo:block>
																</fo:table-cell>
															</xsl:if>
															<fo:table-cell border="0.4pt solid white"
																padding="{$cell-padding}" text-align="center">
																<fo:block font-weight="bold">
																	<xsl:value-of
																		select="$msg/*/entry[@key='Percentage']"/>
																	<xsl:if test="$is-weighted"> (<xsl:value-of
																		select="$msg/*/entry[@key='Weighted']"/>)</xsl:if>
																</fo:block>
															</fo:table-cell>
														</fo:table-row>
													</fo:table-header>
													<!-- VAR CATEGORIES BODY -->
													<fo:table-body>
														<xsl:for-each select="ddi:catgry">
															<fo:table-row background-color="{$color-gray2}"
																text-align="center" vertical-align="top">
																<!-- value -->
																<fo:table-cell text-align="left"
																	border="0.5pt solid white" padding="2pt">
																	<fo:block>
																		<xsl:call-template name="trim">
																			<xsl:with-param name="s" select="ddi:catValu"/>
																		</xsl:call-template>
																	</fo:block>
																</fo:table-cell>
																<!-- label -->
																<fo:table-cell text-align="left"
																	border="0.5pt solid white" padding="2pt">
																	<fo:block>
																		<xsl:call-template name="trim">
																			<xsl:with-param name="s" select="ddi:labl"/>
																		</xsl:call-template>
																	</fo:block>
																</fo:table-cell>
																<!-- frequency -->
																<xsl:variable name="catgry-freq"
																	select="ddi:catStat[@type='freq' and not(@wgtd='wgtd') ]"/>
																<fo:table-cell text-align="center"
																	border="0.5pt solid white" padding="2pt">
																	<fo:block>
																		<xsl:call-template name="trim">
																			<xsl:with-param name="s" select="$catgry-freq"/>
																		</xsl:call-template>
																	</fo:block>
																</fo:table-cell>
																<!-- weighted frequency -->
																<xsl:variable name="catgry-freq-wgtd"
																	select="ddi:catStat[@type='freq' and @wgtd='wgtd' ]"/>
																<xsl:if test="$is-weighted">
																	<fo:table-cell text-align="center"
																		border="0.5pt solid white" padding="2pt">
																		<fo:block>
																			<xsl:call-template name="trim">
																				<xsl:with-param name="s"
																					select="format-number($catgry-freq-wgtd,'0.0')"/>
																			</xsl:call-template>
																		</fo:block>
																	</fo:table-cell>
																</xsl:if>
																<!-- BAR-->

																<!-- compute percentage -->
																<xsl:variable name="catgry-pct">
																	<xsl:choose>
																		<xsl:when test="$is-weighted">
																			<xsl:value-of
																				select="$catgry-freq-wgtd div $catgry-sum-freq-wgtd"
																			/>
																		</xsl:when>
																		<xsl:otherwise>
																			<xsl:value-of
																				select="$catgry-freq div $catgry-sum-freq"/>
																		</xsl:otherwise>
																	</xsl:choose>
																</xsl:variable>
																<!-- compute bar width (percentage of highest value minus some space to display the percentage value) -->
																<xsl:variable name="tmp-col-width-1">
																	<xsl:choose>
																		<xsl:when test="$is-weighted">
																			<xsl:value-of
																				select="($catgry-freq-wgtd div $catgry-max-freq-wgtd) * ($bar-column-width - 0.5)"
																			/>
																		</xsl:when>
																		<xsl:otherwise>
																			<xsl:value-of
																				select="($catgry-freq div $catgry-max-freq) * ($bar-column-width - 0.5)"
																			/>
																		</xsl:otherwise>
																	</xsl:choose>
																</xsl:variable>
																<xsl:variable name="col-width-1">
																<!--	ToDO: handle exceptions regarding column-width	-->
																	<xsl:choose>
																		<xsl:when test="string(number($tmp-col-width-1)) != 'NaN'">
																			<xsl:value-of select="$tmp-col-width-1"/>
																		</xsl:when>
																		<xsl:otherwise>
																			0
																		</xsl:otherwise>
																	</xsl:choose>
																</xsl:variable>
																<!-- compute remaining space for second column -->
																<xsl:variable name="col-width-2"
																	select="$bar-column-width - $col-width-1"/>
																<!-- display the bar but not for missing values or if there was a problem computing the width -->
																<xsl:if
																	test="not(@missing='Y') and $col-width-1 > 0">
																	<fo:table-cell text-align="left"
																		border="0.5pt solid white" padding="2pt">
																		<fo:table table-layout="fixed" width="100%">
																			<fo:table-column column-width="{$col-width-1}in"/>
																			<fo:table-column column-width="{$col-width-2}in"/>
																			<fo:table-body>
																				<fo:table-row>
																					<fo:table-cell background-color="{$color-gray4}">
																						<fo:block> </fo:block>
																					</fo:table-cell>
																					<fo:table-cell margin-left="0.05in">
																						<fo:block>
																							<xsl:value-of
																								select="format-number($catgry-pct , '#0.0%')"/>
																						</fo:block>
																					</fo:table-cell>
																				</fo:table-row>
																			</fo:table-body>
																		</fo:table>
																		<!-- end bar table -->
																	</fo:table-cell>
																</xsl:if>

															</fo:table-row>
														</xsl:for-each>
														<!-- category total -->
														<!-- TODO -->
													</fo:table-body>
												</fo:table>
												<!-- end category table -->
												<fo:block font-weight="bold" color="#400000"
													font-size="6pt" font-style="italic">
													<xsl:value-of
														select="$msg/*/entry[@key='SumStat_Warning']"/>
												</fo:block>
											</fo:table-cell>
										</xsl:when>
										<xsl:otherwise>
											<fo:table-cell background-color="{$color-gray1}" text-align="center" font-style="italic" border="{$default-border}" number-columns-spanned="2" padding="{$cell-padding}">
												<fo:block>
													<xsl:value-of select="$msg/*/entry[@key='Frequency_table_not_shown']"/>
													<xsl:text> </xsl:text>(<xsl:value-of select="$category-count"/>
													<xsl:text> </xsl:text><xsl:value-of select="$msg/*/entry[@key='Modalities']"/>)
												</fo:block>
											</fo:table-cell>
										</xsl:otherwise>
									</xsl:choose>

								</fo:table-row>
							</xsl:if>
						</fo:table-body>
					</fo:table>
					<!-- end of variable table -->
				</fo:table-cell>
			</fo:table-row>
		</xsl:if>
	</xsl:template>
	<!--
		ddi:timePrd
	-->
	<xsl:template match="ddi:timePrd">
		<fo:block>
			<xsl:if test="@cycle">
				<xsl:value-of select="@cycle"/>
				<xsl:text>: </xsl:text>
			</xsl:if>
			<xsl:if test="@event">
				<xsl:value-of select="@event"/>
				<xsl:text> </xsl:text>
			</xsl:if>
			<xsl:value-of select="@date"/>
		</fo:block>
	</xsl:template>
	<!--
		RDF Description
	-->
	<xsl:template match="rdf:Description">
		<fo:block id="{generate-id()}" background-color="{$color-gray1}" space-after="0.2in"
			border-top="0.5pt solid {$color-gray2}" border-bottom="0.5pt solid {$color-gray2}"
			padding-bottom="0.05in" padding-top="0.05in">
			<!-- title -->
			<fo:inline font-weight="bold">
				<xsl:choose>
					<xsl:when test="normalize-space(dc:title)">
						<xsl:value-of select="normalize-space(dc:title)"/>
					</xsl:when>
					<xsl:otherwise>*** <xsl:value-of select="$msg/*/entry[@key='Untitled']"/>
						***</xsl:otherwise>
				</xsl:choose>
			</fo:inline>
			<!-- subtitle -->
			<xsl:if test="normalize-space(dcterms:alternative)">
				<xsl:text>, </xsl:text>
				<fo:inline font-style="italic">
					<xsl:value-of select="normalize-space(dcterms:alternative)"/>
				</fo:inline>
			</xsl:if>
			<!-- author -->
			<xsl:if test="normalize-space(dc:creator)">
				<xsl:text>, </xsl:text>
				<xsl:value-of select="normalize-space(dc:creator)"/>
			</xsl:if>
			<!-- date -->
			<xsl:if test="normalize-space(dcterms:created)">
				<xsl:variable name="date" select="normalize-space(dcterms:created)"/>
				<xsl:text>, </xsl:text>
				<xsl:if test="string-length($date) >= 7">
					<xsl:call-template name="isodate-month">
						<xsl:with-param name="isodate" select="$date"/>
					</xsl:call-template>
					<xsl:text> </xsl:text>
				</xsl:if>
				<xsl:if test="string-length($date) >= 4">
					<xsl:value-of select="substring($date,1,4)"/>
				</xsl:if>
			</xsl:if>
			<!-- country -->
			<xsl:if test="normalize-space(dcterms:spatial)">
				<xsl:text>, </xsl:text>
				<xsl:value-of select="normalize-space(dcterms:spatial)"/>
			</xsl:if>
			<!-- language -->
			<xsl:if test="normalize-space(dc:language)">
				<xsl:text>, </xsl:text>
				<xsl:value-of select="normalize-space(dc:language)"/>
			</xsl:if>
			<!-- source -->
			<xsl:if test="normalize-space(@rdf:about)">
				<xsl:text>,  "</xsl:text>
				<xsl:value-of select="normalize-space(@rdf:about)"/>
				<xsl:text>"</xsl:text>
			</xsl:if>
			<!-- description -->
			<xsl:if test="boolean($show-documentation-description)">
				<xsl:if test="normalize-space(dc:description)">
					<fo:block background-color="{$color-gray2}"
						border-top="0.5pt solid {$color-white}" font-size="8pt" font-weight="bold"
						padding-top="0.05in" space-before="0.05in">
						<xsl:value-of select="$msg/*/entry[@key='Description']"/>
					</fo:block>
					<fo:block font-size="8pt" padding-top="0.05in" white-space-collapse="false">
						<xsl:call-template name="trim">
							<xsl:with-param name="s" select="dc:description"/>
						</xsl:call-template>
					</fo:block>
				</xsl:if>
			</xsl:if>
			<!-- abstract -->
			<xsl:if test="boolean($show-documentation-abstract)">
				<xsl:if test="normalize-space(dcterms:abstract)">
					<fo:block background-color="{$color-gray2}"
						border-top="0.5pt solid {$color-white}" font-size="8pt" font-weight="bold"
						padding-top="0.05in" space-before="0.05in">
						<xsl:value-of select="$msg/*/entry[@key='Abstract']"/>
					</fo:block>
					<fo:block font-size="8pt" padding-top="0.05in" white-space-collapse="false">
						<xsl:call-template name="trim">
							<xsl:with-param name="s" select="dcterms:abstract"/>
						</xsl:call-template>
					</fo:block>
				</xsl:if>
			</xsl:if>
			<!-- toc-->
			<xsl:if test="boolean($show-documentation-toc)">
				<xsl:if test="normalize-space(dcterms:tableOfContents)">
					<fo:block background-color="{$color-gray2}"
						border-top="0.5pt solid {$color-white}" font-size="8pt" font-weight="bold"
						padding-top="0.05in" space-before="0.05in">
						<xsl:value-of select="$msg/*/entry[@key='Table_of_Contents']"/>
					</fo:block>
					<fo:block font-size="8pt" padding-top="0.05in" white-space-collapse="false">
						<xsl:call-template name="trim">
							<xsl:with-param name="s" select="dcterms:tableOfContents"/>
						</xsl:call-template>
					</fo:block>
				</xsl:if>
			</xsl:if>
			<!-- subjects -->
			<xsl:if test="boolean($show-documentation-subjects)">
				<xsl:if test="normalize-space(dc:subject)">
					<fo:block background-color="{$color-gray2}"
						border-top="0.5pt solid {$color-white}" font-size="8pt" font-weight="bold"
						padding-top="0.05in" space-before="0.05in">
						<xsl:value-of select="$msg/*/entry[@key='Subjects']"/>
					</fo:block>
					<fo:block font-size="8pt" padding-top="0.05in" white-space-collapse="false">
						<xsl:for-each select="dc:subject">
							<xsl:if test="position()>1">
								<xsl:text>, </xsl:text>
							</xsl:if>
							<xsl:value-of select="normalize-space(.)"/>
						</xsl:for-each>
					</fo:block>
				</xsl:if>
			</xsl:if>
		</fo:block>
	</xsl:template>
	<!--
		DEFAULT TEXT
	-->
	<xsl:template match="ddi:*|text()">
	   
      <!-- If documentation doesnt contain HTML -->
	   <xsl:if test="$allowHTML=0">
	      <xsl:variable name="trimmed">
	         <xsl:call-template name="trim">
	            <xsl:with-param name="s" select="."/>
	         </xsl:call-template>
	      </xsl:variable>
	      <fo:block linefeed-treatment="preserve" white-space-collapse="false" space-after="0.0in">
	         <xsl:value-of select="$trimmed"/>
	      </fo:block>
	   </xsl:if>
	   
	   <!-- If documentation contain HTML -->
	   <xsl:if test="$allowHTML=1">
	      <xsl:call-template name="FixHTML">
	         <xsl:with-param name="InputString" select="."/>
	      </xsl:call-template>      
	   </xsl:if>
	   
	</xsl:template>
	<!--
		VARIABLE TABLE HEADER
	-->
	<xsl:template name="variables-table-col-width">
		<fo:table-column column-width="proportional-column-width( 5)"/>
		<fo:table-column column-width="proportional-column-width(12)"/>
		<fo:table-column column-width="proportional-column-width(20)"/>
		<fo:table-column column-width="proportional-column-width(10)"/>
		<fo:table-column column-width="proportional-column-width(10)"/>
		<fo:table-column column-width="proportional-column-width( 8)"/>
		<fo:table-column column-width="proportional-column-width( 8)"/>
		<xsl:if test="$show-variables-list-question">
			<fo:table-column column-width="proportional-column-width(27)"/>
		</xsl:if>
	</xsl:template>
	<xsl:template name="variables-table-col-header">
		<fo:table-row text-align="center" vertical-align="top" font-weight="bold"
			keep-with-next="always">
			<fo:table-cell border="0.5pt solid black" padding="3pt">
				<fo:block>#</fo:block>
			</fo:table-cell>
			<fo:table-cell border="0.5pt solid black" padding="3pt">
				<fo:block>
					<xsl:value-of select="$msg/*/entry[@key='Name']"/>
				</fo:block>
			</fo:table-cell>
			<fo:table-cell border="0.5pt solid black" padding="3pt">
				<fo:block>
					<xsl:value-of select="$msg/*/entry[@key='Label']"/>
				</fo:block>
			</fo:table-cell>
			<fo:table-cell border="0.5pt solid black" padding="3pt">
				<fo:block>
					<xsl:value-of select="$msg/*/entry[@key='Type']"/>
				</fo:block>
			</fo:table-cell>
			<fo:table-cell border="0.5pt solid black" padding="3pt">
				<fo:block>
					<xsl:value-of select="$msg/*/entry[@key='Format']"/>
				</fo:block>
			</fo:table-cell>
			<fo:table-cell border="0.5pt solid black" padding="3pt">
				<fo:block>
					<xsl:value-of select="$msg/*/entry[@key='Valid']"/>
				</fo:block>
			</fo:table-cell>
			<fo:table-cell border="0.5pt solid black" padding="3pt">
				<fo:block>
					<xsl:value-of select="$msg/*/entry[@key='Invalid']"/>
				</fo:block>
			</fo:table-cell>
			<xsl:if test="$show-variables-list-question">
				<fo:table-cell border="0.5pt solid black" padding="3pt">
					<fo:block>
						<xsl:value-of select="$msg/*/entry[@key='Question']"/>
					</fo:block>
				</fo:table-cell>
			</xsl:if>
		</fo:table-row>
	</xsl:template>
	<!--
		PAGE HEADER / FOOTER
	-->
	<!-- header -->
	<xsl:template name="header">
		<xsl:param name="section"/>
		<fo:static-content flow-name="xsl-region-before">
			<fo:block font-size="6" text-align="center">
				<xsl:value-of select="/ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:titl"
				/> - <xsl:value-of select="$section"/>
			</fo:block>
		</fo:static-content>
	</xsl:template>
	<!-- footer -->
	<xsl:template name="footer">
		<fo:static-content flow-name="xsl-region-after">
			<fo:block font-size="6" text-align="center" space-before="0.3in">- <fo:page-number/>
				-</fo:block>
		</fo:static-content>
	</xsl:template>
	<!--
		UTILITIES
	-->
	<xsl:template name="isodate-long">
		<xsl:param name="isodate" select=" '2005-12-31' "/>
		<xsl:variable name="month">
			<xsl:call-template name="isodate-month">
				<xsl:with-param name="isodate" select="$report-date"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="contains('fr es',$language-code)">
				<!-- european format -->
				<xsl:value-of select="number(substring($isodate,9,2))"/>
				<xsl:text> </xsl:text>
				<xsl:value-of select="$month"/>
				<xsl:text> </xsl:text>
				<xsl:value-of select="substring($isodate,1,4)"/>
			</xsl:when>
			<xsl:when test="contains('ja',$language-code)">
				<!-- japanese format -->
				<xsl:value-of select="$isodate"/>
			</xsl:when>
			<xsl:otherwise>
				<!-- english format -->
				<xsl:value-of select="$month"/>
				<xsl:text> </xsl:text>
				<xsl:value-of select="number(substring($isodate,9,2))"/>
				<xsl:text>, </xsl:text>
				<xsl:value-of select="substring($isodate,1,4)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="isodate-month">
		<xsl:param name="isodate" select=" '2005-12-31' "/>
		<xsl:variable name="month" select="number(substring($isodate,6,2))"/>
		<xsl:choose>
			<xsl:when test="$month=1">
				<xsl:value-of select="$msg/*/entry[@key='January']"/>
			</xsl:when>
			<xsl:when test="$month=2">
				<xsl:value-of select="$msg/*/entry[@key='February']"/>
			</xsl:when>
			<xsl:when test="$month=3">
				<xsl:value-of select="$msg/*/entry[@key='March']"/>
			</xsl:when>
			<xsl:when test="$month=4">
				<xsl:value-of select="$msg/*/entry[@key='April']"/>
			</xsl:when>
			<xsl:when test="$month=5">
				<xsl:value-of select="$msg/*/entry[@key='May']"/>
			</xsl:when>
			<xsl:when test="$month=6">
				<xsl:value-of select="$msg/*/entry[@key='June']"/>
			</xsl:when>
			<xsl:when test="$month=7">
				<xsl:value-of select="$msg/*/entry[@key='July']"/>
			</xsl:when>
			<xsl:when test="$month=8">
				<xsl:value-of select="$msg/*/entry[@key='August']"/>
			</xsl:when>
			<xsl:when test="$month=9">
				<xsl:value-of select="$msg/*/entry[@key='September']"/>
			</xsl:when>
			<xsl:when test="$month=10">
				<xsl:value-of select="$msg/*/entry[@key='October']"/>
			</xsl:when>
			<xsl:when test="$month=11">
				<xsl:value-of select="$msg/*/entry[@key='November']"/>
			</xsl:when>
			<xsl:when test="$month=12">
				<xsl:value-of select="$msg/*/entry[@key='December']"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- left trim -->
	<xsl:template name="ltrim">
		<xsl:param name="s"/>
		<xsl:variable name="s-no-ws" select="translate($s,' &#9;&#10;&#13;','$%*!')"/>
		<xsl:variable name="s-first-non-ws" select="substring($s-no-ws,1,1)"/>
		<xsl:variable name="s-no-leading-ws"
			select="concat($s-first-non-ws,substring-after($s,$s-first-non-ws))"/>
		<xsl:value-of select="concat('[',$s-first-non-ws,'|',$s-no-ws,']')"/>
	</xsl:template>
	<!-- right trim -->
	<xsl:template name="rtrim">
		<xsl:param name="s"/>
		<xsl:param name="i" select="string-length($s)"/>
		<xsl:choose>
			<xsl:when test="translate(substring($s,$i,1),' &#9;&#10;&#13;','')">
				<xsl:value-of select="substring($s,1,$i)"/>
			</xsl:when>
			<xsl:when test="$i&lt;2"/>
			<xsl:otherwise>
				<xsl:call-template name="rtrim">
					<xsl:with-param name="s" select="$s"/>
					<xsl:with-param name="i" select="$i - 1"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- trim -->
	<xsl:template name="trim">
		<xsl:param name="s"/>
		<xsl:call-template name="rtrim">
			<xsl:with-param name="s"
				select="concat(substring(translate($s,' &#9;&#10;&#13;',''),1,1),substring-after($s,substring(translate($s,' &#9;&#10;&#13;',''),1,1)))"
			/>
		</xsl:call-template>
	</xsl:template>
   
   <!-- Creates FOP equivalent from a subset of HTML  -->
   <xsl:template name="FixHTML">
      <xsl:param name="InputString"/>
      <xsl:variable name="headStart" >
         <xsl:text>&lt;h2&gt;</xsl:text>
      </xsl:variable>
      <xsl:variable name="headEnd" >
         <xsl:text>&lt;/h2&gt;</xsl:text>
      </xsl:variable>
      <xsl:variable name="break" >
         <xsl:text>&lt;br/&gt;</xsl:text>
      </xsl:variable>
      
      <xsl:choose>
         <!--  Make a header -->
         <xsl:when test="
            (contains($InputString,$headEnd) and 
            string-length(substring-before($InputString,$headEnd)) &lt; string-length(substring-before($InputString,$break)))
            or (not(contains($InputString,$break))and contains($InputString,$headEnd))
            ">
            <xsl:variable name="beforeEnd" select="substring-before($InputString,$headEnd)"/>
            <fo:block font-weight="bold">
               <xsl:value-of select="substring-after($beforeEnd,$headStart)"/>
            </fo:block>
            <xsl:call-template name="FixHTML">
               <xsl:with-param name="InputString">
                  <xsl:value-of select="substring-after($InputString,$headEnd)"/>
               </xsl:with-param>
            </xsl:call-template>
         </xsl:when>
         <!-- Make a newline -->
         <xsl:when test="contains($InputString,$break)">
            <xsl:if test="string-length(substring-before($InputString,$break))=0">
               <fo:block>
                  &#160;      
               </fo:block>
         </xsl:if>
            <fo:block>
               <xsl:value-of select="substring-before($InputString,$break)"/>
            </fo:block>              
            <xsl:call-template name="FixHTML">
               <xsl:with-param name="InputString">
                  <xsl:value-of select="substring-after($InputString,$break)"/>
               </xsl:with-param>
            </xsl:call-template> 
         </xsl:when>
         <!-- If no headers or breaks left in string, display all -->
         <xsl:otherwise>
            <fo:block>
               <xsl:value-of select="$InputString"/>
            </fo:block>
         </xsl:otherwise>
      </xsl:choose>
      
   </xsl:template>
   
	<!-- EXSLT -->
	<xsl:template name="date:date">
		<xsl:param name="date-time">
			<xsl:choose>
				<xsl:when test="function-available('date:date-time')">
					<xsl:value-of select="date:date-time()"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$date:date-time"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:param>
		<xsl:variable name="neg" select="starts-with($date-time, '-')"/>
		<xsl:variable name="dt-no-neg">
			<xsl:choose>
				<xsl:when test="$neg or starts-with($date-time, '+')">
					<xsl:value-of select="substring($date-time, 2)"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$date-time"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="dt-no-neg-length" select="string-length($dt-no-neg)"/>
		<xsl:variable name="timezone">
			<xsl:choose>
				<xsl:when test="substring($dt-no-neg, $dt-no-neg-length) = 'Z'">Z</xsl:when>
				<xsl:otherwise>
					<xsl:variable name="tz" select="substring($dt-no-neg, $dt-no-neg-length - 5)"/>
					<xsl:if
						test="(substring($tz, 1, 1) = '-' or
						substring($tz, 1, 1) = '+') and
						substring($tz, 4, 1) = ':'">
						<xsl:value-of select="$tz"/>
					</xsl:if>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="date">
			<xsl:if
				test="not(string($timezone)) or
				$timezone = 'Z' or
				(substring($timezone, 2, 2) &lt;= 23 and
				substring($timezone, 5, 2) &lt;= 59)">
				<xsl:variable name="dt"
					select="substring($dt-no-neg, 1, $dt-no-neg-length - string-length($timezone))"/>
				<xsl:variable name="dt-length" select="string-length($dt)"/>
				<xsl:if
					test="number(substring($dt, 1, 4)) and
					substring($dt, 5, 1) = '-' and
					substring($dt, 6, 2) &lt;= 12 and
					substring($dt, 8, 1) = '-' and
					substring($dt, 9, 2) &lt;= 31 and
					($dt-length = 10 or
					(substring($dt, 11, 1) = 'T' and
					substring($dt, 12, 2) &lt;= 23 and
					substring($dt, 14, 1) = ':' and
					substring($dt, 15, 2) &lt;= 59 and
					substring($dt, 17, 1) = ':' and
					substring($dt, 18) &lt;= 60))">
					<xsl:value-of select="substring($dt, 1, 10)"/>
				</xsl:if>
			</xsl:if>
		</xsl:variable>
		<xsl:if test="string($date)">
			<xsl:if test="$neg">-</xsl:if>
			<xsl:value-of select="$date"/>
			<xsl:value-of select="$timezone"/>
		</xsl:if>
	</xsl:template>
	<xsl:template name="math:max">
		<xsl:param name="nodes" select="/.."/>
		<xsl:choose>
			<xsl:when test="not($nodes)">NaN</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="$nodes">
					<xsl:sort data-type="number" order="descending"/>
					<xsl:if test="position() = 1">
						<xsl:value-of select="number(.)"/>
					</xsl:if>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
