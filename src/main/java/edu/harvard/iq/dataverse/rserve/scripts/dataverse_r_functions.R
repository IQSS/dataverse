library(foreign)
library(stats)
library(methods)
library(R2HTML)

options(digits.secs = 3)


############ parameters ########################
univarstathdr<-c("Valid Cases", "Missing Cases(NAs)", "Total", "Mean", "Standard deviation", "Skewness", "Kurtosis", "Coefficient of variation", "Mode", "Minimum","1st Quartile","Median","3rd Quartile","Maximum","Range","Interquartile Range","Normality Test(Shapiro-Wilk Statistic)", "Normality Test(Shapiro-Wilk Statistic: p value)")

imgprfx1<-c("<img src=\"http://")
imgprfx2<-c("/nph-dmpJpg.pl?jpgfn=")
imgsffx1<-c("\" >\n")
imgsffx2<-c("\" >\n")

############# parameters #######################
# Note: 
#  - The parameter na.strings is set to "NA", even though in the DVN tab files Missing Values are encoded as empty strings; 
#    this may be some sort of a legacy thing (may be older files still had "NA"s in them as this was written?). After calling
#    this read.table function, the DVN application seems to always make another call to reset all the empties to NA. 
#    Some functions further down in this file also do that explicitly. 
#  - I changed the strip.white parameter to FALSE (-- L.A., 05/07/2013); having it set to TRUE was resulting in the dropping 
#    the empty entries that were supposed to represent Missing Values, when the subset contained a single numeric column, 
#    no matter what the na.strings= was set to. 

read.dataverseTabData<-function (file, header = FALSE, sep = "\t", quote = "", dec = ".", col.names=NULL, na.strings = "NA",colClasses = NA,  colClassesx = NA, nrows = -1, skip = 0, check.names = TRUE,fill = !blank.lines.skip, strip.white = FALSE, blank.lines.skip = FALSE, comment.char = "", varFormat=list()) 
{
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

    saved.options <- options(digits.secs = 3)

    for (i in 1:cols) {
        if (colClassesx[i] == 0) {

	     # Make sure the character values are handled as such:
	     data[[i]]<-as.character(data[[i]]);
	     # And replace empty strings with NAs:
	     data[[i]][ data[[i]] == '' ]<-NA
	     # And remove the double quotes we had put around the non-missing
 	     # string values as they were stored in the TAB files:

	     data[[i]]<-sub("^\"", "", data[[i]])
	     data[[i]]<-sub("\"$", "", data[[i]])

	     # Special processing for dates and times:
            
             if (is.null(unlist(varFormat[col.names[i]]))){
                data[[i]] <- as(data[[i]], "character")
             } else if (!is.null(unlist(varFormat[col.names[i]]))){
                if (varFormat[col.names[i]] == 'D'){
		    data[[i]]<-as.Date(data[[i]]);
                    colClassesx[i]<-1
                } else if (varFormat[col.names[i]] == 'T'){
                    data[[i]]<-as.POSIXct(strptime(data[[i]], "%T"))
                    colClassesx[i]<-1
                } else if (varFormat[col.names[i]] == 'DT'){
                    data[[i]]<-as.POSIXct(strptime(data[[i]], "%F %H:%M:%OS"))
                    colClassesx[i]<-1
                } else if (varFormat[col.names[i]] == 'JT'){
                    data[[i]]<-as.POSIXct(strptime(data[[i]], "%j %H:%M:%OS"))
                    colClassesx[i]<-1
                }
             }
        } else if (colClassesx[i] == 3) {

	     # special case for Boolean/logical variables: 
	     # (these will be passed from the application as vectors of 0s and 1s)
	     # also, note that this type will be used only when the subset is 
	     # created as part of the "save-as" functionality. When it's for 
	     # analysis, the DVN "boolean" variable will be of type 1, because 
	     # they will be handled as regular integer categoricals with the labels 
	     # "TRUE" and "FALSE". -- L.A. 

	     for (j in 1:length(data[[i]])) {
	     	 if (!is.na(data[[i]][j]) && data[[i]][j] == "") { 
	       	    data[[i]][j]<-NA 
	    	 }
	     }

	     data[[i]]<-as.logical(as.numeric(data[[i]]))

        } else {
	    # Numeric values: 
            data[[i]]<-type.convert(data[[i]], dec = dec)
        }
    }

    options(saved.options)

    class(data) <- "data.frame"
    row.names(data) <- as.character(seq(len = nlines))
    attr(data, "var.type")<-colClassesx
    data
} # end of read.dataverseTabData


