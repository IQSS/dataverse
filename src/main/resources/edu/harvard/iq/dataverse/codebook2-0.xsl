<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="part"/>

<xsl:param name="var-token"/>

<xsl:param name="varGrp-token"/>

<xsl:variable name="filename"><xsl:value-of select="codeBook/docDscr/citation/holdings/@URI"/></xsl:variable>

    <xsl:template match="codeBook">

<xsl:comment>
This work is licensed under a Creative Commons Attribution-Noncommercial 3.0 United States License.
http://creativecommons.org/licenses/by-nc/3.0/us/
</xsl:comment>

        <xsl:comment>This XML stylesheet is intended to serve as a generic stylesheet for HTML
            display of codebooks marked up according to Version 2.0 of the DDI DTD. Please note that 
            the part navigation that appears just below the title is dependent upon the 'holdings' tag
            located at /codeBook/docDscr/citation/holdings. This navigation element allows the user
            to selectively view any of the five parts of the DDI document, assuming those parts are
            present. The URI attribute of the 'holdings' tag should contain the URL at which the XML
            file is accessed on the Web. If that field is not filled in properly, the navigation
            will not work correctly.</xsl:comment>
        <xsl:comment>It's also important to note that this stylesheet doesn't display all elements. 
            The following elements do note display:
            3.2.1 dataItem (locMap)
            3.2.1.1 CubeCoord (locMap)
            3.2.1.2 physLoc (locMap)
            4.2.1 labl (nCubeGrp)
            4.2.2 txt (nCubeGrp)
            4.2.3 concept (nCubeGrp)
            4.2.4 defntn (nCubeGrp)
            4.2.5 universe (nCubeGrp)
            4.2.6 notes (nCubeGrp)
            4.3.18.5 mrow (nCube)
            4.3.18.5.1 mi (nCube)
            4.4.2 location (nCube)
            4.4.3 txt (nCube)
            4.4.4 universe (nCube)
            4.4.5 imputation (nCube)
            4.4.6 security (nCube)
            4.4.7 embargo (nCube)
            4.4.8 respUnit (nCube)
            4.4.9 anlysUnit (nCube)
            4.4.10 verStmt (nCube)
            4.4.10.1 version (nCube)
            4.4.10.2 verResp (nCube)
            4.4.10.3 notes (nCube)
            4.4.11 purpose (nCube)
            4.4.12.1 cohort (nCube)
            4.4.12.1.1 range (nCube)
            4.4.13 measure (nCube)
            4.4.14 notes (nCube)
            
            In addition, many of the attributes of elements have yet to be set to display.
            </xsl:comment>
            <xsl:comment>
            On 2004-06-01 I corrected some errors in this stylesheet, as lists of variables and variable 
            groups weren't linking properly to the detailed views of said variables and variable groups.
            
            On 2005-06-22 I made some minor modifications to the display of variables lists, and enabled
            use of the Link tag within catValu.
            
            On 2005-06-23 I enabled use of Link tag within labl and enabled catStat to display repetitions
            in catgry.
            </xsl:comment>
            
            
        <html>
            <head>
                <title>
                    <xsl:value-of select="stdyDscr/citation/titlStmt/titl"/>
                </title>
				<style type="text/css" media="screen">
body {
background-color: #FFFFFF;
margin-top: 5px;
margin-left: 5px;
margin-right: 5px;
padding-left: 0px;
padding-top: 0px;
padding-right: 0px;
}

tr.h1 {
background: #000000;
color: #FFFFFF;
}

td.h3 {
vertical-align: top;
text-align: right;
font-style: italic;
}

tr.h2 {
background: #CCCCCC;
color: #000000;
}

caption {
font-family: arial, helvetica, geneva, verdana, sans-serif;
font-size: 11pt;
font-weight: bold;
}

p {
font-family: arial, helvetica, geneva, verdana, sans-serif;
font-size: 11pt;
}

table {
padding-top: 10px;
padding-bottom: 10px;
border: none;
}

th {
text-align: left;
}

dl {
font-family: arial, helvetica, geneva, verdana, sans-serif;
font-size: 11pt;
}

td {
vertical-align: top;
}

blockquote {
font-family: arial, helvetica, geneva, verdana, sans-serif;
font-size: 11pt;
}

.noindent {padding-left: 0px;}

.small {font-size: 9pt;}

