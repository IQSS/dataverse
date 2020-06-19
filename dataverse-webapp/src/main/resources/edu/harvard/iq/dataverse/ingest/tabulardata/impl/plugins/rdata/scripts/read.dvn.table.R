library(foreign)
library(stats)
library(methods)
#library(UNF)
#library(R2HTML)

############ parameters ########################

univarstathdr<-c("Valid Cases", "Missing Cases(NAs)", "Total", "Mean", "Standard deviation", "Skewness", "Kurtosis", "Coefficient of variation", "Mode", "Minimum","1st Quartile","Median","3rd Quartile","Maximum","Range","Interquartile Range","Normality Test(Shapiro-Wilk Statistic)", "Normality Test(Shapiro-Wilk Statistic: p value)")

imgprfx1<-c("<img src=\"http://")
imgprfx2<-c("/nph-dmpJpg.pl?jpgfn=")
imgsffx1<-c("\" >\n")
imgsffx2<-c("\" >\n")

###########################################################
read.dvn.table <- function (file, header = FALSE, sep = "\t", quote = "\"", dec = ".", col.names=NULL, na.strings = "NA",colClasses = NA,  colClassesx = NA, nrows = -1, skip = 0, check.names = TRUE,fill = !blank.lines.skip, strip.white = FALSE, blank.lines.skip = TRUE, comment.char = "", varFormat=list()) {
    if (is.character(file)) {
        file <- file(file, "r")
        on.exit(close(file))
    }
    if (!inherits(file, "connection")) stop("argument 'file' must be a character string or connection")
    if (!isOpen(file)) {
        open(file, "r")
        on.exit(close(file))
    }
    if (skip > 0) readLines(file, skip)

    cols<- length(colClassesx)

    if (is.null(col.names)) col.names<-paste("V", 1:cols, sep = "")
    if(check.names) col.names <- make.names(col.names, unique = TRUE)
    what <- rep(list(""), cols)
    names(what) <- col.names
    known <- colClasses %in% c("logical", "integer", "numeric", "complex", "character")
    what[known] <- sapply(colClasses[known], do.call, list(0))
    
    data <- scan(file = file, what = what, sep = sep, quote = quote, dec = dec, nmax = nrows, skip = 0, na.strings = na.strings, quiet = TRUE, fill = fill, strip.white = strip.white, blank.lines.skip = blank.lines.skip, multi.line = FALSE, comment.char = comment.char)
    
    nlines <- length(data[[1]])
    
    if (cols != length(data)) {
        warning(paste("cols =", cols, " != length(data) =", length(data)))
        cols <- length(data)
    }

    for (i in 1:cols) {
        #if (known[i]) next
        #data[[i]] <- as(data[[i]], colClasses[i])
        if (colClassesx[i] == 0) {
             if (is.null(unlist(varFormat[col.names[i]]))) {
                #cat("before-s=",i, "\n")
                data[[i]] <- as(data[[i]], "character")
                #cat("after-s=",i, "\n")
             }
             else if (!is.null(unlist(varFormat[col.names[i]]))){
                if (varFormat[col.names[i]] == 'D'){
                    #cat("before-d=",i, "\n")
                    data[[i]]<-as.Date(data[[i]])
                    #cat("after-d=",i, "\n")
                    colClassesx[i]<-1
                } else if (varFormat[col.names[i]] == 'T'){
                    data[[i]]<-as.POSIXct(data[[i]], "%H:%M:%OS %z")
                    colClassesx[i] <- 1
                } else if (varFormat[col.names[i]] == 'DT'){
                  data[[i]] <- as.POSIXct(data[[i]], format = "%F %H:%M:%OS %z")
                    colClassesx[i]<-1
                } else if (varFormat[col.names[i]] == 'JT'){
                    data[[i]]<-as.POSIXct(data[[i]], "%j %H:%M:%OS")
                    colClassesx[i]<-1
                }
             }
        }
        else {
            data[[i]] <- type.convert(data[[i]], dec = dec)
        }
    }

    class(data) <- "data.frame"
    row.names(data) <- as.character(seq(len = nlines))
    attr(data, "var.type")<-colClassesx
    data
} # end of read.table141vdc