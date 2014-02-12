Appendix
++++++++

Additional documentation complementary to Users Guides.

Control Card-Based Data Ingest
===============================

As of version 2.2 the DVN supports ingesting plain text data files, in
addition to SPSS and STATA formats. This allows users and institutions
to ingest raw data into Dataverse Networks without having to purchase
and maintain proprietary, commercial software packages.

Tab-delimited and CSV files are supported. In order to ingest a plain
data file, an additional file containing the variable metadata needs to
be supplied.

**Two Metadata Types Are Supported**

#. A simplified format based on the classic SPSS control card syntax;
   this appears as "CSV/SPSS" in the menu on the Add Files page.
#. DDI, an xml format from the Data Documentation Inititative
   consortium. Choose "TAB/DDI" to ingest a tab file with a DDI metadata sheet.

The specifics of the formats are documented in the 2 sections below.

CSV Data, SPSS-style Control Card
----------------------------------

Unlike other supported “subsettable” formats, this ingest mechanism
requires 2 files: the CSV raw data file proper and an SPSS Setup file
("control card") with the data set metadata. In the future, support for
other data definition formats may be added (STATA, SAS, etc.). As
always, user feedback is welcome.

**The supported SPSS command syntax:**

Please note that it is not our goal to attempt to support any set of
arbitrary SPSS commands and/or syntax variations. The goal is to enable
users who do not own proprietary statistical software to prepare their
raw data for DVN ingest, using a select subset of SPSS data definitional
syntax.

(In addition to its simplicity and popularity, we chose to use the SPSS
command syntax because Dataverse Network already has support for the SPSS ``.SAV`` and ``.POR`` formats, so we have a good working knowledge of the SPSS formatting
conventions.)

The following SPSS commands are supported:

| ``DATA LIST ``
| ``VARIABLE LABELS ``
| ``NUMBER OF CASES``
| ``VALUE LABELS``
| ``FORMATS`` (actually, not supported as of now -- see below)
| ``MISSING VALUES``

We support mixed cases and all the abbreviations of the above commands
that are valid under SPSS. For example, both "var labels" and "Var Lab"
are acceptable commands.

Individual command syntax.

**1. DATA LIST**

An explicit delimiter definition is required. For example:

``DATA LIST LIST(',')``

specifies ``','`` as the delimiter. This line is followed by the ``'/'``
separator and variable definitions. Explicit type definitions are
required. Each variable is defined by a name/value pair ``VARNAME``

``(VARTYPE)`` where ``VARTYPE`` is a standard SPSS fortran-type
definition.

**Note** that this is the only **required** section. The minimum
amount of metadata required to ingest a raw data file is the delimiter
character, the names of the variables and their data type. All of these
are defined in the ``DATA LIST`` section. Here’s an example of a
complete, valid control card:

``DATA LIST LIST(’,’)``
``CASEID (f) NAME (A) RATIO (f)``
``.``

It defines a comma-separated file with 3 variables named ``CASEID``,
``NAME`` and ``RATIO``, two of them of the types numeric and one character
string.

Examples of valid type definitions:

| **A8** 8 byte character string;
| **A** character string;
| **f10.2** numeric value, 10 decimal digits, with 2 fractional digits;
| **f8** defaults to F8.0
| **F** defaults to F.0, i.e., numeric integer value
| **2** defaults to F.2, i.e., numeric float value with 2 fractional digits.

The following SPSS date/time types are supported:

type                            format

``DATE``                       ``yyyy-MM-dd``

``DATETIME``                ``yyyy-MM-dd HH:mm:ss``

The variable definition pairs may be separated by any combination of
white space characters and newlines. **Wrapped-around lines must start
with white spaces** (i.e., newlines must be followed by spaces). The
list must be terminated by a line containing a single dot.

Please note, that the actual date values should be stored in the CSV
file as strings, in the format above. As opposed to how SPSS stores the
types of the same name (as integer numbers of seconds).

**2. VARIABLE LABELS**

Simple name/value pairs, separated by any combination of white space
characters and newlines (as described in section 1 above). The list is
terminated by a single dot.

For example:

| ``VARIABLE LABELS``
| ``CELLS "Subgroups for sample-see documentation"``
| ``STRATA "Cell aggregates for sample”``
| ``.``

**3. NUMBER OF CASES (optional)**

The number of cases may be explicitly specified. For example:

``num of cases 1000``

When the number of cases is specified, it will be checked against the
number of observations actually found in the CSV file, and a mismatch
would result in an ingest error.

**4. VALUE LABELS**

Each value label section is a variable name followed by a list of
value/label pairs, terminated by a single "/" character. The list of
value label sections is terminated by a single dot.

For example,

| ``VALUE labels``
| ``FOO 0 "NADA"``
| ``1 "NOT MUCH"``
| ``99999999 "A LOT"``
| ``/``
| ``BAR 97 "REFUSAL"``
| ``98 "DONT KNOW"``
| ``99 "MISSING"``
| ``/``
| ``.``

**5. FORMATS**

This command is actually redundant if you explicitly supply the variable
formats in the ``DATA LIST`` section above.

**NOTE:** It appears that the only reason the``FORMATS`` command exists is
that ``DATA LIST`` syntax does not support explicit fortran-style format
definitions when fixed-field data is defined. So it is in fact redundant
when we're dealing with delimited files only.

Please supply valid, fortran-style variable formats in the ``DATA
LIST`` section, as described above.

**6. MISSING VALUES**

This is a space/newline-separate list of variable names followed by a
comma-separated list of missing values definition, in parentheses. For
example: 

| ``INTVU4 (97, 98, 99)``
| The list is terminated with a single dot.

An example of a valid ``MISSING VALUES`` control card section:

| ``MISSING VALUES``
| ``INTVU4 (97, 98, 99)``
| ``INTVU4A ('97', '98', '99')``
| ``.``

| **An example of a control card ready for ingest:**

.. code-block:: guess

	data list list(',') /
	  CELLS (2)  STRATA (2)  WT2517 (2)
	  SCRNRID (f) CASEID (f)  INTVU1 (f)
	  INTVU2 (f)  INTVU3 (f)  INTVU4 (f)
	  INTVU4A (A)
	  .
	VARIABLE LABELS
	  CELLS "Subgroups for sample-see documentation"
	  STRATA "Cell aggregates for sample-see documenta"
	  WT2517 "weight for rep. sample-see documentation"
	  SCRNRID "SCREENER-ID"
	  CASEID "RESPONDENT'S CASE ID NUMBER"
	  INTVU1 "MONTH RESPONDENT BEGAN INTERVIEW"
	  INTVU2 "DAY RESPONDENT BEGAN INTERVIEW"
	  INTVU3 "HOUR RESPONDENT BEGAN INTERVIEW"
	  INTVU4 "MINUTE RESPONDENT BEGAN INTERVIEW"
	  INTVU4A "RESPONDENT INTERVIEW BEGAN AM OR PM"
	  .
	VALUE labels
	  CASEID   99999997 "REFUSAL"
					  99999998 "DONT KNOW"
					  99999999 "MISSING"
					  /
	  INTVU1   97 "REFUSAL"
					  98 "DONT KNOW"
					  99 "MISSING"
					  /
	  INTVU2   97 "REFUSAL"
					  98 "DONT KNOW"
					  99 "MISSING"
					  /
	  INTVU3   97 "REFUSAL"
					  98 "DONT KNOW"
					  99 "MISSING"
					  /
	  INTVU4   97 "REFUSAL"
					  98 "DONT KNOW"
					  99 "MISSING"
					  /
	  INTVU4A "97" "REFUSAL"
					  "98" "DONT KNOW"
					  "99" "MISSING"
					  "AM" "MORNING"
					  "PM" "EVENING"
	  .
	MISSING VALUES
	  CASEID (99999997, 99999998, 99999999)
	  INTVU1 (97, 98, 99)
	  INTVU2 (97, 98, 99)
	  INTVU3 (97, 98, 99)
	  INTVU4 (97, 98, 99)
	  INTVU4A ('97', '98', '99')
	  .
	NUMBER of CASES 2517

**DATA FILE.**

