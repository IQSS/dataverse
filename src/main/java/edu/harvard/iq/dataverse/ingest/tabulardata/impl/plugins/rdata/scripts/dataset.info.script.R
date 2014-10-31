#' 
#' @author Matt Owen
#' @since 2013-04-11

#' Create a variable.meta.data Object
#' Generates an object that represents the metadata associated with a vector
#' in R. This is used to later recreate the object at a later step.
#' @param values a vector that 
#' @return a list containing the slots: type, type.string, class, and levels
VariableMetaData <- function (values) {
  UseMethod("VariableMetaData")
}
#' Create a variable.meta.data Object from an Unspecified Object
#' @param values a vector of any type
VariableMetaData.default <- function (values) {
  list(type = 0, type.string = "character", class = class(values), levels = NULL, format = NULL)
}
#' Create a variable.meta.data Object from a Factor
#' @param values a vector of factors, potentially with levels
VariableMetaData.factor <- function (values) {
  if (is.ordered(values)) 
  {
   list(type = 0, type.string = "factor", class = class(values), levels = levels(values), format = "ordered")
  }
  else 
  {
   list(type = 0, type.string = "factor", class = class(values), levels = levels(values), format = NULL)
  }
}
#' @param values a vector of logical values
VariableMetaData.logical <- function (values) {
  list(type = 1, type.string = "logical", class = class(values), levels = NULL, format = NULL)
}
#' @param values a vector of integers
VariableMetaData.integer <- function (values) {
  list(type = 1, type.string = "integer", class = class(values), levels = NULL, format = NULL)
}
#' @param values a vector of factors, potentially with levels
VariableMetaData.numeric <- function (values) {
  list(type = 1, type.string = "numeric", class = class(values), levels = NULL, format = NULL)
}
#' @param values a vector of POSIXt objects
VariableMetaData.POSIXt <- function (values) {
#  list(type = 1, type.string = "DateTime", class = class(values), levels = NULL, format = "Y-m-d H:M:s:S z")
  list(type = 1, type.string = "DateTime", class = class(values), levels = NULL, format = "yyyy-MM-dd HH:mm:ss.SSS z")
}
#' @param values a vector of Date objects
VariableMetaData.Date <- function (values) {
#  list(type = 1, type.string = "Date", class = class(values), levels = NULL, format = "Y-m-d")
  list(type = 1, type.string = "Date", class = class(values), levels = NULL, format = "yyyy-MM-dd")
}

#' Create a List Pairing Column-numbers
#'
VariableMetaDataTable <- function (data) {
  # Create result object
  result <- list()

  # Iterate through columns, storing meta-data
  for (k in 1:ncol(data))
    result[[k]] <- VariableMetaData(data[[k]])

  # Return result
  result
}

# Start of actual script
# ...
# ...

types <- c()

for (col in colnames(data.set)) {
  types <- c(types, class(data.set[, col])[1])
}

list(varNames = colnames(data.set), caseQnty = nrow(data.set), dataTypes = types, meta.info = VariableMetaDataTable(data.set))