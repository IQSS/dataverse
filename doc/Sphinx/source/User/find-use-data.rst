Finding and Using Data
++++++++++++++++++

Ends users, without need to login to the Dataverse Network, can browse
dataverses, search studies, view study description and data files for
public studies, and subset, analyze and visualize data for public data
files. If entire studies or individual data files are restricted, end
users need to be given permission from the dataverse administrator to
access the data.


Search
=======

To find a study or data set, you can search or browse studies offered
in any released dataverse on the Network homepage. Each dataverse offers
a hierarchical organization comprising one or more collections of data
sets with a particular theme. Most dataverses allow you to search for
data within their files, or you can start browsing through the dataverse
classifications that are closest to your substantive interests.

**Browse Collections**

You can browse all public dataverses from the Network homepage. Click
the title of a dataverse to browse that dataverse's collections and
studies. Click the title of a collection to view a list of studies and
subcollections for that selection. Click the title of a study to view
the Cataloging Information and study files for that selection.

When you select a dataverse to view its contents, the homepage opens to
the \ *root collection*, and the dataverse's studies are displayed
directly under the root collection name. If the root collection contains
other collections, then those collections are listed and not the studies
within them. You must select a collection title to view the studies
contained within it.

Note: If a dataverse includes links to collections from another
dataverse and the root collection does not contain other collections,
the homepage opens to a list of the root and linked collections.

**Search - Basic**

You can search for studies across the entire Dataverse Network from the
Network homepage, or search within a dataverse from the dataverse
homepage. When you search across the Network, studies from restricted
dataverses are not included in the search. Restricted studies are
included in search results, and a lock icon appears beside those studies
in the results list. After your search is complete, you can further
narrow your list of data by searching again in the results. See Search
Tips for search examples and guidelines.

When you enter more than one term in the search text field, the results
list contains studies that have these terms near each other within the
study fields searched. For example, if you enter ``United Nations``,
the results include studies where the words *United* and *Nations* are
separated by no more than four words in the same study field, such as
abstract or title.

It supports a search in any field of the studies' Cataloging
Information, which includes citation information, abstract and other
scope-related information, methodology, and Terms of Use. In addition,
file descriptions also are searched.

**Search - Advanced**

In an advanced search, you can refine your criteria by choosing which
Cataloging Information fields to search. You also can apply logic to the
field search. For text fields, you can specify that the field searched
either *contains* or *does not contain\ the text that you enter. For
date fields, you can specify that the field searched is either *later
than* nor *earlier than* the date that you enter. Refer to
the `Documentation <http://lucene.apache.org/java/docs/>`__  page for
the latest version at the Lucene website and look for *Query Syntax* for full details.

To perform an advanced search, click the Advanced Search link at the
top-right of the Search panel. You can search the following study
metadata fields by using the Search Scope drop-down list:

-  Title - Title field of studies' Cataloging Information.
-  Author - Author fields of studies' Cataloging Information.
-  (Study) Global ID - ID assigned to studies.
-  Other ID - A different ID previously given to the study by another
   archive.
-  Abstract - Any words in the abstract of the study.
-  Keyword - A term that defines the nature or scope of a study. For
   example, ``elections``.
-  Keyword Vocabulary - Reference to the standard used to define the
   keywords.
-  Topic Classification - One or more words that help to categorize the
   study.
-  Topic Classification Vocabulary - Reference used to define the Topic
   Classifications.
-  Producer - Institution, group, or person who produced the study.
-  Distributor - Institution that is responsible for distributing the
   study.
-  Funding Agency - Agency that funded the study.
-  Production Date - Date on which the study was created or completed.
-  Distribution Date - Date on which the study was distributed to the
   public.
-  Date of Deposit - Date on which the study was uploaded to the
   Network.
-  Time Period Cover Start - The beginning of the period covered by the
   study.
-  Time Period Cover End - The end of the period covered by the study.
-  Country/Nation - The country or countries where the study took place.
-  Geographic Coverage - The geographical area covered by the study. For
   example, ``North America``.
-  Geographic Unit - The smallest geographic unit in which the study
   took place, such as ``state``.
-  Universe - Universe of interest, population of interest, or target
   population.
-  Kind of Data - The type of data included in the file, such
   as ``survey data``, ``census/enumeration data``,
   or ``aggregate data``.
-  Variable Information - The variable name and description in the
   studies' data files, given that the data file is subsettable and
   contains tabular data. It returns the studies that contain the file
   and the variable name where the search term was found.

**Sort Results**

When your search is complete, the results page lists studies that met
the search criteria in order of relevance. For example, a study that
includes your search term within the Cataloging Information in ten
places appears before a study that includes your search term in the
Cataloging Information in only one place.

You can sort search results by title, study ID, last updated, or number
of downloads (that is, the number of times users downloaded any file
belonging to that study). Click the Sort By drop-down list to choose
your sort order.

**Search Tips**

Use the following guidelines to search effectively within a Network or a
dataverse:

-  The default search syntax uses ``AND`` logic within individual
   fields. That is, if you enter more than one term, the search engine
   looks for all terms within a single field, such as title or abstract.
   For example, if you enter ``United Nations report``, the results
   list any studies that include the terms *United*, *Nations*,
   and *report* within a single metadata field.