Data must be stored in a text file, one observation per line. Both DOS
and Unix new line characters are supported as line separators. On each
line, individual values must be separated by the delimiter character
defined in the DATA LISTsection. There may only be exactly (``NUMBER OF
VARIABLES - 1``) delimiter characters per line; i.e. character values must
not contain the delimiter character.

**QUESTIONS, TODOS:**

Is there any reason we may want to support ``RECODE`` command also?

--- comments, suggestions are welcome! ---


Tab Data, with DDI Metadata
----------------------------

As of version 2.2, another method of ingesting raw TAB-delimited data
files has been added to the Dataverse Network. Similarly to the SPSS control
card-based ingest (also added in this release), this ingest mechanism
requires 2 files: the TAB raw data file itself and the data set metadata
in the DDI/XML format.

**Intended use case:**

Similarly to the SPSS syntax-based ingest, the goal is to provide
another method of ingesting raw quantitative data into the DVN, without
having to first convert it into one of the proprietary, commercial
formats, such as SPSS or STATA. Pleaes note, that in our design
scenario, the DDI files supplying the ingest metadata will be somehow
machine-generated; by some software tool, script, etc. In other words,
this design method is targeted towards more of an institutional user,
perhaps another data archive with large quantities of data and some
institutional knowledge of its structure, and with some resources to
invest into developing an automated tool to generate the metadata
describing the datasets. With the final goal of ingesting all the data
into a DVN by another automated, batch process. The DVN project is also
considering developing a standalone tool of our own that would guide
users through the process of gathering the information describing their
data sets and producing properly formatted DDIs ready to be ingested.

For now, if you are merely looking for a way to ingest a single
“subsettable” data set, you should definitely be able to create a
working DDI by hand to achieve this goal. However, we strongly recommend
that you instead consider the CSV/SPSS control card method, which was
designed with this use case in mind. If anything, it will take
considerably fewer keystrokes to create an SPSS-syntax control card than
a DDI encoding the same amount of information.

**The supported DDI syntax:**

You can consult the DDI project for complete information on the DDI
metadata (`http://icpsr.umich.edu/DDI <http://icpsr.umich.edu/DDI>`__).
However, only a small subset of the published format syntax is used for
ingesting individual data sets. Of the 7 main DDI sections, only 2,
fileDscr and dataDscr are used. Inside these sections, only a select set
of fields, those that have direct equivalents in the DVN data set
structure, are supported.

These fields are outlined below. All the fields are mandatory, unless
specified otherwise. An XSD schema of the format subset is also
provided, for automated validation of machine-generated XML.

.. code-block:: guess

		<?xml version="1.0" encoding="UTF-8"?>
		<codeBook xmlns="http://www.icpsr.umich.edu/DDI"\>
		<fileDscr>
			<fileTxt ID="file1">
					<dimensns>
							<caseQnty>NUMBER OF OBSERVATIONS</caseQnty>
							<varQnty>NUMBER OF VARIABLES</varQnty>
					</dimensns>
			</fileTxt>
		</fileDscr>
		<dataDscr>
			<!-- var section for a discrete numeric variable: -->
			<var ID="v1.1" name="VARIABLE NAME" intrvl="discrete" >
					<location fileid="file1"/>
					<labl level="variable">VARIABLE LABEL</labl>
					<catgry>
							<catValu>CATEGORY VALUE</catValu>
					</catgry>
				…
				<!-- 1 or more category sections are allowed for discrete variables -->
					<varFormat type="numeric" />
			</var>
		   <!-- var section for a continuous numeric variable: -->
			<var ID="v1.2" name="VARIABLE NAME" intrvl="contin" >
					<location fileid="file1"/>
					<labl level="variable">VARIABLE LABEL</labl>
					<varFormat type="numeric" />
			</var>
		   <!-- var section for a character (string) variable: -->
			<var ID="v1.10" name="VARIABLE NAME" intrvl="discrete" >
					<location fileid="file1"/>
					<labl level="variable">VARIABLE LABEL</labl>
					<varFormat type="character" />
			</var>
			<!-- a discrete variable with missing values defined: -->
		</dataDscr>
		</codeBook>


--- comments, suggestions are welcome! ---

.. _metadata-references:

Metadata References
====================

