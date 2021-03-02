Restricted Files and DDI "dataDscr" Information (Summary Statistics, Variable Names, Variable Labels)

In previous releases, DDI "dataDscr" information (summary statistics, variable names, and variable labels, sometimes known as "variable metadata") for tabular files that were ingested successfully were available even if files were restricted. This has been changed in the following ways:

- At the dataset level, DDI exports no longer show "dataDscr" information for restricted files. There is only one version of this export and it is the version that's suitable for public consumption with the "dataDscr" information hidden for restricted files.
- Similarly, at the dataset level, the DDI HTML Codebook no longer shows "dataDscr" information for restricted files. 
- At the file level, "dataDscr" information is no longer publicly available for restricted files. In practice, it was only possible to get this publicly via API (the download/access button was hidden).
- At the file level, "dataDscr" (variable metadata) information can still be downloaded for restricted files if you have access to download the file.

After upgrading, you should re-export to replace cached DDI exports with restricted summary stats with DDI exports fit for public consumption:

    curl http://localhost:8080/api/admin/metadata/reExportAll

For details on this operation, see https://guides.dataverse.org/en/5.4/admin/metadataexport.html