-  The search logic looks for multiple terms within a specific proximity
   to one another, and in the same field. The current proximity criteria
   is four words. That is, if you enter two search terms, both terms
   must be within four words of each other in the same field to be
   returned as a result.
   For example, you might enter ``10 year`` in a basic search. If a
   study includes the string *10 millions deaths per year* within a
   metadata field, such as abstract, that study is not included in the
   search results. A study that contains the string *10 per year* within the abstract field is included in the search results.
-  During the index process that supports searches, periods are removed
   in strings and each term between periods is indexed individually. If
   you perform a basic search for a term that contains one or more
   periods, the search works because the analyzer applies
   the *AND* logic. If you search on a specific field, though, note
   that you should specify individually each component of the string
   between periods to return your results.
-  You can enter one term in the search field, and then search within
   those results for another term to narrow the results further. This
   might be more effective than searching for both terms at one time, if
   those terms do not meet the proximity and field limits specified
   previously.
   You could search first for an author's name, and then search those
   results for a specific term in the title. If you try searching for
   both terms in the author and title fields together, you might not
   find the study for which you are looking.
   For example, you can search the Harvard Dataverse Network for the
   following study:

       *Gary King; Will Lowe, 2003, "10 Million International Dyadic
       Events", hdl:1902.1/FYXLAWZRIA UNF:3:um06qkr/1tAwpS4roUqAiw==
       Murray Research Archive [Distributor]*

   If you type ``King, 10 Million`` in the Search field and click
   Search, you see ``0 matches were found`` in the Results field. If
   you type ``10`` in the Search field and click Search, you see
   something like ``1621 matches were found`` in the Results field.
   But if you first type ``King`` in the Search field and click
   Search, then type ``10 Million`` in the Search field and click
   Search again, you see something like ``4 matches were found`` in the
   Results field.


View Studies / Download Data
========================

**Cataloging Information**

When a study is created, a set of *metadata* is associated with that
study. This metadata is called the *Cataloging Information* for the
study. When you select a study to view it, you first see the Cataloging
Information tab listing the metadata associated with that study. This is
the default view of a study.

Cataloging Information contains numerous fields that help to describe
the study. The amount of information you find for each study varies,
based on what was entered by the author (Contributor) or Curator of that
study. For example, one study might display the distributor, related
material, and geographic coverage. Another study might display only the
authors and the abstract. Every study includes the *Citation Information* fields in the Cataloging Information.

Note: A comprehensive list of all Cataloging Information fields is
provided in the :ref:`List of Metadata References <metadata-references>`

Cataloging Information is divided into four sections. These sections and
their details are displayed only when the author (Contributor) or
Curator provides the information when creating the study. Sections
consist of the following:

-  Citation Information - These fields comprise
   the `citation <http://thedata.org/citation>`__ for the study,
   consisting of a global identifier for all studies and a UNF, or
   Universal Numerical Fingerprint, for studies that contain subsettable
   data files. It also can include information about authors, producers
   and distributors, and references to related studies or papers.
-  Abstract and Scope - This section describes the research study, lists
   the study's data sets, and defines the study's geographical scope.
-  Data Collection/Methodology - This section includes the technical
   details of how the author obtained the data.
-  Terms of Use - This information explains that the study requires
   users to accept a set of conditions or agreements before downloading
   or analyzing the data. If any *Terms of Use* text is displayed in
   the Cataloging Information section, you are prompted to accept the
   conditions when you click the download or analyze icons in the Files
   page.
   Note: A study might not contain Terms of Use, but in some cases the
   original parent dataverse might have set conditions for all studies
   owned by that dataverse. In that case, the conditions are inherited
   by the study and you must accept these conditions before downloading
   files or analyzing the data.

**List of Study Files**

When you view a study, click the Documentation, Data and Analysis tab to
view a list of all electronic files associated with the study that were
provided by the author or Curator.

A study might contain documentation, data, or other files. When the
study contributor uploads data files of the type ``.dta``, ``.sav``, or ``.por`` to the Network, those files are converted
to ``.tab`` tab-delimited files. These ``.tab`` files
are subsettable, and can be subsetted and analyzed online by using the Dataverse Network
application.

Data files of the type ``.xml`` also are considered to be subsettable,
and can be subsetted and analyzed to a minimal degree online.
An ``.xml`` type file indicates social network data that complies with
the `GraphML <http://graphml.graphdrawing.org/>`__ file format.

You can identify a subsettable data file by the *Subsetting* label and
the number of cases and variables listed next to the file name. Other
files that also contain data might be associated with a study, but the
Dataverse Network application does not recognize them as data (or
subsettable) files.

**Download Study Files**

You can download any of the following within a study:

-  All or selected data files within a *study* or a *category* (type
   of files)
-  Individual *data files*
-  Individual subsets within a data file (see :ref:`Subset and Analyze
   Tabular Data Sets <tabular-data>`
   or :ref:`Subset and Analyze Network Data Sets <network-data>` for details)