###########################################################
createvalindex <-function(dtfrm, attrname=NULL){

    if (is.null(dtfrm)) {
        stop("dataframe is not specified\n")
    } else if (is.null(attrname)){
        stop("attrname is is not specified\n")
    } else if (!exists('dtfrm')) {
        stop("dataframe is not found\n")
    } else if (!is.data.frame(dtfrm) ) {
        stop("Specified object is not a data.frame\n")
    }
        
    #DBG<-TRUE
    DBG<-FALSE
    try ( {
    if (attrname == 'val.index') {
        tabletype<-'val.table'
        valtable<-attr(dtfrm, 'val.table')
    } else if (attrname == 'missval.index') {
        tabletype<-'missval.table'
        valtable<-attr(dtfrm, 'missval.table')
    } else stop ("Specified attrname must be either val.index or missval.index\n")
    
    if (DBG) {cat("\nattribute name=",attrname,"\n")}

    if (length(valtable)) {
        vlindex  <- list();
        vlst  <- list();
        lstall<-list()
        vltbl<-list()
        if (DBG) {
            cat("length(",attrname,")=",length(valtable),"\n")
            cat("varidset(",attrname,")=",names(valtable),"\n")
        }
        nameset<-names(valtable)
        if (DBG) {
            str(nameset)
            cat("\nnameset:", paste(nameset,collapse="|"), "\n",sep="")
        }
        for (i in 1:(length(valtable))){
        if (DBG) {
            cat("var=",i,"\n", sep="")
            cat("\tlstall:", paste(if (length(lstall)) {as.vector(lstall,mode="integer")} else {"empty"}, collapse=","), "\n",sep="")
        }
            nameseti<-nameset[i]
            if (!is.null(lstall[[as.character(i)]])){next}
            lsti<-list()

            # set i to the new list
            lsti[[as.character(i)]]<-i
            lstall[[as.character(i)]]<-i
            vlindex[[as.character(nameseti)]]<-nameset[i]
            vltbl[[as.character(nameseti)]]<-valtable[[i]]

            if (DBG) {cat("\tlsti:", paste(as.vector(lsti, mode="integer"),collapse=","), "\n",sep="")}
            for (j in i:length(valtable)){
                if (!is.null(lstall[[as.character(j)]])){next}
                if (attrname == 'val.index') {
                    if (  identical( names(valtable[[i]]), names(valtable[[j]])  ) & identical(valtable[[i]], valtable[[j]]) ) {
                        if (DBG) {cat("\tVL:new duplicate (var#) to be added:", j,"\n",sep="")}
                        lsti[[as.character(j)]]<-j
                        vlindex[[as.character(nameset[j])]]<-nameseti
                        lstall[[as.character(j)]]<-j
                    }
                } else if (attrname == 'missval.index') {
                    if ( identical(valtable[[i]], valtable[[j]]) ) {
                        if (DBG) {cat("\tMSVL: new duplicate (var#) to be added:", j,"\n",sep="")}
                        lsti[[as.character(j)]]<-j
                        vlindex[[as.character(nameset[j])]]<-nameseti
                        lstall[[as.character(j)]]<-j
                    }
                }
            }
            if (DBG) {cat("\tlsti to be attached to vlst:", paste(as.vector(lsti, mode="integer"),collapse=","), "\n",sep="")}
            if (length(lsti)){
                vlst[[nameseti]]<-nameset[as.vector(lsti, mode="integer")]
            }
        }
        if (DBG) {
            cat("\nvlst=attr(dtfrm,'val.list')  <- vlst\n")
            str(vlst)
            cat("\nvlindex=attr(dtfrm,'val.index') <- vlindex\n")
            str(vlindex)
            cat("\nvltbl=attr(dtfrm,'val.table')<- valtablex\n")
            str(vltbl)
            cat("\nnames(vltbl): equivalent to tmpunique\n")
            cat("unique var IDs:", paste(names(vltbl),collapse="|"), "\n",sep="")
        }
        attr(dtfrm, attrname)<-vlindex

        if (attrname == 'val.index') {
            attr(dtfrm, 'val.list')  <- vlst
            attr(dtfrm, 'val.table') <- vltbl
        } else if (attrname == 'missval.index') {
            attr(dtfrm, 'missval.list')  <- vlst
            attr(dtfrm, 'missval.table')<-vltbl
        }
            
    } else {
            # no value labels
            #vlindex<-rep(NA, dim(dtfrm)[2])
            attr(dtfrm, attrname)<-NULL
            if (attrname == 'val.index') {
                attr(dtfrm, 'val.list')<- NA 
            } else if (attrname == 'missval.index') {
                attr(dtfrm, 'missval.list')  <- NA
            }
    }
        
    invisible(dtfrm)
    }) # end try    
} # end of createvalindex

