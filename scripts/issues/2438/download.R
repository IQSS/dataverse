arg <- commandArgs(trailingOnly = TRUE)

download.dataverse.file <- function(url) {
  if (length(url) == 0L)   {
    return(
      "Please provide a URL to a file: http://guides.dataverse.org/en/latest/api/dataaccess.html"
    )
  }
  # Examples of URLs for tsv, original, RData, JSON, DDI/XML:
  # https://groups.google.com/d/msg/dataverse-community/fFrJi7NnBus/LNpfXItbtZYJ
  #
  # This script assume the tsv URL is used. File id 91 is just an example. You must
  # look up the id of the file. As of this writing the easiest way is via SWORD:
  # https://github.com/IQSS/dataverse/issues/1837#issuecomment-121736332
  #
  # url.to.download = 'https://demo.dataverse.org/api/v1/access/datafile/91'
  url.to.download = url
  tsvfile = 'file.tsv'
  download.file(url = url.to.download, destfile =
                  tsvfile, method = 'curl')
  mydata <- read.table(tsvfile, header = TRUE, sep = "\t")
  print(mydata)
  unlink(tsvfile)
}

download.dataverse.file(arg)