The default format for subsettable tabular data file downloads
is *tab-delimited*. When you download one or more subsettable files in
tab-delimited format, the file contains a header row. When you download
one subsettable file, you can select from the following formats in
addition to tab-delimited:

-  Original file
-  Splus
-  Stata
-  R

The default format for subsettable network data file downloads
is *Original file*. In addition, you can choose to download network
data files in *GraphML* format.

If you select any other format for a tabular data file, the file is
downloaded in a zipped archive. You must unzip the archive to view or
use the individual data file.

If you download all or a selection of data files within a study, the
files are downloaded in a zipped archive, and the individual files are
in tab-delimited or network format. You must unzip the archive to view
or use the individual data files.

Note: Studies and data files often have user restrictions applied. If
prompted to accept Terms of Use for a study or file, check the *I Accept* box and then click the Continue button to view or download the
file.

**User Comments**

If the User Comment feature is enabled within a dataverse, users are
able to add comments about a study within that dataverse.

When you view a study, click the User Comments tab to view all comments
associated with the study. Comments can be monitored and abuse reported
to the Network admin, who has permission to remove any comments deemed
inappropriate. Note that the dataverse admin does not have permission to
remove comments, to prevent bias.

If you choose, you also can add your own comments to a study from the
User Comments tab. See :ref:`Comment on Studies or Data <edit-study-comments-settings>` for
detailed information.

Note: To add a comment to a study, you must register and create an
account in the dataverse that owns the study about which you choose to
comment. This helps to prevent abuse and SPAM issues.

**Versions**

Upon creating a study, a version is created. This is a way to archive
the *metadata* and *data files* associated with the study citation
or UNF.

**View Citations**

You can view a formatted citation for any of the following entities
within the Dataverse Network application:

-  Studies - For every study, you can view a citation for that study.
   Go to the Cataloging Information tab for a study and view the *How
   to Cite* field.
-  Data sets - For any data set, you can view a citation for that set.
   Go to the Documentation, Data and Analysis tab for a study to see the
   list of study files. To view the citation for any data set click
   the *View Data Citation* link associated with that subsettable
   file.
-  Data subsets - If you subset and analyze a data set, you can view a
   citation for each subset. 
   See :ref:`Apply Descriptive Statistics <apply-descriptive-statistics>` or :ref:`Perform Advanced Analysis <perform-advanced-analysis>` for
   detailed information.
   Also, when you download a workspace file, a copy
   of the citation information for that subset is provided in the
   download.

Note: For individual variables within a subsettable data subset, you can
view the `UNF <http://thedata.org/citation/tech>`__ for that variable.
This is not a full citation for the variable, but it is one component of
that citation. Note also that this does not apply to ``.xml`` data.

Subset and Analysis
==============

Subsetting and analysis can be performed on tabular and network data
files. Refer to the appropriate section for more details.

.. _tabular-data:

Tabular Data
--------------

Tabular data files (subsettable files) can be subsetted and analyzed
online by using the Dataverse Network application. For analysis, the
Dataverse Network offers a user interface to Zelig, a powerful, R-based
statistical computing tool. A comprehensive set of Statistical Analysis
Models are provided.

After you find the tablular data set that you want, access the Subset
and Analysis options to use the online tools. Then, you can *subset
data by variables or observations*, translate it into a convenient
format, download subsets, and apply statistics and analysis.

Network data files (also subsettable) can be subsetted online, and then
downloaded as a subset. Note that network data files cannot be analyzed
online.

Review the Tabular Data Subset and Recode Tips before you start.

**Access Subset and Analysis Options**

You can subset and analyze tabular data files before you download the
file or your subsets.

To access the Subset and Analysis options for a data set:

#. Click the title of the study from which you choose to analyze or
   download a file or subset.
#. Click the Documentation, Data and Analysis tab for the study.
#. In the list of study files, locate the data file that you choose to
   download, subset, or analyze.
   You can download data sets for a file only if the file entry includes
   the subset icon.
#. Click the *Access Subset/Analysis* link associated with the
   selected file.
   If prompted, check the *I accept* box and click Continue to accept
   the Terms of Use.
   You see the Data File page listing data for the file that you choose
   to subset or analyze.

**View Variable Quick Summary**

When a subsettable data file is uploaded for a study, the Dataverse
Network code calculates summary statistics for each variable within that
data file. On any tab of the Data File page, you can view the summary
statistics for each variable in the data file. Information listed
comprises the following:

-  For continuous variables, the application calculates summary
   statistics that are listed in the DDI schema.
-  For discrete variables, the application tabulates values and their
   labels as a frequency table.
   Note, however, that if the number of categories is more than 50, the
   values are not tabulated.
-  The UNF value for each variable is included.

To view summary statistics for a variable:

#. In the Data File page, click any tab.
#. In the variable list on the bottom of the page, the right column is
   labeled *Quick Summary*.
   locate a variable for which you choose to view summary statistics.
   Then, click the Quick Summary icon for that variable to toggle the
   statistic's information on and off.
   You see a small chart that lists information about that variable. The
   information provided depends upon the variable selected.

**Download Tabular Subsets**

