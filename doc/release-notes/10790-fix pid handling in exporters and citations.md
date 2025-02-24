### Improvements to PID formatting in exports and citations

Multiple small issues with the formatting of PIDs in the
DDI exporters, and EndNote and BibTeX citation formats have
been addressed. These should improve the ability to import
Dataverse citations into reference managers and fix potential
issues harvesting datasets using PermaLinks.

Backward Incompatibility

Changes to PID formatting occur in the DDI/DDI Html export formats
and the EndNote and BibTex citation formats. These changes correct
errors and improve conformance with best practices but could break
parsing of these formats.
 
For more information, see #10790.
