## Bug Fixes

- Loading the files of a dataset version no longer runs several database queries per file. For datasets with tens of thousands of files, this made the file page take many seconds to load. See #12739.