You can download a subset of variables within a tabular-data study file.
You also can recode a subset of those variables and download the recoded
subset, if you choose.

To download a subset of variables in tabular data:

#. In the Data File page, click the Download Subset tab.
#. Click the radio button for the appropriate File Format in which to
   download the variables: Text, R Data, S plus, or Stata.
#. On the right side of the tab, use the Show drop-down list to select
   the quantities of variables to list at one time: 10, 20, 50, or All.
#. Scroll down the screen and click the check boxes to select variables
   from the table of available values. When you select a variable, it is
   added to the Selected Variables box at the top of the tab.
   To remove a variable from this box, deselect it from the Variable
   Type list at the bottom of the screen.
   To select all variables, click the check box beside the column name,
   Variable Type.
#. Click the *Create Zip File* button.
   The *Create Zip File* button label changes the following
   format: ``zipFile_<number>.zip``.
#. Click the ``zipFile_<number>.zip`` button and follow your browser's
   prompts to open or save the data file to your computer's disk drive

.. _apply-descriptive-statistics:

**Apply Descriptive Statistics**

When you run descriptive statistics for data, you can do any of the
following with the analysis results:

-  Open the results in a new window to save or print the results.
-  Download the R workspace in which the statistics were analyzed, for
   replication of the analysis. See Replicate Analysis for more
   information.
-  View citation information for the data analyzed, and for the full
   data set from which you selected variables to analyze. See View
   Citations for more information.

To apply descriptive statistics to a data set or subset:

#. In the Data File page, click the Descriptive Statistics tab.
#. Click one or both of the Descriptive Statistics options: Univariate
   Numeric Summaries and Univariate Graphic Summaries.
#. On the right side of the tab, use the Show drop-down list to select
   one of the following options to show variables in predefined
   quantities: 10, 20, 50, or All.
#. Scroll down the screen and click the check boxes to select variables
   from the table of available values. When you select a variable, it is
   added to the Selected Variables box at the top of the tab.
   To remove a variable from this box, deselect it from the Variable
   Type list at the bottom of the screen.
   To select all variables, click the check box beside the column name,
   Variable Type.
#. Click the Run Statistics button.
   You see the Dataverse Analysis page.
#. To save or print the results, scroll to the Descriptive Statistics
   section and click the link *Open results in a new window*. You then
   can print or save the window contents.
   To save the analysis, scroll to the Replication section and click the
   button *zipFile_<number>.zip*.
   Review the Citation Information for the data set and for the subset
   that you analyzed.
#. Click the link *Back to Analysis and Subsetting* to return the
   previous page and continue analysis of the data.

**Recode and Case-Subset Tabular Data**

Review the Tabular Data Recode and Subset Tips before you start work
with a study's files.

To recode and subset variables within a tabular data set:

#. In the Data File page, click the Recode and Case-Subsetting tab.
#. One the right side of the variable list, use the Show drop-down list
   and select one of the following options to show variables in
   predefined quantities: 10, 20, 50, or All.
#. Scroll down the screen and click the check boxes to select variables
   from the table of available values. When you select a variable, it is
   added to the Selected Variables box at the top of the tab.
   To remove a variable from this box, deselect it from the Variable
   Type list at the bottom of the screen.
   To select all variables, click the check box beside the column name,
   Variable Type.
#. Select one variable in the Selected Variables box, and then
   click *Start*.
   The existing name and label of the variable appear in the New
   Variable Name and New Variable Label boxes.
#. In the New Variable Label field, change the variable name to a unique
   value that is not used in the data file.
   The new variable label is optional.
#. In the table below the Variable Name fields, you can check one or
   more values to drop them from the subset, or enter new values,
   labels, or ranges (as a condition) as needed. Click the Add
   Value/Range button to create more entries in the value table.
   Note: Click the ``?`` Info buttons to view tips on how to use the
   Recode and Subset table. Also, See Tabular Data Recode and Subset
   Tips for more information about adding values and ranges.
#. Click the Apply Recodes button.
   Your renamed variables appear at the bottom of the page in the List
   of Recode Variables.
#. Select another variable in the Selected Variables box, click the
   Start button, and repeat the recode action.
   Repeat this process for each variable that you choose to recode.
#. To remove a recoded variable, scroll to the List of Recode Variables
   at the bottom of the page and click the Remove link for the recoded
   variable that you choose to delete from your subset.

.. _perform-advanced-analysis:

**Perform Advanced Analysis**

When you run advanced statistical analysis for data, you can do any of
the following with the analysis results:

-  Open the results in a new window to save or print the results.
-  Download the R workspace in which the statistics were analyzed, for
   replication of the analysis. See Replicate Analysis for more
   information.
-  View citation information for the data analyzed, and for the full
   data set from which you selected variables to analyze. See View
   Citations for more information.

To run statistical models for selected variables:

#. In the Data File page, click the Advanced Statistical Analysis tab.
#. Scroll down the screen and click the check boxes to select variables
   from the table of available values. When you select a variable, it is
   added to the Selected Variables box at the top of the tab.
   To remove a variable from this box, deselect it from the Variable
   Type list at the bottom of the screen.
   To select all variables, click the check box beside the column name,
   Variable Type.
