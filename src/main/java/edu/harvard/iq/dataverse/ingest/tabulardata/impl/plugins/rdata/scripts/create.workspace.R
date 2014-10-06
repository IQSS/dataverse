# 
options(digits.secs=3)

directories <- list()
directories$parent <- tempfile(pattern='DvnRWorkspace', tmpdir='/tmp/Rserv')
 
created <- list()

for (key in names(directories)) {
  cat(paste(directories[[key]], "\n",sep=""))
  dir.name <- directories[[key]]

  if (dir.create(dir.name))
    created[[key]] <- dir.name
}

created