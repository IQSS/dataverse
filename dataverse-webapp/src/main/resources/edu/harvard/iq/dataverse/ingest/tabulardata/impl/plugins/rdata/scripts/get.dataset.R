# Get a Dataset from an Rdata Object
#
# Searches the workspace for available "data.frame" objects, and returns the
# first one alphabetically.
#
# author: Matt Owen
# date:   03-29-2013

available.data.frames <- ls()
available.data.frames <- Filter(function (y) is.data.frame(get(y)), available.data.frames)
data.set <- available.data.frames[[1]]
data.set <- get(data.set)