#. Select a model from the Choose a Statistical Model drop-down list.
#. Select one variable in the Selected Variables box, and then click the
   applicable arrow button to assign a function to that variable from
   within the analysis model.
   You see the name of the variables in the appropriate function box.
   Note: Some functions allow a specific type of variable only, while
   other functions allow multiple variable types. Types include
   Character, Continuous, and Discrete. If you assign an incorrect
   variable type to a function, you see an ``Incompatible type`` error
   message.
#. Repeat the variable and function assignments until your model is
   complete.
#. Select your Output options.
#. Click the Run Model button.
   If the statistical model that you defined is incomplete, you first
   are prompted to correct the definition. Correct your model, and then
   click Run Model again.
   You see the Dataverse Analysis page.
#. To save or print the results, scroll to the Advanced Statistical
   Analysis section and click the link *Open results in a new window*.
   You then can print or save the window contents.
   To save the analysis, scroll to the Replication section and click the
   button ``zipFile_<number>.zip``.
   Review the Citation Information for the data set and for the subset
   that you analyzed.
#. Click the link *Back to Analysis and Subsetting* to return the
   previous page and continue analysis of the data.

**Replicate Analysis**

You can save the R workspace in which the Dataverse Network performed an
analysis. You can download the workspace as a zipped archive that
contains four files. Together, these files enable you to recreate the
subset analysis in another R environment:

-  ``citationFile.<identifier>.txt`` - The citation for the subset that you analyzed.
-  ``rhistoryFile.<identifier>.R`` - The R code used to perform the analysis.
-  ``tempsubsetfile.<identifier>.tab`` - The R object file used to perform the analysis.
-  ``tmpRWSfile.<identifier>.RData`` - The subset data that you analyzed.

To download this workspace for your analysis:

#. For any subset, Apply Descriptive Statistics or Perform Advanced
   Analysis.
#. On the Dataverse Analysis or Advanced Statistical Analysis page,
   scroll to the Replication section and click the
   button ``zipFile_<number>.zip``.
#. Follow your browser's prompts to save the zipped archive.
   When the archive file is saved to your local storage, extract the
   contents to use the four files that compose the R workspace.

**Statistical Analysis Models**

You can apply any of the following advanced statistical models to all or
some variables in a tabular data set:

Categorical data analysis: Cross tabulation

Ecological inference model: Hierarchical mulitnomial-direct ecological
inference for R x C tables

Event count models, for event count dependent variables:

-  Negative binomial regression
-  Poisson regression

Models for continuous bounded dependent variables:

-  Exponential regression for duration
-  Gamma regression for continuous positives
-  Log-normal regression for duration
-  Weibull regression for duration

Models for continuous dependent variables:

-  Least squares regression
-  Linear regression for left-censoreds

Models for dichotomous dependent variables:

-  Logistic regression for binaries
-  Probit regression for binaries
-  Rare events logistic regression for binaries

Models for ordinal dependent variables:

-  Ordinal logistic regression for ordered categoricals
-  Ordinal probit regression for ordered categoricals

**Tabular Data Recode and Subset Tips**

Use the following guidelines when working with tabular data files:

-  Recoding:

   -  You must fill at least the first (new value) and last (condition)
      columns of the table; the second column is optional and for a new
      value label.
   -  If the old variable you chose for recoding has information about
      its value labels, you can prefill the table with these data for
      convenience, and then modify these prefilled data.
   -  To exclude a value from your recoding scheme, click the Drop check
      box in the row for that value.

-  Subsetting:

   -  If the variable you chose for subsetting has information about its
      value labels, you can prefill the table with these data for
      convenience.
   -  To exclude a value in the last column of the table, click the Drop
      check box in row for that value.
   -  To include a particular value or range, enter it in the last
      column whose header shows the name of the variable for subsetting.

-  Entering a value or range as a condition for subsetting or recoding:

   -  Suppose the variable you chose for recoding is x.
      If your condition is x==3, enter ``3``.
      If your condition is x < -3, enter ``(--3``.
      If your condition is x > -3, enter ``-3-)``.
      If your condition is -3 < x < 3, enter ``(-3, 3)``.
   -  Use square brackets (``[]``) for closed ranges.
   -  You can enter non-overlapping values and ranges separated by a
      comma, such as ``0,[7-9]``.

.. _network-data:

Network Data
--------------

Network data files (subsettable files) can be subsetted and analyzed
online by using the Dataverse Network application. For analysis, the
Dataverse Network offers generic network data analysis. A list of
Network Analysis Models are provided.

Note: All subsetting and analysis options for network data assume a
network with undirected edges.

After you find the network data set that you want, access the Subset and
Analysis options to use the online tools. Then, you can subset data
by *vertices* or *edges*, download subsets, and apply network
measures.

**Access Network Subset and Analyze Options**

You can subset and analyze network data files before you download the
file or your subsets. To access the Subset and Analysis options for a
network data set:

#. Click the title of the study from which you choose to analyze or
   download a file or subset.
