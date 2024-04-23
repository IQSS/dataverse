SPSS
+++++++

SPSS data files (POR and SAV formats).

.. contents:: |toctitle|
	:local:

Supported Versions
------------------

The Dataverse Software supports reading of all SPSS versions 7 to 22. But please see the "Limitations" section. 

Limitations
-----------

SPSS does not openly publish the specifications of their proprietary file formats. Our ability to read and parse their files is based on some documentation online from unofficial sources, and some reverse engineering. Because of that we cannot, unfortunately, guarantee to be able
to process *any* SPSS file uploaded. 

However, we've been improving this process for a few years by now, and it should be quite robust in the current version of the Dataverse Software. Thus your chances of success - uploading an SPSS files and having it turned into a fully functional tabular data table in the Dataverse installation - should be reasonably good. 

If you are having a problem with a particular SPSS file, please contact our support and we'll see if it's something we could further improve in our SPSS ingest driver. 

SPSS Control Cards - not supported
-----------------------------------

In the past, there have been attempts to support SPSS "control cards", in addition to the .SAV and .POR files; both in the early VDC software, and in some versions of DVN. A "control card" is a plain text file with a set of SPSS commands that describe the raw data, provided in a separate, fixed-width-columns or comma-separated-values file. For various reasons, it has never been very successful.  

Please contact us if you have any questions and/or strong feelings on this issue, or if you have serious amounts of data exclusively available in this format that you would like to ingest under the Dataverse Software. 

Support for Language Encodings in SPSS
---------------------------------------

Historically, there was no support for specifying a particular language/code page encoding for the data stored in an SPSS file. Meaning, text values in none-ASCII encodings, or non-Latin characters could be entered and stored, but there was no setting to unambiguously specify what language, or what character set it was. By default, the Dataverse Software will try to interpret binary characters as UTF8. If that's not working - for example, if the descriptive labels and/or categorical values ingest as garbage - and if you happen to know what encoding was used in the original file, you can now specify it in the Ingest Options. 

For example, if you know that the text in your SAV file is in Mandarin, and is encoded using the GB2312, specify it as follows: 

Upload your file, in the "Edit Files" tab of the Dataset page. Once the file is recognized as SPSS/save, and *before* you click Save, go into the "Advanced Ingest Options", and select "Simplified Chinese, GB2312" in the nested menu under "Language Encoding" -> "East Asian".

