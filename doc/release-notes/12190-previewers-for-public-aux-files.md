## Allow unrestricted preview of public auxiliary files

In Dataverse, restricted datafiles can have public auxiliary files 
(e.g. ones that only contain metadata or a less sensitive subset of the data). 
Public NcML auxiliary files are automatically generated from NetCDF and HDF5 files, for example, even if they are restricted.
With this Dataverse release it is now possible to create previewers for this case, i.e. ones
that allow users who cannot view the restricted file to still see a preview 
based on the auxiliary file. (The only known examples of this to date are 
the previewers for qualitative data analysis files (conforming to the 
[REFI-QDAS standard](https://www.qdasoftware.org/)) which were
[presented by QDR at the 2026 Dataverse Community meeting in Barcelona](https://docs.google.com/presentation/d/1CF9wdJncwFSqqnsDjQ4ktxSei4cFD2Q-glW7dKxoKxM/edit?usp=sharing)
that support creation and viewing of redacted QDAS files.) 