#. Click the Documentation, Data and Analysis tab for the study.
#. In the list of study files, locate the network data file that you
   choose to download, subset, or analyze. You can download data sets
   for a file only if the file entry includes the subset icon.
#. Click the \ *Access Subset/Analysis* link associated with the
   selected file. If prompted, check the \ *I accept* box and click
   Continue to accept the Terms of Use.
   You see the Data File page listing data for the file that you choose
   to subset or analyze.

**Subset Network Data**

There are two ways in which you can subset network data. First, you can
run a manual query, and build a query of specific values for edge or
vertex data with which to subset the data. Or, you can select from among
three automatically generated queries with which to subset the data:

-  Largest graph - Subset the <nth> largest connected component of the
   network. That is, the largest group of nodes that can reach one
   another by walking across edges.
-  Neighborhood - Subset the <nth> neighborhood of the selected
   vertices. That is, generate a subgraph of the original network
   composed of all vertices that are positioned at most <n> steps away
   from the currently selected vertices in the original network, plus
   all of the edges that connect them.

You also can successively subset data to isolate specific values
progressively.

Continue to the next topics for detailed information about subsetting a
network data set.

**Subset Manually**

Perform a manual query to slice a graph based on the attributes of its
vertices or edges. You choose whether to subset the graph based on
vertices or edges, then use the Manual Query Builder or free-text Query
Workspace fields to construct a query based on that element's
attributes. A single query can pertain only to vertices or only to
edges, never both. You can perform separate, sequential vertex or edge
queries.

When you perform a vertex query, all vertices whose attributes do not
satisfy the query are dropped from the graph, in addition to all edges
that touch them. When you perform an edge query, all edges whose
attributes do not satisfy the criteria are dropped, but all vertices
remain *unless* you enable the *Eliminate disconnected vertices* check box. Note that enabling this option drops all
disconnected vertices whether or not they were disconnected before the
edge query.

Review the Network Data Tips before you start work with a study's files.

To subset variables within a network data set by using a manually
defined query:

#. In the Data File page, click the Manual Query radio button near the
   top of the page.
#. Use the Attribute Set drop-down list and select Vertex to subset by
   node or vertex values.
   Select Edge to subset by edge values.
#. Build the first attribute selection value in the Manual Query Builder
   panel:

   #. Select a value in the Attributes list to assign values on which to
      subset.
   #. Use the Operators drop-down list to choose the function by which
      to define attributes for selection in this query.
   #. In the Values field, type the specific values to use for selection
      of the attribute.
   #. Click *Add to Query* to complete the attribute definition for
      selection.
      You see the query string for this attribute in the Query Workspace
      field.

   Alternatively, you can enter your query directly by typing it into
   the Query Workspace field.

#. Continue to add selection values to your query by using the Manual
   Query Builder tools.
#. To remove any verticies that do not connect with other data in the
   set, check the \ *Eliminate disconnected vertices* check box.
#. When you complete construction of your query string, click \ *Run* to
   perform the query.
#. Scroll to the bottom of the window, and when the query is processed
   you see a new entry in the Subset History panel that defines your
   query.

Continue to build a successive subset or download a subset.

**Subset Automatically**

Peform an Automatic Query to select a subgraph of the nextwork based on
structural properties of the network. Remember to review the Network
Data Tips before you start work with a study's files.

To subset variables within a network data set by using an automatically
generated query:

#. In the Data File page, click the Automatic Query radio button near
   the middle of the page.
#. Use the Function drop-down list and select the type of function with
   which to select your subset:

   -  Largest graph - Subset the <nth> largest group of nodes that can
      reach one another by walking across edges.
   -  Neighborhood - Generate a subgraph of the original network
      composed of all vertices that are positioned at most <n> steps
      away from the currently selected vertices in the original network,
      plus all of the edges that connect them. This is the only query
      that can (and generally does) increase the number of vertices and
      edges selected.

#. In the Nth field, enter the <nth> degree with which to select data
   using that function.
#. Click \ *Run* to perform the query.
#. Scroll to the bottom of the window, and when the query is processed
   you see a new entry in the Subset History panel that defines your
   query.

Continue to build a successive subset or download a subset.

**Build or Restart Subsets**

**Build a Subset**

To build successive subsets and narrow your data selection
progressively:

#. Perform a manual or automatic subset query on a selected data set.
#. Perform a second query to further narrow the results of your previous
   subset activity.
#. When you arrive at the subset with which you choose to work, continue
   to analyze or download that subset.

**Undo Previous Subset**

You can reset, or undo, the most recent subsetting action for a data
set. Note that you can do this only one time, and only to the most
recent subset.

Scroll to the Subset History panel at the bottom of the page and
click \ *Undo* in the last row of the list of successive subsets.
The last subset is removed, and the previous subset is available for
downloading, further subsetting, or analysis.

**Restart Subsetting**

You can remove all subsetting activity and restore data to the original
set.

Scroll to the Subset History panel at the bottom of the page and
click \ *Restart* in the row labeled \ *Initial State*.
The data set is restored to the original condition, and is available
for downloading, subsetting, or analysis.

**Run Network Measures**