.red {color: #CC0000;}

ol {
font-family: arial, helvetica, geneva, verdana, sans-serif;
font-size: 11pt;
}

ul {
font-family: arial, helvetica, geneva, verdana, sans-serif;
list-style: square;
font-size: 11pt;
}

h1 {
font-family: arial, helvetica, geneva, verdana, sans-serif; 
font-size: 14pt;
}

h2 {
font-family: arial, helvetica, geneva, verdana, sans-serif; 
font-size: 12pt;
}

h3 {
font-family: arial, helvetica, geneva, verdana, sans-serif; 
font-size: 11pt;
font-style: bold;
}

h4 {
font-family: arial, helvetica, geneva, verdana, sans-serif; 
font-size: 10pt;
}
				</style>
            </head>
            <body bgcolor="#ffffff" lang="en-US">
                <h1>
                    <xsl:value-of select="stdyDscr/citation/titlStmt/titl"/> (<xsl:value-of
                        select="stdyDscr/citation/titlStmt/IDNo"/>)<xsl:if test="stdyDscr/citation/titlStmt/altTitl">
                        <br/>(<xsl:value-of select="stdyDscr/citation/titlStmt/altTitl"/>)</xsl:if>
                </h1>

                <table border="0" cellpadding="5" cellspacing="0">
                    <tr>
                        <td valign="top">
                            <p>
                                <b>View:</b>
                            </p>
                        </td>
                        <td>
                            <p>
                                <xsl:if test="docDscr">
                                    <xsl:element name="a">
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                                select="$filename"/>#1.0</xsl:attribute>Part 1: Document Description</xsl:element>
                                    <br/>
                                </xsl:if>
                                <xsl:if test="stdyDscr">
                                    <xsl:element name="a">
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                        select="$filename"/>#2.0</xsl:attribute>Part 2: Study Description</xsl:element>
                                    <br/>
                                </xsl:if>
                                <xsl:if test="fileDscr">
                                    <xsl:element name="a">
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                        select="$filename"/>#3.0</xsl:attribute>Part 3: Data
                                        Files Description</xsl:element>
                                    <br/>
                                </xsl:if>
                                <xsl:if test="dataDscr">
                                    <xsl:element name="a">
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                        select="$filename"/>#4.0</xsl:attribute>Part 4: Variable Description</xsl:element>
                                    <br/>
                                </xsl:if>
                                <xsl:if test="otherMat">
                                    <xsl:element name="a">
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                        select="$filename"/>#5.0</xsl:attribute>Part 5: Other
                                        Study-Related Materials</xsl:element>
                                    <br/>
                                </xsl:if>
                                <xsl:element name="a">
                                    <xsl:attribute name="href">
                                        <xsl:value-of
                                    select="$filename"/></xsl:attribute>Entire Codebook</xsl:element>
                            </p>
                        </td>
                    </tr>
                </table>

                
                <table border="0" cellpadding="5" cellspacing="5">

                    <xsl:choose>
                        <xsl:when test="$part=1">
                            <xsl:apply-templates select="docDscr"/>
                        </xsl:when>
                        <xsl:when test="$part=2">
                            <xsl:apply-templates select="stdyDscr"/>
                        </xsl:when>
                        <xsl:when test="$part=3">
                            <xsl:apply-templates select="fileDscr"/>
                        </xsl:when>
                        <xsl:when test="$part=4">
                            <xsl:apply-templates select="dataDscr"/>
                        </xsl:when>
                        <xsl:when test="$part=5">
                            <xsl:apply-templates select="otherMat"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select="docDscr"/>
                            <xsl:apply-templates select="stdyDscr"/>
                            <xsl:apply-templates select="fileDscr"/>
                            <xsl:apply-templates select="dataDscr"/>
                            <xsl:apply-templates select="otherMat"/>
                        </xsl:otherwise>
                    </xsl:choose>

                </table>
           </body>
        </html>
    </xsl:template>

<!-- begin docDscr templates -->

    <xsl:template match="docDscr">
        <tr class="h1">
            <th align="left" colspan="2">
                <p><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                    <a name="1.0" id="1.0">Document Description</a>
                </p>
            </th>
        </tr>
        <xsl:apply-templates select="citation"/>
        <xsl:apply-templates select="guide"/>
        <xsl:apply-templates select="docStatus"/>
        <xsl:apply-templates select="docSrc"/>
        <xsl:apply-templates select="notes" mode="row" />
    </xsl:template>

    <xsl:template match="citation">
        <tr class="h2">
            <th colspan="2"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Citation</p>
            </th>
        </tr>
        <xsl:apply-templates select="titlStmt"/>
        <xsl:apply-templates select="rspStmt"/>
        <xsl:apply-templates select="prodStmt"/>
        <xsl:apply-templates select="distStmt"/>
        <xsl:apply-templates select="serStmt"/>
        <xsl:apply-templates select="verStmt" mode="row" />
        <xsl:apply-templates select="biblCit"/>
        <xsl:apply-templates select="holdings"/>
        <xsl:apply-templates select="notes" mode="row" />
    </xsl:template>

    <xsl:template match="titlStmt">
        <xsl:apply-templates select="titl"/>
        <xsl:apply-templates select="subTitl"/>
        <xsl:apply-templates select="altTitl"/>
        <xsl:apply-templates select="parTitl"/>
        <xsl:apply-templates select="IDNo"/>
    </xsl:template>
 
    <xsl:template match="titl">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Title:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
 
    <xsl:template match="subTitl">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Subtitle:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
 
    <xsl:template match="altTitl">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Alternative Title:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
 
    <xsl:template match="parTitl">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Parallel Title:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
 
    <xsl:template match="IDNo">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Identification Number:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
 
    <xsl:template match="rspStmt">
        <xsl:apply-templates select="AuthEnty"/>
        <xsl:apply-templates select="othId"/>
    </xsl:template>
 
    <xsl:template match="AuthEnty">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <xsl:if test="position()=1"><p>Authoring Entity:</p></xsl:if>
            </td>
            <td>
                <p>
                    <xsl:apply-templates /><xsl:if test="@affiliation"> (<xsl:value-of select="@affiliation" />)</xsl:if>
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="othId">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Other identifications and acknowledgements:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="prodStmt">
        <xsl:apply-templates select="producer"/>
        <xsl:apply-templates select="copyright"/>
        <xsl:apply-templates select="prodDate"/>
        <xsl:apply-templates select="prodPlace"/>
        <xsl:apply-templates select="software" mode="row"/>
        <xsl:apply-templates select="fundAg"/>
        <xsl:apply-templates select="grantNo"/>
    </xsl:template>
    <xsl:template match="producer">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <xsl:if test="position()=1"><p>Producer:</p></xsl:if>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="copyright">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Copyright:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="prodDate">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Date of Production:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="prodPlace">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Place of Production:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="software" mode="row">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Software used in Production:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="fundAg">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Funding Agency/Sponsor:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="grantNo">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Grant Number:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="distStmt">
        <xsl:apply-templates select="distrbtr"/>
        <xsl:apply-templates select="contact"/>
        <xsl:apply-templates select="depositr"/>
        <xsl:apply-templates select="depDate"/>
        <xsl:apply-templates select="distDate"/>
    </xsl:template>
    <xsl:template match="distrbtr">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Distributor:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="contact">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Contact Persons:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="depositr">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Depositor:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="depDate">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Date of Deposit:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="distDate">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Date of Distribution:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="serStmt">
        <xsl:apply-templates select="serName"/>
        <xsl:apply-templates select="serInfo"/>
    </xsl:template>
    <xsl:template match="serName">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Series Name:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="serInfo">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Series Information:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="verStmt" mode="row">
        <xsl:apply-templates select="version" mode="row" />
        <xsl:apply-templates select="verResp" mode="row" />
        <xsl:apply-templates select="notes" mode="row" />
    </xsl:template>
    <xsl:template match="version" mode="row">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Version:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="verResp" mode="row">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Version Responsibility:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="biblCit">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Bibliographic Citation:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="holdings">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Holdings Information:</p>
            </td>
            <td>
                <p>
                    <xsl:value-of select="."/>
                    <xsl:value-of select="@URI"/>
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="guide">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Guide to Codebook:</p>
            </td>
            <td>
                <p>
                    <xsl:value-of select="."/>
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="docStatus">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Documentation Status:</p>
            </td>
            <td>
                <p>
                    <xsl:value-of select="."/>
                </p>
            </td>
        </tr>
    </xsl:template>


    <xsl:template match="docSrc">
        <tr class="h2">
            <th colspan="2"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Documentation Source</p>
            </th>
        </tr>
        <xsl:apply-templates select="titlStmt"/>
        <xsl:apply-templates select="rspStmt"/>
        <xsl:apply-templates select="prodStmt"/>
        <xsl:apply-templates select="distStmt"/>
        <xsl:apply-templates select="serStmt"/>
        <xsl:apply-templates select="verStmt" mode="row" />
        <xsl:apply-templates select="biblCit"/>
        <xsl:apply-templates select="holdings"/>
        <xsl:apply-templates select="notes" mode="row" />
    </xsl:template>


<!-- end docDscr templates -->

<!-- begin stdyDscr templates -->

    <xsl:template match="stdyDscr">
        <tr class="h1">
            <th colspan="2">
                <p><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                    <a name="2.0">Study Description</a>
                </p>
            </th>
        </tr>
        <xsl:apply-templates select="citation"/>
        <xsl:apply-templates select="stdyInfo"/>
        <xsl:apply-templates select="method"/>
        <xsl:apply-templates select="dataAccs"/>
        <xsl:apply-templates select="othrStdyMat"/>
        <xsl:apply-templates select="notes" mode="row" />
    </xsl:template>

    <xsl:template match="stdyInfo">
        <tr class="h2">
            <th colspan="2"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Study Scope</p>
            </th>
        </tr>
        <xsl:apply-templates select="subject"/>
        <xsl:apply-templates select="abstract"/>
        <xsl:apply-templates select="sumDscr"/>
        <xsl:apply-templates select="notes" mode="row" />
    </xsl:template>

    <xsl:template match="subject">
 
        <xsl:if test="keyword">
            <tr>
                <td class="h3">
                <xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Keywords:</p></td>
                <td>
                    <p>
                        <xsl:apply-templates select="keyword"/>
                    </p>
                </td>
            </tr>
        </xsl:if>

        <xsl:if test="topcClas">
            <tr>
                <td class="h3">
                <xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Topic Classification:</p></td>
                <td>
                    <p>
                        <xsl:apply-templates select="topcClas"/>
                    </p>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>

<xsl:template match="keyword">
	<xsl:value-of select="." /><xsl:if test="position()!=last()">, </xsl:if>
</xsl:template>

<xsl:template match="topcClas">
	<xsl:apply-templates /><xsl:if test="position()!=last()">, </xsl:if>
</xsl:template>


    <xsl:template match="abstract">
                <tr>
                    <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                        <xsl:if test="position()=1"><p>Abstract:</p></xsl:if>
                    </td>
                    <td>
                        <p>
                            <xsl:apply-templates />
                        </p>
                    </td>
                </tr>
    </xsl:template>

    <xsl:template match="sumDscr">
        <xsl:if test="timePrd">
            <tr>
                <td class="h3">
                    <p>Time Period:</p>
                </td>
                <td>
                    <p>
                        <xsl:apply-templates select="timePrd"/>
                    </p>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="collDate">
            <tr>
                <td class="h3">
                    <p>Date of Collection:</p>
                </td>
                <td>
                    <p>
                        <xsl:apply-templates select="collDate"/>
                    </p>
                </td>
            </tr>
        </xsl:if>
        <xsl:if test="nation">
            <tr>
                <td class="h3">
                    <p>Country:</p>
                </td>
                <td>
                    <p>
                        <xsl:apply-templates select="nation"/>
                    </p>
                </td>
            </tr>
        </xsl:if>

        <xsl:if test="geogCover">
            <tr>
                <td class="h3">
                    <p>Geographic Coverage:</p>
                </td>
                <td>
                    <p>
                        <xsl:apply-templates select="geogCover"/>
                    </p>
                </td>
            </tr>
        </xsl:if>

        <xsl:if test="geogUnit">
            <tr>
                <td class="h3">
                    <p>Geographic Unit(s):</p>
                </td>
                <td>
                    <p>
                        <xsl:apply-templates select="geogUnit"/>
                    </p>
                </td>
            </tr>
        </xsl:if>

        <xsl:apply-templates select="geoBndBox"/>


        <xsl:apply-templates select="anlyUnit"/>
        <xsl:apply-templates mode="row" select="universe"/>
        <xsl:apply-templates select="dataKind"/>
    </xsl:template>

    <xsl:template match="timePrd">
        <xsl:choose>
            <xsl:when test="@event='start'">
                <xsl:apply-templates />-</xsl:when>
            <xsl:when test="@event='end'">
                <xsl:apply-templates />
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="collDate">
        <xsl:choose>
            <xsl:when test="@event='start'">
                <xsl:apply-templates />-</xsl:when>
            <xsl:when test="@event='end'">
                <xsl:apply-templates />
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="nation">
                <xsl:apply-templates />
        <xsl:if test="position()!=last()">, </xsl:if>
    </xsl:template>

    <xsl:template match="geogCover">
                <xsl:apply-templates />
        <xsl:if test="position()!=last()">, </xsl:if>
    </xsl:template>

    <xsl:template match="geogUnit">
                <xsl:apply-templates />
        <xsl:if test="position()!=last()">, </xsl:if>
    </xsl:template>



    <xsl:template match="geoBndBox">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Geographic Bounding Box:</p>
            </td>
            <td>
                <ul>
                    <xsl:apply-templates select="westBL" />
                    <xsl:apply-templates select="eastBL" />
                    <xsl:apply-templates select="southBL" />
                    <xsl:apply-templates select="northBL" />
               </ul>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="westBL">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        West Bounding Longitude: <xsl:apply-templates /></li>
    </xsl:template>

    <xsl:template match="eastBL">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        East Bounding Longitude: <xsl:apply-templates /></li>
    </xsl:template>

    <xsl:template match="southBL">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        South Bounding Latitude: <xsl:apply-templates /></li>
    </xsl:template>

    <xsl:template match="northBL">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        North Bounding Latitude: <xsl:apply-templates /></li>
    </xsl:template>

    <xsl:template match="boundPoly">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Geographic Bounding Polygon:</p>
            </td>
            <td>
                 <xsl:apply-templates select="polygon" />
            </td>
        </tr>
    </xsl:template>


    <xsl:template match="polygon">
        <xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <ul>
        <xsl:apply-templates select="point" />
        </ul>
    </xsl:template>

    <xsl:template match="point">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><xsl:apply-templates select="gringLat" />; <xsl:apply-templates select="gringLon" /></li>
    </xsl:template>

    <xsl:template match="gringLat">G-Ring Latitude: <xsl:apply-templates /></xsl:template>

    <xsl:template match="gringLon">G-Ring Longitude: <xsl:apply-templates /></xsl:template>

    <xsl:template match="anlyUnit">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Unit of Analysis:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="universe" mode="row">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Universe:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="dataKind">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Kind of Data:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="method">
        <tr class="h2">
            <th colspan="2"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Methodology and Processing</p>
            </th>
        </tr>
        <xsl:apply-templates select="dataColl"/>
        <xsl:apply-templates select="notes" mode="row" />
        <xsl:apply-templates select="anlyInfo"/>
        <xsl:apply-templates select="stdyClas"/>
    </xsl:template>
 
    <xsl:template match="dataColl">
        <xsl:apply-templates select="timeMeth"/>
        <xsl:apply-templates select="dataCollector"/>
        <xsl:apply-templates select="frequenc"/>
        <xsl:apply-templates select="sampProc"/>
        <xsl:apply-templates select="deviat"/>
        <xsl:apply-templates select="collMode"/>
        <xsl:apply-templates select="resInstru"/>
        <xsl:apply-templates select="sources"/>
        <xsl:apply-templates select="collSitu"/>
        <xsl:apply-templates select="actMin"/>
        <xsl:apply-templates select="ConOps"/>
        <xsl:apply-templates select="weight"/>
        <xsl:apply-templates select="cleanOps"/>
    </xsl:template>
    <xsl:template match="timeMeth">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Time Method:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="dataCollector">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <xsl:if test="position()=1"><p>Data Collector:</p></xsl:if>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="frequenc">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Frequency of Data Collection:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="sampProc">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <xsl:if test="position()=1"><p>Sampling Procedure:</p></xsl:if>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="deviat">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Major Deviations from the Sample Design:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="collMode">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Mode of Data Collection:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="resInstru">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Type of Research Instrument:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="sources">

<tr><th colspan="2"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
<p>Sources Statement</p></th></tr>

        <xsl:apply-templates select="dataSrc" />
        <xsl:apply-templates select="srcOrig" />
        <xsl:apply-templates select="srcChar" />
        <xsl:apply-templates select="srcDocu" />
        <xsl:apply-templates select="sources" />
    </xsl:template>

    <xsl:template match="dataSrc">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <xsl:if test="position()=1"><p>Data Sources:</p></xsl:if>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="srcOrig">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Origins of Sources:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="srcChar">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Characteristics of Source Notes:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="srcDocu">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Documentation and Access to Sources:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="collSitu">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Characteristics of Data Collection Situation:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="actMin">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Actions to Minimize Losses:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="ConOps">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Control Operations:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="weight">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Weighting:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="cleanOps">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Cleaning Operations:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
 
 
    <xsl:template match="anlyInfo">
        <xsl:apply-templates select="respRate"/>
        <xsl:apply-templates select="EstSmpErr"/>
        <xsl:apply-templates select="dataAppr"/>
    </xsl:template>
    <xsl:template match="respRate">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Response Rate:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="EstSmpErr">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Estimates of Sampling Error:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="dataAppr">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Other Forms of Data Appraisal:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="stdyClas">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Class of the Study:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="dataAccs">
        <tr class="h2">
            <th colspan="2"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Data Access</p>
            </th>
        </tr>
        <xsl:apply-templates select="setAvail"/>
        <xsl:apply-templates select="useStmt"/>
        <xsl:apply-templates select="notes" mode="row" />
    </xsl:template>

    <xsl:template match="setAvail">
        <xsl:apply-templates select="accsPlac"/>
        <xsl:apply-templates select="origArch"/>
        <xsl:apply-templates select="avlStatus"/>
        <xsl:apply-templates select="collSize"/>
        <xsl:apply-templates select="complete"/>
        <xsl:apply-templates select="fileQnty"/>
        <xsl:apply-templates select="notes" mode="row" />
    </xsl:template>

    <xsl:template match="accsPlac">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Location:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="origArch">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Archive Where Study was Originally Stored:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="avlStatus">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Availability Status:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="collSize">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Extent of Collection:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="complete">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Completeness of Study Stored:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="fileQnty">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Number of Files:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
 
    <xsl:template match="useStmt">
        <xsl:apply-templates select="confDec"/>
        <xsl:apply-templates select="specPerm"/>
        <xsl:apply-templates select="restrctn"/>
        <xsl:apply-templates select="contact"/>
        <xsl:apply-templates select="citReq"/>
        <xsl:apply-templates select="deposReq"/>
        <xsl:apply-templates select="conditions"/>
        <xsl:apply-templates select="disclaimer"/>
    </xsl:template>
    <xsl:template match="confDec">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Confidentiality Declaration:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="specPerm">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Special Permissions:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="restrctn">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Restrictions:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="contact">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Access Authority:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="citReq">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Citation Requirement:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="deposReq">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Deposit Requirement:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="conditions">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Conditions:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="disclaimer">
        <tr>
            <td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Disclaimer:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="othrStdyMat">
        <tr class="h2">
            <th colspan="2"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Other Study Description Materials</p>
            </th>
        </tr>
        <xsl:apply-templates select="relMat"/>
        <xsl:apply-templates select="relStdy"/>
        <xsl:apply-templates select="relPubl"/>
        <xsl:apply-templates select="othRefs"/>
    </xsl:template>

    <xsl:template match="relMat">
        <xsl:if test="position()=1">
        <tr>
            <th colspan="2">
                <p>Related Materials</p>
            </th>
        </tr>
        </xsl:if>

<xsl:choose>
<xsl:when test="citation">
        <xsl:apply-templates />
</xsl:when>
<xsl:otherwise>
	<tr>
		<td></td>
		<td><p><xsl:value-of select="." /></p></td>
	</tr>
</xsl:otherwise>
</xsl:choose>

    </xsl:template>

    <xsl:template match="relStdy">
        <xsl:if test="position()=1">
        <tr>
            <th colspan="2">
                <p>Related Studies</p>
            </th>
        </tr>
        </xsl:if>

<xsl:choose>
<xsl:when test="citation">
        <xsl:apply-templates />
</xsl:when>
<xsl:otherwise>
	<tr>
		<td></td>
		<td><p><xsl:value-of select="." /></p></td>
	</tr>
</xsl:otherwise>
</xsl:choose>

    </xsl:template>


    <xsl:template match="relPubl">
        <xsl:if test="position()=1">
        <tr>
            <th colspan="2">
                <p>Related Publications</p>
            </th>
        </tr>
        </xsl:if>
        
<xsl:choose>
<xsl:when test="citation">
        <xsl:apply-templates />
</xsl:when>
<xsl:otherwise>
	<tr>
		<td></td>
		<td><p><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><xsl:value-of select="." /></p></td>
	</tr>
</xsl:otherwise>
</xsl:choose>

    </xsl:template>

    <xsl:template match="othRefs">
        <xsl:if test="position()=1">
        <tr>
            <th colspan="2">
                <p>Other Reference Note(s)</p>
            </th>
        </tr>
        </xsl:if>

<xsl:choose>
<xsl:when test="citation">
        <xsl:apply-templates />
</xsl:when>
<xsl:otherwise>
	<tr>
		<td></td>
		<td><p><xsl:value-of select="." /></p></td>
	</tr>
</xsl:otherwise>
</xsl:choose>

    </xsl:template>

<!-- end stdyDscr templates -->

<!-- begin fileDscr templates -->

    <xsl:template match="fileDscr">
        <tr class="h1">
            <th colspan="2">
                <p>
                    <a name="3.0">File Description</a>
                    <xsl:if test="@ID">--<xsl:value-of select="@ID"/>
                    </xsl:if>
                </p>
            </th>
        </tr>


        <xsl:apply-templates select="fileTxt"/>
		<xsl:apply-templates select="locMap"/>
        <xsl:apply-templates select="notes" mode="row" />


    </xsl:template>
 
   <xsl:template match="locMap">
        <tr>
            <td class="h3"><p>Location Map:</p>
            </td>
            <td>
                <p>Location map data is present, but not displayed.</p>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="fileTxt">
        <tr class="h2">
            <th colspan="2"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>File<xsl:apply-templates select="fileName" /></p>
            </th>
           </tr>
           <tr><td></td>
            <td>
                <ul>
                    <xsl:apply-templates select="fileCont" />
                    <xsl:apply-templates select="fileStrc" />
                    <xsl:apply-templates select="dimensns" />
                    <xsl:apply-templates select="fileType" />
                    <xsl:apply-templates select="format" />
                    <xsl:apply-templates select="filePlac" />
                    <xsl:apply-templates select="dataChck" />
                    <xsl:apply-templates select="ProcStat" />
                    <xsl:apply-templates select="dataMsng" />
                    <xsl:apply-templates select="software" mode="list" />
                    <xsl:apply-templates select="verStmt" mode="list" />


                    <xsl:apply-templates select="notes" mode="list" />
               </ul>
  
<!-- ADD: locMap -->  

  
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="fileName"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>: <xsl:apply-templates /></xsl:template>
    <xsl:template match="fileCont">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Contents of Files: <xsl:apply-templates />
        </p></li>
    </xsl:template>
    <xsl:template match="fileStrc">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>File Structure: <xsl:value-of select="@type"/></p>
        </li>
        <xsl:apply-templates select="recGrp"/>
        <xsl:apply-templates mode="list" select="notes"/>
    </xsl:template>
    <xsl:template match="recGrp">
    	<li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Record Group</p>
      	<ul>
        <xsl:apply-templates select="labl" mode="list" />
        <xsl:apply-templates select="recDimnsn"/>
        </ul>
		</li>
    </xsl:template>
    <xsl:template match="labl" mode="list">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Label: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="recDimnsn">
        <xsl:apply-templates select="varQnty"/>
        <xsl:apply-templates select="caseQnty"/>
        <xsl:apply-templates select="logRecL"/>
    </xsl:template>
    <xsl:template match="varQnty">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>No. of variables per record: <xsl:apply-templates /></p></li>
    </xsl:template>
    <xsl:template match="caseQnty">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Number of cases: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="logRecL">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Logical Record Length: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="dimensns">
        <xsl:apply-templates select="caseQnty"/>
        <xsl:apply-templates select="varQnty"/>
        <xsl:apply-templates select="logRecL"/>
        <xsl:apply-templates select="recPrCas"/>
        <xsl:apply-templates select="recNumTot"/>
    </xsl:template>
    <xsl:template match="recPrCas">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Records per Case: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="recNumTot">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Overall Number of Records: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="fileType">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Type of File: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="format">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Data Format: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="filePlac">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Place of File Production: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="dataChck">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Extent of Processing Checks: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="ProcStat">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Processing Status: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="dataMsng">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Missing Data: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="software" mode="list">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Software: <xsl:apply-templates /></p>
        </li>
    </xsl:template>
    <xsl:template match="verStmt" mode="list">
        <xsl:apply-templates select="version" mode="list"/>
        <xsl:apply-templates select="verResp" mode="list"/>
        <xsl:apply-templates select="notes" mode="list"/>
    </xsl:template>
    <xsl:template match="version" mode="list">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Version: <xsl:apply-templates /></p></li>
    </xsl:template>
    <xsl:template match="verResp" mode="list">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if><p>Version Responsibility Statement: <xsl:apply-templates /></p></li>
    </xsl:template>
 
 <!-- end fileDscr templates -->
 
 <!-- begin dataDscr templates -->
 
 <!-- ADD: nCubeGrp nCube -->
 
    <xsl:template match="dataDscr">
        <tr class="h1">
            <th colspan="2">
                <p><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                    <a name="4.0">Variable Description</a>
                </p>
            </th>
        </tr>
        <xsl:choose>
            <xsl:when test="varGrp">
         		<tr class="h2">
         		   <th colspan="2"><p>Variable Groups</p></th>
       			</tr>

              <tr>
                    <td>
                    </td>
                    <td>
                        <ul>
                            <xsl:apply-templates mode="list" select="varGrp"/>
                        </ul>
                    </td>
                </tr>

                <xsl:apply-templates mode="detail" select="varGrp"/>

            </xsl:when>
            <xsl:otherwise>
                <tr>
                    <td class="h3">
                        <p>List of Variables:</p>
                    </td>
                    <td>
                        <ul>
                            <xsl:apply-templates mode="list" select="var"/>
                        </ul>
                    </td>
                </tr>
            </xsl:otherwise>
        </xsl:choose>
        <tr class="h2">
            <td colspan="2">
                <p>
                    <strong>Variables</strong>
                </p>
            </td>
        </tr>
 
        <xsl:apply-templates mode="detail" select="var"/>
 
 <xsl:if test="nCube">
 
<tr>
<td class="h3"><p>nCube(s):</p></td>
<td>

<xsl:if test="nCubeGrp"><p>nCube groups are present in this XML file.</p></xsl:if>

<table border="1" cellpadding="5" cellspacing="0" width="90%">
<tr>
<th><p>ID</p></th>
<th><p>Label</p></th>
<th><p>Cells</p></th>
<th><p>Variables</p></th>
</tr>

<xsl:apply-templates select="nCube" mode="row" />

</table>

</td>
</tr>

</xsl:if>

 		<xsl:apply-templates select="notes" mode="row" />
 
 
 
    </xsl:template>
 
 
    <xsl:template match="nCube" mode="row">
<tr>
<td><p><xsl:value-of select="@ID" /></p></td>
<td><p><xsl:value-of select="labl" /></p></td>
<td><p><xsl:value-of select="@cellQnty" /></p></td>
<td><p><xsl:apply-templates select="dmns" /></p></td>
</tr>

</xsl:template>

<xsl:template match="dmns"><xsl:value-of select="@varRef" />&#160;</xsl:template> 
 
 
    <xsl:template match="varGrp" mode="list">
        <li>
<xsl:element name="a">
        <xsl:attribute name="href">#<xsl:value-of select="@ID" /></xsl:attribute>
        <xsl:apply-templates mode="no-format" select="labl"/>
        </xsl:element>

<!--
<xsl:element name="a">
        <xsl:attribute name="href"><xsl:value-of select="$filename" />#<xsl:value-of select="@ID" /><xsl:if test="$part!=''">?part=<xsl:value-of select="$part" /></xsl:if>
        </xsl:attribute>
        <xsl:apply-templates mode="no-format" select="labl"/>
        </xsl:element>
-->

        </li>
 
    </xsl:template>
    <xsl:template match="varGrp" mode="detail">
  
                  <xsl:if test="@varGrp">
                    <tr>
                    <th colspan="2">
                    <p><xsl:element name="a">
                    <xsl:attribute name="name"><xsl:value-of select="@ID" /></xsl:attribute>
                    <xsl:attribute name="id"><xsl:value-of select="@ID" /></xsl:attribute>
                    <xsl:value-of select="labl"/>
                    </xsl:element>
                    </p>
                    </th>
                    </tr>
                    <tr><td></td>
                    <td>
                      <p>Variable Groups within <xsl:value-of select="labl"/></p>
                  <ul>
                        <xsl:apply-templates mode="list-varGrp" select="../varGrp">
                            <xsl:with-param name="varGrp-token" select="@varGrp"/>
                        </xsl:apply-templates>
                    </ul>
                    </td>
                    </tr>
                </xsl:if>

  
                <xsl:if test="@var">
                <tr>
                <th colspan="2">               
                    <p><xsl:element name="a">
                    <xsl:attribute name="name"><xsl:value-of select="@ID" /></xsl:attribute>
                    <xsl:attribute name="id"><xsl:value-of select="@ID" /></xsl:attribute>
                    <xsl:value-of select="labl"/>
                    </xsl:element>
                    </p>
                    
                  </th>
                  </tr>
                  <tr><td></td>
                  <td>  
                    <p>Variables within <xsl:value-of select="labl"/></p>
                    <ul>
                        <xsl:apply-templates mode="varGrp" select="../var">
                            <xsl:with-param name="var-token" select="@var"/>
                        </xsl:apply-templates>
                    </ul>
                    
                    </td></tr>
                    
                </xsl:if>

                    <xsl:apply-templates mode="row" select="txt"/>
                    <xsl:apply-templates select="concept"/>
                    <xsl:apply-templates select="defntn"/>
                    <xsl:apply-templates mode="row" select="universe"/>
                    <xsl:apply-templates mode="row" select="notes"/>

   <!-- What's this table for? Do tables normally appear in varGrp? -->
   
        <xsl:apply-templates select="table"/>

    </xsl:template>

    <xsl:template match="varGrp" mode="list-varGrp">
        <xsl:param name="varGrp-token"/>
        <xsl:if test="contains($varGrp-token,@ID)">
            <li><a href="{@ID}"><xsl:apply-templates mode="no-format" select="labl"/></a>
            </li>
        </xsl:if>
    </xsl:template>

    <xsl:template match="var" mode="varGrp">
        <xsl:param name="var-token"/>
        <xsl:choose>
            <xsl:when test="contains($var-token,@ID)">
                <li><a href="#{@name}">
                        <xsl:choose>
                            <xsl:when test="labl">
                                <xsl:apply-templates select="labl" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="@name"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </a>
                </li>
            </xsl:when>
            <xsl:when test="contains($var-token,@ID) and string-length(substring-after($var-token,@ID))=0">
                <li><a href="#{@name}">
                        <xsl:choose>
                            <xsl:when test="labl">
                                <xsl:apply-templates select="labl" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="@name"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </a>
                </li>
            </xsl:when>
            <xsl:otherwise/>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="labl" mode="no-format">
        <xsl:if test="@ID"><a name="{@ID}" /></xsl:if><xsl:apply-templates />
    </xsl:template>
    <xsl:template match="txt" mode="list">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>Text: <xsl:apply-templates />
        </li>
    </xsl:template>
    <xsl:template match="defntn">
		<tr>
			<td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
				<p>Definition:</p>
			</td>
			<td>
				<p><xsl:apply-templates /></p>
			</td>
        </tr>
    </xsl:template>

    <xsl:template match="concept">
		<tr>
			<td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
				<p>Concept:</p>
			</td>
			<td>
				<p><xsl:apply-templates /></p>
			</td>
        </tr>
    </xsl:template>

    <xsl:template match="txt" mode="row">
		<tr>
			<td class="h3"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
				<p>Text:</p>
			</td>
			<td>
				<p><xsl:apply-templates /></p>
			</td>
        </tr>
    </xsl:template>

    <xsl:template match="universe" mode="list">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>Universe: <xsl:apply-templates />
        </li>
    </xsl:template>
    <xsl:template match="var" mode="list">
        <li><a href="#{@name}"><xsl:value-of select="@name" /></a><xsl:if test="labl and labl!=''">&#160;-&#160;<xsl:apply-templates select="labl" /></xsl:if>

        </li>
    </xsl:template>
 
 
    <xsl:template match="var" mode="detail">
   <tr>
   <th colspan="2">
                   <p><a name="{@name}"></a><a id="{@ID}"></a>
                        <xsl:choose>
                            <xsl:when test="labl and labl!=''">
                                <xsl:apply-templates select="labl" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="@name"/>
                            </xsl:otherwise>
                        </xsl:choose>
                </p>
   </th>
   </tr>
   
   
   
   
        <tr>
            <td valign="top">
                <xsl:apply-templates select="location"/>
            </td>
            <td valign="top">
                <xsl:apply-templates select="qstn"/>
                <xsl:apply-templates mode="para" select="txt"/>
                <xsl:choose>
                    <xsl:when test="catgryGrp">
                        <table border="1" cellpadding="5" cellspacing="0" width="90%">
                            <xsl:apply-templates select="catgryGrp"/>
                        </table>
                    </xsl:when>
                    <xsl:when test="catgry">
                        <table border="1" cellpadding="5" cellspacing="0" width="90%">
                            <tr>
                                <th>
                                    <p>Value</p>
                                </th>
                                <th>
                                    <p>Label</p>
                                </th>
                                <th>
                                    <p>Frequency</p>
                                </th>
                                <th>
                                    <p>Text</p>
                                </th>
                            </tr>
                            <xsl:apply-templates mode="no-catgryGrp" select="catgry"/>
                        </table>
                    </xsl:when>
                </xsl:choose>
                <xsl:apply-templates select="catgry/catStat/table"/>
                <xsl:apply-templates select="imputation"/>
                <xsl:apply-templates select="security"/>
                <xsl:apply-templates select="embargo"/>
                <xsl:apply-templates select="respUnit"/>
                <xsl:apply-templates select="anlysUnit"/>
                <xsl:apply-templates select="valrng"/>
                <xsl:apply-templates select="invalrng"/>
                <xsl:apply-templates select="undocCod"/>
                <xsl:apply-templates mode="para" select="universe"/>
                <xsl:apply-templates select="TotlResp"/>

<xsl:if test="sumStat">
<p>Summary Statistics: <xsl:apply-templates select="sumStat"/></p>
</xsl:if>

                <xsl:apply-templates select="stdCatgry"/>
                <xsl:apply-templates select="codInstr"/>
                <xsl:apply-templates select="verStmt" mode="list2"/>
                <xsl:apply-templates select="concept"/>
                <xsl:apply-templates select="derivation"/>
                <xsl:apply-templates select="varFormat"/>
                <xsl:apply-templates select="geoMap"/>
                <xsl:apply-templates mode="para" select="notes"/>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="catgryGrp">
  
  <!-- ADD: catStat -->
  
        <tr>
            <th align="left" colspan="4">
                <p><strong>
                    <xsl:apply-templates mode="no-format" select="labl"/></strong>
                    <xsl:apply-templates mode="no-format" select="txt"/>
                </p>
            </th>
        </tr>
        <tr>
            <th>
                <p>Value</p>
            </th>
            <th>
                <p>Label</p>
            </th>
            <th>
                <p>Frequency</p>
            </th>
            <th>
                <p>Text</p>
            </th>
        </tr>
        <xsl:apply-templates mode="group-list" select="../catgry">
            <xsl:with-param name="catgryGrp-token" select="@catgry"/>
        </xsl:apply-templates>
    </xsl:template>
    <xsl:template match="txt" mode="no-format"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if> - <xsl:apply-templates />
    </xsl:template>
    <xsl:template match="catgry" mode="no-catgryGrp">
        <tr>
            <td>
            	<xsl:apply-templates select="catValu" />
            </td>
            <td>
                <p>
                    <xsl:apply-templates select="labl" />
                </p>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="catStat/table">
                        <p>see table below</p>
                    </xsl:when>
                    <xsl:otherwise>
                            <xsl:apply-templates select="catStat"/>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <td>
                <p>
                    <xsl:value-of select="txt"/>
                </p>
            </td>
        </tr>
    </xsl:template>
    
    <xsl:template match="catStat">
    	<p><xsl:apply-templates /></p>
    </xsl:template>
    
    <xsl:template match="catgry" mode="group-list">
        <xsl:param name="catgryGrp-token"/>
        <xsl:choose>
            <xsl:when test="contains($catgryGrp-token,concat(@ID,' '))">
                <tr>
                    <td>
	            	<xsl:apply-templates select="catValu" />
                    </td>
                    <td>
                        <p>
                                                            <xsl:apply-templates select="labl" />

                        </p>
                    </td>
                    <td>
                        <xsl:choose>
                            <xsl:when test="catStat/table">
                                <p>see table below</p>
                            </xsl:when>
                            <xsl:otherwise>
                                    <xsl:apply-templates select="catStat"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                    <td>
                        <p>
                            <xsl:value-of select="txt"/>
                        </p>
                    </td>
                </tr>
            </xsl:when>
            <xsl:when test="contains($catgryGrp-token,@ID) and string-length(substring-after($catgryGrp-token,@ID))=0">
                <tr>
                    <td>
	            	<xsl:apply-templates select="catValu" />
                    </td>
                    <td>
                        <p>
                                                            <xsl:apply-templates select="labl" />

                        </p>
                    </td>
                    <td>
                        <xsl:choose>
                            <xsl:when test="catStat/table">
                                <p>see table below</p>
                            </xsl:when>
                            <xsl:otherwise>
                                <p>
                                    <xsl:apply-templates select="catStat"/>
                                </p>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                    <td>
                        <p>
                            <xsl:value-of select="txt"/>
                        </p>
                    </td>
                </tr>
            </xsl:when>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="catValu">
    
    <p><xsl:apply-templates /><xsl:if test="substring(catValu,string-length(catValu)-1,1)!='.'">.</xsl:if></p>
    
    
    </xsl:template>

    <xsl:template match="location"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p class="small">
            <xsl:if test="@fileid">
                <xsl:value-of select="@fileid"/> </xsl:if>Location:</p>
        <p class="small">
            <xsl:if test="@StartPos">
                Start: <xsl:value-of select="@StartPos"/>
                <br />
            </xsl:if>
            <xsl:if test="@EndPos">
                End: <xsl:value-of select="@EndPos"/>
                <br />
            </xsl:if>
            <xsl:if test="@width">
                Width: <xsl:value-of select="@width"/>
                <br />
            </xsl:if>
            <xsl:if test="@RecSegNo">
                Record Segment No. <xsl:value-of select="@RecSegNo"/>
                <br />
            </xsl:if>
        </p>
    </xsl:template>
    <xsl:template match="imputation"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Imputation: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="security"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Security: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="embargo"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Embargo: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="respUnit"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Response Unit: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="anlysUnit"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Analysis Unit: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="qstn"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Question: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="preQTxt">
        <xsl:apply-templates /> </xsl:template>
    <xsl:template match="qstnLit">
        <xsl:apply-templates /> </xsl:template>
    <xsl:template match="postQTxt">
        <xsl:apply-templates /> </xsl:template>
    <xsl:template match="forward">
        <xsl:apply-templates /> </xsl:template>
    <xsl:template match="backward">
        <xsl:apply-templates /> </xsl:template>
    <xsl:template match="ivuInstr">
        <xsl:apply-templates /> </xsl:template>
    <xsl:template match="valrng"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Range of Valid Data Values: <xsl:apply-templates select="range"/>
            <xsl:apply-templates select="item"/>
            <xsl:apply-templates select="key"/>
            <xsl:apply-templates mode="no-format" select="notes"/>
        </p>
    </xsl:template>
    <xsl:template match="invalrng"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Range of Invalid Data Values: <xsl:apply-templates select="range"/>
            <xsl:apply-templates select="item"/>
            <xsl:apply-templates select="key"/>
            <xsl:apply-templates mode="no-format" select="notes"/>
        </p>
    </xsl:template>
    <xsl:template match="range">
        <xsl:value-of select="@min"/>-<xsl:value-of select="@max"/>
    </xsl:template>

    <xsl:template match="key">
        <xsl:apply-templates />
    </xsl:template>
 
     <xsl:template match="item">
        <xsl:value-of select="@VALUE"/>
    </xsl:template>

    <xsl:template match="undocCod"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>List of Undocumented Codes: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="universe" mode="para"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Universe: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="TotlResp"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Total Responses: <xsl:apply-templates />
        </p>
    </xsl:template>
  
  
<xsl:template match="sumStat"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
 
 <xsl:choose>
 <xsl:when test="@type='vald'">Valid <xsl:apply-templates /><xsl:if test="position()!=last()">; </xsl:if></xsl:when>
 <xsl:when test="@type='min'">Min. <xsl:apply-templates /><xsl:if test="position()!=last()">; </xsl:if></xsl:when>
 <xsl:when test="@type='max'">Max. <xsl:apply-templates /><xsl:if test="position()!=last()">; </xsl:if></xsl:when>
 <xsl:when test="@type='mean'">Mean <xsl:apply-templates /><xsl:if test="position()!=last()">; </xsl:if></xsl:when>
 <xsl:when test="@type='stdev'">StDev <xsl:apply-templates /><xsl:if test="position()!=last()">; </xsl:if></xsl:when>
 </xsl:choose> 
 
</xsl:template>
  
  
  
  
    <xsl:template match="txt" mode="para"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Variable Text: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="stdCatgry"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Standard Categories: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="codInstr"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Coder Instructions: <xsl:apply-templates />
        </p>
    </xsl:template>

    <xsl:template match="verStmt" mode="list2">
        <ul>
            <xsl:apply-templates mode="list" select="version"/>
            <xsl:apply-templates mode="list" select="verResp"/>
            <xsl:apply-templates mode="list" select="notes"/>
        </ul>
    </xsl:template>

    <xsl:template match="concept"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Concept: <xsl:apply-templates />
        </p>
    </xsl:template>
    <xsl:template match="derivation"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Derivation</p>
        <ul>
            <xsl:apply-templates select="drvdesc"/>
            <xsl:apply-templates select="drvcmd"/>
        </ul>
    </xsl:template>
    <xsl:template match="drvdesc">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>Description: <xsl:value-of select="(.)"/>
        </li>
    </xsl:template>
    <xsl:template match="drvcmd">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>Command: <xsl:value-of select="(.)"/>
        </li>
    </xsl:template>

    <xsl:template match="geoMap"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Geographic Map: <xsl:value-of select="@URI"/>
        </p>
    </xsl:template>


    <xsl:template match="varFormat"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <p>Variable Format: <xsl:value-of select="@type"/>
        </p>
    </xsl:template>

<!-- end dataDscr templates -->

<!-- begin otherMat templates -->


    <xsl:template match="otherMat">
        <tr>
            <th align="left" colspan="2">
                <p><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                    <a name="5.0">Other Study-Related Materials</a>
                </p>
            </th>
        </tr>
        <xsl:apply-templates mode="row" select="labl"/>
        <xsl:apply-templates mode="para2" select="txt"/>
        <xsl:apply-templates mode="row" select="notes"/>
        <xsl:apply-templates select="table"/>
        <xsl:apply-templates select="citation"/>
        <xsl:apply-templates select="otherMat"/>
    </xsl:template>
    <xsl:template match="labl" mode="row">
        <tr>
            <td align="right" valign="top"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Label:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
 <xsl:template match="labl"><xsl:apply-templates /></xsl:template>
 
 <xsl:template match="txt" mode="para2">
        <tr>
            <td align="right" valign="top"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <p>Text:</p>
            </td>
            <td>
                <p>
                    <xsl:apply-templates />
                </p>
            </td>
        </tr>
    </xsl:template>
    <xsl:template match="table"><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
        <table border="1" cellpadding="5" cellspacing="0" width="90%">
            <xsl:if test="titl">
                <caption>
                    <xsl:value-of select="titl"/>
                </caption>
            </xsl:if>
            <xsl:apply-templates select="tgroup"/>
        </table>
    </xsl:template>
    <xsl:template match="tgroup">
        <xsl:apply-templates select="thead"/>
        <xsl:apply-templates select="tbody"/>
    </xsl:template>
    <xsl:template match="thead">
        <xsl:apply-templates mode="thead" select="row"/>
    </xsl:template>
    <xsl:template match="row" mode="thead">
        <tr>
            <xsl:apply-templates mode="thead" select="entry"/>
        </tr>
    </xsl:template>
    <xsl:template match="entry" mode="thead">
        <th>
            <p>
                <xsl:apply-templates />
            </p>
        </th>
    </xsl:template>
    <xsl:template match="tbody">
        <xsl:apply-templates mode="tbody" select="row"/>
    </xsl:template>
    <xsl:template match="row" mode="tbody">
        <tr>
            <xsl:apply-templates mode="tbody" select="entry"/>
        </tr>
    </xsl:template>
    <xsl:template match="entry" mode="tbody">
        <td>
            <p>
                <xsl:apply-templates />
            </p>
        </td>
    </xsl:template>

<!-- end otherMat templates -->

   <xsl:template match="notes" mode="row">
        <tr>
            <td class="h3">
                <xsl:if test="position()=1"><p>Notes:</p></xsl:if>
            </td>
            <td>
                <p><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
                <xsl:apply-templates /></p>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="notes" mode="list">
        <li><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
Notes: <xsl:apply-templates /></li>
    </xsl:template>

    <xsl:template match="notes" mode="para">
        <p><xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
Notes: <xsl:apply-templates /></p>
    </xsl:template>

    <xsl:template match="notes" mode="no-format">
		<xsl:if test="@ID"><a name="{@ID}" /></xsl:if>
		<xsl:apply-templates />
    </xsl:template>

<xsl:template match="Link">

&#160;(<a href="#{@refs}">link</a>)

</xsl:template>

<xsl:template match="ExtLink">

(<a href="{@URI}">external link</a>)

</xsl:template>

<xsl:template match="p">
	<p><xsl:apply-templates /></p>
</xsl:template>

<xsl:template match="emph">
	<em><xsl:apply-templates /></em>
</xsl:template>

<!-- ADD: div head hi item list -->

</xsl:stylesheet>
