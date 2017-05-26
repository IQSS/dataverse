Data Access API
===============

The Data Access API provides programmatic download access to the files stored under Dataverse. 
More advanced features of the Access API include format-specific transformations (thumbnail generation/resizing for images; converting tabular data into alternative file formats) and access to the data-level metadata that describes the contents of the tabular files. 

.. contents:: |toctitle|
   :local:

Basic File Access
-----------------

Basic acces URI: 

``/api/access/datafile/$id``


Parameters:
~~~~~~~~~~~

``format`` 

the following parameter values are supported (for tabular data files only):

==============  ===========
Value           Description
==============  ===========
original        "Saved Original", the proprietary (SPSS, Stata, R, etc.) file from which the tabular data was ingested;
RData           Tabular data as an R Data frame (generated; unless the "original" file was in R);
prep		"Pre-processed data", in JSON. (TODO: *get a proper description of the feature from James/Vito*)
==============  ===========

``imageThumb``

the following parameter values are supported (for image and pdf files only): 

==============  ===========
Value           Description
==============  ===========
true            Generates a thumbnail image, by rescaling to the default thumbnail size (64 pixels)
``N``           Rescales the image to ``N`` pixels.
==============  ===========

``vars``

For column-wise subsetting (available for tabular data files only).

Example: 

``http://localhost:8080/api/meta/datafile/6?vars=123,127``

where 123 and 127 are the ids of data variables that belong to the data file with the id 6.

Multiple File ("bundle") download
---------------------------------

``/api/access/datafiles/$id1,$id2,...$idN``

Returns the files listed, zipped. 

Parameters: 
~~~~~~~~~~~
none.

"All Formats" bundled access for Tabular Files. 
-----------------------------------------------

``/api/access/datafile/bundle/$id``

This is convenience packaging method is available for tabular data files. 
It returns a zipped bundle that contains the data in the following formats: 

* Tab-delimited;
* "Saved Original", the proprietary (SPSS, Stata, R, etc.) file from which the tabular data was ingested;
* Generated R Data frame (unless the "original" above was in R);
* Data (Variable) metadata record, in DDI XML;
* File citation, in Endnote and RIS formats. 

Parameters: 
~~~~~~~~~~~
none.

Data Variable Metadata Access
-----------------------------

**These methods are only available for tabular data files. (i.e., data files with associated data table and variable objects).**


``/api/access/datafile/$id/metadata/ddi``

In its basic form the verb above returns a DDI fragment that describes the file and the data variables in it. 
The DDI XML is the only format supported so far. In the future, support for formatting the output in JSON will 
(may?) be added.

The DDI returned will only have 2 top-level sections: 

* a single ``fileDscr``, with the basic file information plus the numbers of variables and observations and the UNF of the file.  
* a single ``dataDscr`` section, with one ``var`` section for each variable. 

Example: 

``http://localhost:8080/api/meta/datafile/6``

.. code-block:: xml

   <codeBook version="2.0">
      <fileDscr ID="f6">
         <fileTxt>
            <fileName>_73084.tab</fileName>
            <dimensns>
               <caseQnty>3</caseQnty>
               <varQnty>2</varQnty>
            </dimensns>
            <fileType>text/tab-separated-values</fileType>
         </fileTxt>
         <notes level="file" type="VDC:UNF" subject="Universal Numeric Fingerprint">UNF:6:zChnyI3fjwNP+6qW0VryVQ==</notes>
      </fileDscr>
      <dataDscr>
         <var ID="v1" name="id" intrvl="discrete">
            <location fileid="f6"/>
            <labl level="variable">Personen-ID</labl>
            <sumStat type="mean">2.0</sumStat>
            <sumStat type="mode">.</sumStat>
            <sumStat type="medn">2.0</sumStat>
            <sumStat type="stdev">1.0</sumStat>
            <sumStat type="min">1.0</sumStat>
            <sumStat type="vald">3.0</sumStat>
            <sumStat type="invd">0.0</sumStat>
            <sumStat type="max">3.0</sumStat>
            <varFormat type="numeric"/>
            <notes subject="Universal Numeric Fingerprint" level="variable" type="VDC:UNF">UNF:6:AvELPR5QTaBbnq6S22Msow==</notes>
         </var>
         <var ID="v3" name="sex" intrvl="discrete">
            <location fileid="f6"/>
            <labl level="variable">Geschlecht</labl>
            <sumStat type="mean">1.3333333333333333</sumStat>
            <sumStat type="max">2.0</sumStat>
            <sumStat type="vald">3.0</sumStat>
            <sumStat type="mode">.</sumStat>
            <sumStat type="stdev">0.5773502691896257</sumStat>
            <sumStat type="invd">0.0</sumStat>
            <sumStat type="medn">1.0</sumStat>
            <sumStat type="min">1.0</sumStat>
            <catgry>
               <catValu>1</catValu>
               <labl level="category">Mann</labl>
            </catgry>
            <catgry>
               <catValu>2</catValu>
               <labl level="category">Frau</labl>
            </catgry>
            <varFormat type="numeric"/>
            <notes subject="Universal Numeric Fingerprint" level="variable" type="VDC:UNF">UNF:6:XqQaMwOA63taX1YyBzTZYQ==</notes>
         </var>
      </dataDscr>
   </codeBook>



More information on the DDI is available at (TODO). 

Advanced options/Parameters: 

It is possible to request only specific subsets of, rather than the
full file-level DDI record. This can be a useful optimization, in
cases such as when an application needs to look up a single variable;
especially with data files with large numbers of variables.

Partial record parameters: 

(TODO). 

``/api/access/datafile/$id/metadata/preprocessed``

This method provides the "Pre-processed Data" - a summary record that describes the values of the data vectors in the tabular file, in JSON. These metadata values are used by TwoRavens, the companion data exploration utility of the Dataverse application. 

Authentication and Authorization
-------------------------------- 

Data Access API supports both session- and API key-based authentication. 

If a session is available, and it is already associated with an authenticated user, it will be used for access authorization. If not, or if the user in question is not authorized to access the requested object, an attempt will be made to authorize based on an API key, if supplied. 
All of the API verbs above support the key parameter ``key=...``.