When you finish selecting the specific data that you choose to analyze,
run a Network Measure analysis on that data. Review the Network Data
Tips before you start your analysis.

#. In the Data File page, click the Network Measure radio button near
   the bottom of the page.
#. Use the Attributes drop-down list and select the type of analysis to
   perform:

   -  Page Rank - Determine how much influence comes from a specific
      actor or node.
   -  Degree - Determine the number of relationships or collaborations
      exist within a network data set.
   -  Unique Degree - Determine the number of collaborators that exist.
   -  In Largest Component - Determine the largest component of a
      network.
   -  Bonacich Centrality - Determine the importance of a main actor or
      node.

#. In the Parameters field, enter the specific value with which to
   subset data using that function:

   -  Page Rank - Enter a value for the parameter <d>, a proportion,
      between 0 and 1.
   -  Degree - Enter the number of relationships to extract from a
      network data set.
   -  Unique Degree - Enter the number of unique relationships to
      extract.
   -  In Largest Component - Enter the number of components to extract
      from a network data set, starting with the largest.

#. Click *Run* to perform the analysis.
#. Scroll to the bottom of the window, and when the analysis is
   processed you see a new entry in the Subset History panel that
   contains your analyzed data.

Continue to download the analyzed subset.

**Download Network Subsets or Measures**

When you complete subsetting and analysis of a network data set, you can
download the final set of data. Network data subsets are downloaded in a
zip archive, which has the name ``subset_<original file name>.zip``.
This archive contains three files:

-  ``subset.xml`` - A GraphML formatted file that contains the final
   subsetted or analyzed data.
-  ``verticies.tab`` - A tabular file that contains all node data for
   the final set.
-  ``edges.tab`` - A tabular file that contains all relationship data
   for the final set.

Note: Each time you download a subset of a specific network data set, a
zip archive is downloaded that has the same name. All three zipped files
within that archive also have the same names. Be careful not to
overwrite a downloaded data set that you choose to keep when you perform
sucessive downloads.

To download a final set of data:

#. Scroll to the Subset History panel on the Data File page.
#. Click *Download Latest Results* at the bottom of the history list.
#. Follow your browser's prompts to open or save the data file to your
   computer's disk drive. Be sure to save the file in a unique location
   to prevent overwritting an existing downloaded data file.

**Network Data Tips**

Use these guidelines when subsetting or analyzing network data:

-  For a Page rank network measure, the value for the parameter <d> is a
   proportion and must be between 0 and 1. Higher values of <d> increase
   dispersion, while values of <d> closer to zero produce a more uniform
   distribution. PageRank is normalized so that all of the PageRanks sum
   to 1.
-  For a Bonacich Centrality network measure, the alpha parameter is a
   proportion that must be between -1 and +1. It is normalized so that
   all alpha centralities sum to 1.
-  For a Bonacich Centrality network measure, the exo parameter must be
   greater than 0. A higher value of exo produces a more uniform
   distribution of centrality, while a lower value allows more
   variation.
-  For a Bonacich Centrality network measure, the original alpha
   parameter of alpha centrality takes values only from -1/lambda to
   1/lambda, where lambda is the largest eigenvalue of the adjacency
   matrix. In this Dataverse Network implementation, the alpha parameter
   is rescaled to be between -1 and 1 and represents the proportion of
   1/lambda to be used in the calculation. Thus, entering alpha=1 sets
   alpha to be 1/lambda. Entering alpha=0.5 sets alpha to be
   1/(2\*lambda).

Data Visualization
===============

Data Visualization allows contributors to make time series
visualizations available to end users. These visualizations may be
viewable and downloadable as graphs or data tables. Please see the
appropriate guide for more information on setting up a visualization or
viewing one.

Explore Data
--------------

The study owner may make a data visualization interface available to
those who can view a study.  This will allow you to select various data
variables and see a time series graph or data table.  You will also be
able to download your custom graph for use in your own reports or
articles.

The study owner will at least provide a list of data measures from which
to choose.   These measures may be divided into types.  If they are you
will be able to narrow the list of measures by first selecting a measure
type.  Once you have selected a measure, if there are multiple variables
associated with the measure you will be able to select one or more
filters to uniquely identify a variable. By default any filter assigned
to a variable will become the label associated with the variable in the
graph or table.   By pressing the Add Line button you will add the
selected variable to your custom graph.

  |image0|

Once you have added data to your graph you will be able to customize it
further.  You will be given a choice of display options made available
by the study owner.  These may include an interactive flash graph, a
static image graph and a numerical data table.   You will also be
allowed to edit the graph title, which by default is the name of the
measure or measures selected. You may also edit the Source Label. 
Other customizable features are the height and the legend location of
the image graph.  You may also select a subset of the data by selecting
the start and end points of the time series.  Finally, on the display
tab you may opt to display the series as indices in which case a single
data point known as the reference period will be designated as 100 and
all other points of the series will be calculated relative to the
reference period.  If you select data points that do not have units in
common (i.e. one is in percent while the other is in dollars) then the
display will automatically be set to indices with the earliest common
data point as the default reference period.

|image1| 

