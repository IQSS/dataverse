Stata
++++++++

.. contents:: |toctitle|
	:local:

Of all the third party statistical software providers, Stata does the best job at documenting the internal format of their files, by far. And at making that documentation freely and easily available to developers (yes, we are looking at you, SPSS). Because of that, Stata is the best supported format for tabular data ingest.  

Supported Format Versions
=========================


Of the **"New Stata dta"** formats (variations of the format in use since Stata 13) our ingest supports the following:


=================== ================= =================
Stata format name   Introduced in     Used by 
=================== ================= =================
dta_117             Stata 13          Stata 13
dta_118             Stata 14          Stata 14 - 19 
dta_119             Stata 15          Stata 15 - 19
=================== ================= =================

This means that, in theory, every dta file produced by Stata v.13 - 17 should be ingestible. (Please see below for more information on Stata 18 and 19). In practice, we cannot *guarantee* that our code will in fact be able to parse any such file. There is always a possibility that we missed a certain way to compose the data that the ingest will fail to understand. So, if you encounter such an error, where Dataverse **tries but fails** to ingest a Stata file in one of these 3 formats, please open a GitHub issue and we will try to address it. Please note that this a different scenario from when Dataverse skips even trying to ingest a file (with no ingest errors shown in the UI). As that will in most cases be the result of the file exceeding the size limit set by the Dataverse instance administrators, or a client uploading the file with a wrong content type attached, so that Dataverse fails to recognize it as Stata.

Please note that there was an issue present in older versions of Dataverse where Stata 13-17 files were not ingested when deposited via direct upload to S3. The issue was accompanied by the confusing error message ``The file is not in a STATA format that we can read or support`` shown in the UI. Fortunately, a case like this can be addressed by running the reIngest API on the affected file. 

The following 2 formats have been introduced in 2024 and are **not yet supported**:

=================== ================ =================
Stata format name   Introduced in    Used by 
=================== ================ =================
dta_120             Stata 18         Stata 18 - 19
dta_121             Stata 18         Stata 18 - 19 
=================== ================ =================

Please note however, that this does not mean that no files produced by Stata 18 or 19 are ingestable! In reality, in most cases these versions of Stata still save files in the ``dta_118`` (i.e., Stata 14) format, with the later formats only utilized when necessary. When, for example, the number of variables in the datafile exceeds what ``dta_118`` can handle, or when it has "alias variables" introduced in Stata 18. Case in point, in a year since the introduction of these 2 newest formats, it appears that not a single file in either of them has been uploaded on production Dataverse instance at IQSS. We are planning to eventually add support for these formats, but it is not considered a priority as of yet. However, please feel free to open a GitHub issue if this is an important use case for you.

**"Old Stata"**, a distinctly different format used by Stata versions prior to 13 is supported. 
However, this functionality is considered legacy code that we no longer actively maintain. If any problems or bugs are found in it, we cannot promise that the core development team will be able to prioritize looking into such. We will of course gladly accept a properly submitted pull request from the user community. 
