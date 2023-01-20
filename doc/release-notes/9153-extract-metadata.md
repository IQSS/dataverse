For NetCDF and HDF5 files, an attempt will be made to extract metadata in NcML (XML) format and save it as an auxiliary file.

An "extractNcml" API endpoint has been added, especially for installations with existing NetCDF and HDF5 files. After upgrading, they can iterate through these files and try to extract an NcML file.