On the Line Details tab you will see additional information on the data
you have selected.  This may include links to outside web pages that
further explain the data.  On this tab you will also be able to edit the
label or delete the line from your custom graph.

On the Export tab you will be given the opportunity to export your
custom graph and/or data table.   If you select multiple files for
download they will be bound together in a single zip file. 

The Refresh button clears any data that you have added to your custom
graph and resets all of the display options to their default values.

Set Up
--------

This feature allows you to make time series visualizations available to
your end users.   These visualizations may be viewable and downloadable
as graphs or data tables.  In the current beta version of the feature
your data file must be subsettable and must contain at least one date
field and one or more measures.  You will be able to associate data
fields from your file to a time variable and multiple measures and
filters. 

When you select Set Up Exploration from within a study, you must first
select the file for which you would like to set up the exploration.  The
list of files will include all subsettable data files within the study.

Once you have selected a file you will go to a screen that has 5 tabs to
guide you through the data visualization set-up. (In general, changes
made to a visualization on the individual tabs are not saved to the
database until the form’s Save button is pressed.  When you are in add
or edit mode on a tab, the tab will have an update or cancel button to
update the “working copy” of a visualization or cancel the current
update.)

**Time Variable**

On the first tab you select the time variable of your data file.  The
variable list will only include those variables that are date or time
variables.  These variables must contain a date in each row.  You may
also enter a label in the box labeled Units.  This label will be
displayed under the x-axis of the graph created by the end user.

|image2|

**Measures**

On the Measures tab you may assign measures to the variables in your
data file.  First you may customize the label that the end user will see
for measures.  Next you may add measures by clicking the “Add Measure”
link.  Once you click that link you must give your measure a unique
name.  Then you may assign Units to it.  Units will be displayed as the
y-axis label of any graph produced containing that measure.  In order to
assist in the organizing of the measures you may create measure types
and assign your measures to one or more measure types.  Finally, the
list of variables for measures will include all those variables that are
entered as numeric in your data file.  If you assign multiple variables
to the same measure you will have to distinguish between them by
assigning appropriate filters.   For the end user, the measure will be
the default graph name.

|image3|  

**Filters**

On the filters tab you may assign filters to the variables in your data
file.  Generally filters contain demographic, geographic or other
identifying information about the variables.  For a given group of
filters only one filter may be assigned to a single variable.  The
filters assigned to a variable must be sufficient to distinguish among
the variables assigned to a single measure.   Similar to measures,
filters may be assigned to one or more types.   For the end user the
filter name will be the default label of the line of data added to a
graph.

|image4|

| 

**Sources**

On the Sources tab you can indicate the source of each of the variables
in your data file.  By default, the source will be displayed as a note
below the x-axis labels.  You may assign a single source to any or all
of your data variables.  You may also assign multiple sources to any of
your data variables.

|image5|

| 

**Display**

On the Display tab you may customize what the end user sees in the Data
Visualization interface.  Options include the data visualization formats
made available to the end user and default view, the Measure Type label,
and the Variable Info Label.

| 
|  |image6|  

**Validate Button**

When you press the “Validate” button the current state of your
visualization data will be validated.  In order to pass validation your
data must have one time variable defined.  There must also be at least
one measure variable assigned.  If more than one variable is assigned to
a given measure then filters must be assigned such that each single
variable is defined by the measure and one or more filters.  If the data
visualization does not pass validation a detailed error message
enumerating the errors will be displayed.

**Release Button**

Once the data visualization has been validated you may release it to end
users by pressing the “Release” button.  The release button will also
perform a validation.  Invalid visualizations will not be released, but
a detailed error message will not be produced. 

**Save Button**

The “Save” button will save any changes made to a visualization on the
tabs to the database.   If a visualization has been released and changes
are saved that would make it invalid the visualization will be set to
“Unreleased”.

**Exit Button**

To exit the form press the “Exit” button.  You will be warned if you
have made any unsaved changes.

**Examples**

Simplest case – a single measure associated with a single variable.

Data variable contains information on average family income for all
Americans.  The end user of the visualization will see an interface as
below:

|image7|

Complex case - multiple measures and types along with multiple filters
and filter types.  If you have measures related to both income and
poverty rates you can set them up as measure types and associate the
appropriate measures with each type.  Then, if you have variables
associated with multiple demographic groups you can set them up as
filters.  You can set up filter types such as age, gender, race and
state of residence.  Some of your filters may belong to multiple types
such as males age 18-34.

|image8|

.. |image0| image:: ./datausers-guides_files/measure_selected.png
.. |image1| image:: ./datausers-guides_files/complex_graph_screenshot.png
.. |image2| image:: ./datausers-guides_files/edittimevariablescreenshot.png
.. |image3| image:: ./datausers-guides_files/editmeasuresscreenshot.png
.. |image4| image:: ./datausers-guides_files/editfiltersscreenshot.png
.. |image5| image:: ./datausers-guides_files/sourcetabscreenshot.png
.. |image6| image:: ./datausers-guides_files/displaytabscreenshot.png
.. |image7| image:: ./datausers-guides_files/simple_explore_data.png
.. |image8| image:: ./datausers-guides_files/complex_exploration.png