The Dataverse Network metadata is compliant with the `DDI schema
version 2 <http://www.icpsr.umich.edu/DDI/>`__. The Metadata fields associated with each Dataset contain most of the fields
in the study description section of the DDI. That way the Dataverse
Network metadata can be mapped easily to a DDI, and be exported into XML
format for preservation and interoperability.

Dataverse Network data also is compliant with `Simple Dublin
Core <http://www.dublincore.org/>`__ (DC) requirements. For imports
only, Dataverse Network data is compliant with the `Content Standard
for Digital Geospatial Metadata (CSDGM), Vers. 2 (FGDC-STD-001-1998) <http://www.fgdc.gov/metadata>`__ (FGDC).

Attached is a [need to include this] that defines and maps all Dataverse Network
Cataloging Information fields. Information provided in the file includes
the following:

- Field label - For each Cataloging Information field, the field label
  appears first in the mapping matrix.

- Description - A description of each field follows the field label.

- Query term - If a field is available for use in building a query, the
  term to use for that field is listed.

- Dataverse Network database element name - The Dataverse Network
  database element name for the field is provided.

- Advanced search - If a field is available for use in an advanced
  search, that is indicated.

- DDI element mapping for imports - For harvested or imported studies,
  the imported DDI elements are mapped to Dataverse Network fields.

- DDI element mapping for exports - When a study or dataverse is
  harvested or exported in DDI format, the Dataverse Network fields are
  mapped to DDI elements.

- DC element mapping for imports - For harvested or imported studies,
  the imported DC elements are mapped to specific Dataverse Network
  fields.

- DC element mapping for exports - When a study or dataverse is
  harvested or exported in DC format, specific Dataverse Network fields
  are mapped to the DC elements.

- FGDC element mapping for imports - For harvested or imported studies,
  the imported FGDC elements are mapped to specific Dataverse Network fields.

Also attached is an example of a DDI for a simple study containing
title, author, description, keyword, and topic classification cataloging
information fields suitable for use with batch import.

[add link to metadata table for 4.0]

|image10|
`simple\_study.xml <http://guides.thedata.org/files/thedatanew_guides/files/simple_study_1.xml>`__

Zelig Interface
===============

Zelig is statistical software for everyone: researchers, instructors,
and students. It is a front-end and back-end for R (Zelig is written in
R). The Zellig software:

- Unifies diverse theories of inference

- Unifies different statistical models and notation

- Unifies R packages in a common syntax

Zelig is distributed under the GNU General Public License, Version 2.
After installation, the source code is located in your R library
directory. You can download a tarball of the latest Zelig source code
from \ `http://projects.iq.harvard.edu/zelig <http://projects.iq.harvard.edu/zelig>`__.

The Dataverse Network software uses Zelig to perform advanced
statistical analysis functions. The current interface schema used by the
Dataverse Network for Zelig processes is in the following location:

**Criteria for Model Availability**

Three factors determine which Zelig models are available for analysis in
the Dataverse Network: 

- Some new models require data structures and modeling parameters that
  are not compatible with the current framework of the Dataverse Network
  and other web-driven applications. These types of models are not
  available in the Dataverse Network.

- Models must be explicitly listed in the Zelig packages to be used in
  the Dataverse Network, and all models must be disclosed fully, including
  runtime errors. Zelig models that do not meet these specifications are
  excluded from the Dataverse Network until they are disclosed with a
  complete set of information.

- An installation-based factor also can limit the Zelig models available
  in the Dataverse Network. A minimum version of the core software package
  GCC 4.0 must be installed on any Linux OS-based R machine used with the
  Dataverse Network, to install and run a key Zelig package, MCMCpack. If
  a Linux machine that is designated to R is used for DSB services and
  does not have the minimum version of the GCC package installed, the
  Dataverse Network looses at least eight models from the available
  advanced analysis models.

|image11|
`configzeliggui.xml <http://guides.thedata.org/files/thedatanew_guides/files/configzeliggui_0.xml>`__

.. |image9| image:: ./appendix-0_files/application-pdf.png
.. |image10| image:: ./appendix-0_files/application-octet-stream.png
.. |image11| image:: ./appendix-0_files/application-octet-stream.png

