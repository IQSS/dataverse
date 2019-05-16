Data Access API
===============

The Data Access API provides programmatic download access to the files stored under Dataverse. 
More advanced features of the Access API include format-specific transformations (thumbnail generation/resizing for images; converting tabular data into alternative file formats) and access to the data-level metadata that describes the contents of the tabular files. 

.. contents:: |toctitle|
   :local:

Basic File Access
-----------------

Basic access URI: 

``/api/access/datafile/$id``

.. note:: Files can be accessed using persistent identifiers. This is done by passing the constant ``:persistentId`` where the numeric id of the file is expected, and then passing the actual persistent id as a query parameter with the name ``persistentId``.

  Example: Getting the file whose DOI is *10.5072/FK2/J8SJZB* ::

    GET http://$SERVER/api/access/datafile/:persistentId/?persistentId=doi:10.5072/FK2/J8SJZB


Parameters:
~~~~~~~~~~~

``format`` 

the following parameter values are supported (for tabular data files only):

==============  ===========
Value           Description
==============  ===========
original        "Saved Original", the proprietary (SPSS, Stata, R, etc.) file from which the tabular data was ingested;
RData           Tabular data as an R Data frame (generated; unless the "original" file was in R);
prep		"Pre-processed data", in JSON.
subset          Column-wise subsetting. You must also supply a comma separated list of variables in the "variables" query parameter. In this example, 123 and 127 are the database ids of data variables that belong to the data file with the id 6: ``curl 'http://localhost:8080/api/access/datafile/6?format=subset&variables=123,127'``.
==============  ===========

---------------------------

``noVarHeader``

(supported for tabular data files only; ignored for all other file types)

==============  ===========
Value           Description
==============  ===========
true|1          Tab-delimited data file, without the variable name header (added to tab. files by default)
==============  ===========

---------------------------

``imageThumb``

the following parameter values are supported (for image and pdf files only): 

==============  ===========
Value           Description
==============  ===========
true            Generates a thumbnail image, by rescaling to the default thumbnail size (64 pixels)
``N``           Rescales the image to ``N`` pixels.
==============  ===========

Multiple File ("bundle") download
---------------------------------

``/api/access/datafiles/$id1,$id2,...$idN``

Returns the files listed, zipped. 

