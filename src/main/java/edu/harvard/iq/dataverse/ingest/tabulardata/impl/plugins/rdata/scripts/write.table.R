#' Write a DVN Tab File
#' Prints its required argument "data.set" to a file or connection.
#' This is used internally by DVN's R-ingester to appropriately create TAB
#' files, the DVN's archival format. Major differences between this and standard
#' write.table are that it has fewer options, and outputs Date and POSIXt
#' entries with timezone information
#' @param data.set a data.frame or matrix object to be printed
#' @param ... parameters passed to the "write.table" function
#' @return NULL (invisibly)
write.dvn.table <- function (data.set, ...) {
  # Set millisecond precision in environment, and save old state

  saved.options <- options(digits.secs = 3)

  # Return a list of indices that need to be quoted
  needs.quotes <- function (data.set, subset = NULL) {
    if (is.null(subset)) {
      subset <- 1:ncol(data.set)
    }

    quotes <- c()

    for (k in subset) {
      if (is.character(data.set[, k]) || is.factor(data.set[, k]))
        quotes <- c(quotes, k)
    }

    quotes
  }

  # Define reformat method
  reformat <- function (x)
    UseMethod("reformat")

  # Default method does nothing
  reformat.default <- function (x)
    x

  # Convert POSIXt date-times to the good format
  reformat.POSIXt <- function (x) {
    if (attr(x,"tzone") == "" || is.null(attr(x,"tzone"))) {
       # If no timezone is explicitly defined, we'll
       # display the actual UTC/GMT time value stored:
       attr(x, "tzone") <- "UTC"
       # Strip the milliseconds, if all zeros: 
       #sub("\\.000", "", format(x, format = "%F %H:%M:%OS"))
       sub("\\.000", "", paste(format(x, format = "%F %H:%M:%OS"), attr(x,"tzone"), sep = " "))
    } else {
       # If there is a time zone, we will preserve it as is, 
       # without making any effort to validate it. We'll 
       # leave it to the app to try to interpret it.
       # Also, strip the milliseconds, if all zeros: 
       sub("\\.000", "", paste(format(x, format = "%F %H:%M:%OS"), attr(x,"tzone"), sep = " "))
    }
  }

  # Convert date
  reformat.Date <- function (x)
    format(x, format = "%Y-%m-%d")

  for (k in 1:ncol(data.set)) {
    # What's below is a rather god-awful hack:
    # We confirmed that write.table CANNOT save both NA and NaN,
    # if both are present, unambiguously (i.e., as different string
    # tokens) in the text file. So we are going to convert all the 
    # numeric (double) vectors in the dataset to string vectors, 
    # and replace the special values - NA, NaN and Inf with the 
    # correct string tokens - "NA", "NaN" and "Inf", respectively. 
    # Note that the RData file will be reopened when the R ingest
    # reader creates the metadata describing the variables. So 
    # the numerics will still be registered as such. So when
    # the R ingest reader reads the tab file produced below, it 
    # will know to do post-processing on the numeric columns -
    # namely, to remove the double quotes around the values. 
    # As I said, this is an awful hack. But it appears to be
    # the most practical way to resolve this. -- L.A.
    if (!(is(data.set[, k], "POSIXt")) && !(is(data.set[, k], "Date")) && is.double(data.set[, k])) {
      strvec <- c()
      for (nc in 1:length(data.set[, k])) {
	if (is.infinite(data.set[nc,k])) {
	  if (data.set[nc,k] > 0) {
	    strvec <- c(strvec, "Inf") 
	  } else {
	    strvec <- c(strvec, "-Inf")
	  }
        } else if (is.nan(data.set[nc,k])) {
	  # the test for NaN must come *before* the test for NA!
	  # this is because is.na(NaN) actually returns true. 
	  strvec <- c(strvec, "NaN")
        } else if (is.na(data.set[nc,k])) {
          strvec <- c(strvec, "NA")
        } else {
	  strvec <- c(strvec, data.set[nc,k])
	}
      }
      data.set[, k] <- strvec
    }
  } 

  # Determine which columns need quotes:
  quotes <- needs.quotes(data.set)

  # Reformat each column
  for (k in 1:length(data.set)) {
    data.set[, k] <- reformat(data.set[, k])
  }

  # Write the table in the DVN's TAB file format

  if (length(quotes) > 0) {
    write.table(data.set, quote = quotes, sep = "\t", eol = "\n", na = "", dec = ".", row.names = FALSE, col.names = FALSE, qmethod = "double", ...)
  } else {
    # write.table bombs if given an empty vector as the quotes parameter! -- L.A. 
    write.table(data.set, sep = "\t", eol = "\n", na = "", dec = ".", row.names = FALSE, col.names = FALSE, qmethod = "double", ...)
  }

  # Restore options to previous
  options(saved.options)

  # Return NULL invisibly
  invisible(NULL)
}