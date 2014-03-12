# 
options(digits.secs=3)

directories <- list()
directories$parent <- tempfile('DvnRWorkspace')
directories$web <- file.path(directories$parent, 'web')
directories$dvn <- file.path(directories$parent, 'dvn')
directories$dsb <- file.path(directories$parent, 'dsb')
 
created <- list()

for (key in names(directories)) {
  dir.name <- directories[[key]]

  if (dir.create(dir.name))
    created[[key]] <- dir.name
}

created