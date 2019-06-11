available.data.frames <- ls()
available.data.frames <- Filter(function (y) is.data.frame(get(y)), available.data.frames)
data.set <- available.data.frames[[1]]
data.set <- get(data.set)
colnames(data.set)