###########################################################
createDataverseDataFrame<-function(dtfrm, dwnldoptn, dsnprfx) {
    # dtfrm(=z1)        dataset to be downloaded
    # dwnldoptn(=z2)    data download option
    # dsnprfx(=z3)      dataset name prefix

# The portion of code immediately below has been added to 
# convert extra metadata, such as value labels, supplied in
# the proprietary attributes (below) into standard R notations,
# such as "comments" for variable labels and "factors" for 
# value labels. 
# 
# This is still work in progress! -- L.A. 

    NAMESET<-names(dtfrm)
    VARLABELS<-attr(dtfrm,"var.labels")

    attr(x,"orig.names")<-attr(dtfrm,"var.labels")

    CHRTLST<-attr(dtfrm, "univarChart.lst")
    STATLST<-attr(dtfrm, "univarStat.lst")
    VARTYPE<-attr(dtfrm, "var.type")
    VALINDEX<-attr(dtfrm, "val.index")
    VALTABLE<-attr(dtfrm, "val.table")

    MISSVALINDEX <- attr(x,"missval.index")
    MISSVALTABLE <- attr(x,"missval.table")

    for (i in 1:length(x)) {
        # cat("inside the for loop\n")
	# cat("class: ")
	# cat(class(x[[i]]))
	# cat("\n")
	# Recoding discrete, categorical variables as R factors;
	# But, (experimental...) only if there are value labels supplied. 
	# This means, among other things, that an ingested R character, 
	# or integer vector would stay a vector, and not a factor, 
	# in a saved-as-R subset.  


	# -- L.A.
	
	if (!is.null(VARTYPE) && VARTYPE[i]<2) {

	if (!(is.null(VALINDEX[[as.character(i)]]))) {

            vti <- VALTABLE[[VALINDEX[[as.character(i)]]]]
	    # cat(paste(class(vti),"\n"))
	    # cat(paste(length(vti),"\n"))
	    # cat(paste("VTI", vti, "\n", sep=" : "))

	  if (is.numeric(x[[i]])) {
	     vtilevels<-as.numeric(names(vti))
          } else {
	    vtilevels<-names(vti) 
          }

          # save / re-attach date/time-related class name
	  classToken <- class(x[[i]])
          vlevsi <- as.list(sort(unique.default(c(x[[i]],vtilevels))))
          if ((classToken[1] == "Date") || (classToken[1] == "POSIXt")) {
	      class(vlevsi)<- classToken
	  }

	  names(vlevsi)<-vlevsi
          tmatch<-na.omit(match(names(vti),names(vlevsi)))
          if (length(tmatch)>0) {
	      names(vlevsi)[tmatch] <- vti
	  }

	  mti<-integer(0);
	  mti<-integer(0);
	  if (!is.null(MISSVALINDEX[[as.character(i)]])) {
	    mti<-MISSVALTABLE[[MISSVALINDEX[[as.character(i)]]]]
	    tmatch<-na.omit(match(mti,vlevsi))
	    if (length(tmatch)>0) {
	      vlevsi[tmatch]<-NULL
	    }
	  }

	if (!(is.null(VALORDER[[as.character(i)]]))) {
	   # cat("ordered value labels supplied")
		x[[i]]  <-  factor(x[[i]],
				levels=VALORDER[[as.character(i)]],
				ordered=TRUE)
	} else {
	  # cat("no ordered value labels supplied\n")
	  # cat(paste(VARTYPE[i],"\n",sep=""))
	  # cat(paste(length(vlevsi),"\n",sep=""))
	  # orderedfct<-(VARTYPE[i]>0 && ((length(vlevsi)-length(mti)>2)))
          # cat(paste(as.character(orderedfct),"\n", sep=""))
	  # paste("MTI", mti,"\n",sep=" : ")
	  # paste("VLEVSI", vlevsi,"\n",sep=" : ")
	  
		x[[i]]  <-  factor(x[[i]],
	        		levels=vlevsi,
			     	labels=names(vlevsi),
			     	ordered=(VARTYPE[i]>0 && ((length(vlevsi)-length(mti)>2))))
	}

	attr(x,"vlevsi")<-vlevsi;
	attr(x,"namesvlevsi")<-names(vlevsi); 

	}
	}

	# try to add variable labels as R comments: (L.A. -- ?)

	comment(x[[i]]) <- VARLABELS[i]
      }

# end of added recoding -- L.A.
	
# SAVE AS R WORKSPACE: (L.A.)
        save(x,file=dsnprfx)
} # end of createDataverseDataFrame