.. note:: If the request can only be completed partially - if only *some* of the requested files can be served (because of the permissions and/or size restrictions), the file MANIFEST.TXT included in the zipped bundle will have entries specifying the reasons the missing files could not be downloaded. IN THE FUTURE the API will return a 207 status code to indicate that the result was a partial success. (As of writing this - v.4.11 - this hasn't been implemented yet)

.. note:: If any of the datafiles have the ``DirectoryLabel`` attributes in the corresponding ``FileMetadata`` entries, these will be added as folders to the Zip archive, and the files will be placed in them accordingly. 

Parameters: 
~~~~~~~~~~~

``format`` 
the following parameter values are supported (for tabular data files only):

==============  ===========
Value           Description
==============  ===========
original        "Saved Original", the proprietary (SPSS, Stata, R, etc.) file from which the tabular data was ingested;
==============  ===========


"All Formats" bundled download for Tabular Files. 
-------------------------------------------------

``/api/access/datafile/bundle/$id``

This is a convenience packaging method available for tabular data files. 
It returns a zipped bundle that contains the data in the following formats: 

* Tab-delimited;
* "Saved Original", the proprietary (SPSS, Stata, R, etc.) file from which the tabular data was ingested;
* Generated R Data frame (unless the "original" above was in R);
* Data (Variable) metadata record, in DDI XML;
* File citation, in Endnote and RIS formats. 


Parameters: 
~~~~~~~~~~~

``fileMetadataId``

==============  ===========
Value           Description
==============  ===========
ID              Exports file with specific file metadata ``ID``. For example for data file with id 6 and file metadata id 2: ``curl 'http://localhost:8080/api/access/datafile/6?fileMetadataId=2'``
==============  ===========


Data Variable Metadata Access
-----------------------------

**These methods are only available for tabular data files. (i.e., data files with associated data table and variable objects).**


``/api/access/datafile/$id/metadata/ddi``

In its basic form the verb above returns a DDI fragment that describes the file and the data variables in it. 

The DDI returned will only have two top-level sections:

* a single ``fileDscr``, with the basic file information plus the numbers of variables and observations and the UNF of the file.  
* a single ``dataDscr`` section, with one ``var`` section for each variable. 

Example: 

``http://localhost:8080/api/access/datafile/6/metadata/ddi``

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


Parameters: 
~~~~~~~~~~~

``fileMetadataId``

==============  ===========
Value           Description
==============  ===========
ID              Exports file with specific file metadata ``ID``. For example for data file with id 6 and file metadata id 2: ``curl 'http://localhost:8080/api/access/datafile/6?fileMetadataId=2'``
==============  ===========


More information on DDI is available in the :doc:`/user/tabulardataingest/ingestprocess` section of the User Guide.

Advanced options/Parameters: 

It is possible to request only specific subsets of, rather than the
full file-level DDI record. This can be a useful optimization, in
cases such as when an application needs to look up a single variable;
especially with data files with large numbers of variables. See
``variables=123,127`` in the example above.

Preprocessed Data
-----------------

``/api/access/datafile/$id/metadata/preprocessed``

This method provides the "preprocessed data" - a summary record that describes the values of the data vectors in the tabular file, in JSON. These metadata values are used by TwoRavens, the companion data exploration utility of the Dataverse application. Please note that this format might change in the future.

Authentication and Authorization
-------------------------------- 

Data Access API supports both session- and API key-based authentication. 

If a session is available, and it is already associated with an authenticated user, it will be used for access authorization. If not, or if the user in question is not authorized to access the requested object, an attempt will be made to authorize based on an API key, if supplied. 
All of the API verbs above support the key parameter ``key=...`` as well as the newer ``X-Dataverse-key`` header. For more details, see "Authentication" in the :doc:`intro` section.

Access Requests and Processing
------------------------------

All of the following endpoints take the persistent identifier as a parameter in place of 'id'.

Allow Access Requests:
~~~~~~~~~~~~~~~~~~~~~~

Allow or disallow users from requesting access to restricted files in a dataset where id is the database id of the dataset or pid is the persistent id (DOI or Handle) of the dataset to update. 

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT -d true http://$SERVER/api/access/{id}/allowAccessRequest
    
A curl example using a ``pid``::

   curl -H "X-Dataverse-key:$API_TOKEN" -X PUT -d true http://$SERVER/api/access/:persistentId/allowAccessRequest?persistentId={pid}    
    

Request Access:
~~~~~~~~~~~~~~~
``/api/access/datafile/$id/requestAccess``

This method requests access to the datafile whose id is passed on the behalf of an authenticated user whose key is passed. Note that not all datasets allow access requests to restricted files. 

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT http://$SERVER/api/access/datafile/{id}/requestAccess
    
Grant File Access:
~~~~~~~~~~~~~~~~~~ 

``/api/access/datafile/{id}/grantAccess/{identifier}``

This method grants access to the datafile whose id is passed on the behalf of an authenticated user whose user identifier is passed with an @ prefix. The key of a user who can manage permissions of the datafile is required to use this method.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT http://$SERVER/api/access/datafile/{id}/grantAccess/{@userIdentifier}
    
Reject File Access:
~~~~~~~~~~~~~~~~~~~ 

``/api/access/datafile/{id}/rejectAccess/{identifier}``

This method rejects the access request to the datafile whose id is passed on the behalf of an authenticated user whose user identifier is passed with an @ prefix. The key of a user who can manage permissions of the datafile is required to use this method.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT http://$SERVER/api/access/datafile/{id}/rejectAccess/{@userIdentifier}
    
Revoke File Access:
~~~~~~~~~~~~~~~~~~~ 

``/api/access/datafile/{id}/revokeAccess/{identifier}``

This method revokes previously granted access to the datafile whose id is passed on the behalf of an authenticated user whose user identifier is passed with an @ prefix. The key of a user who can manage permissions of the datafile is required to use this method.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X DELETE http://$SERVER/api/access/datafile/{id}/revokeAccess/{@userIdentifier}    
    
List File Access Requests:
~~~~~~~~~~~~~~~~~~~~~~~~~~ 

``/api/access/datafile/{id}/listRequests``

This method returns a list of Authenticated Users who have requested access to the datafile whose id is passed. The key of a user who can manage permissions of the datafile is required to use this method.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X GET http://$SERVER/api/access/datafile/{id}/listRequests
