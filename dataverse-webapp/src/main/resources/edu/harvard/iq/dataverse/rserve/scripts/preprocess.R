##
##  preprocess.R
##
##  May 29, 2015
##

library(rjson)
library(DescTools)


preprocess<-function(hostname=NULL, fileid=NULL, testdata=NULL, types=NULL, filename=NULL){
    
    histlimit<-13
    
    if(!is.null(testdata)){
        mydata<-testdata
    }else if(!is.null(filename)){
        mydata<-tryCatch(expr=read.delim(file=filename), error=function(e) NULL)
    }else{
        path<-paste("http://",hostname,"/api/access/datafile/",fileid,sep="")
        mydata<-tryCatch(expr=read.delim(file=path), error=function(e) NULL)
        #mydata<-getDataverse(hostname=hostname, fileid=fileid) #could use this function if we set up a common set of utilities with the rook code.
    }
    
    defaulttypes <- typeGuess(mydata)
    # Note: types can be passed directly to preprocess, as would be the case if a TwoRavens user tagged a variable as "nominal"
    if(is.null(types)) { # no types have been passed, so simply copying the defaults into the type fields
        types$numchar <- defaulttypes$defaultNumchar
        types$nature <- defaulttypes$defaultNature
        types$binary <- defaulttypes$defaultBinary
        types$interval <- defaulttypes$defaultInterval
        types <- c(types, defaulttypes)
    }else{ # types have been passed, so filling in the default type fields and accounting for the possibility that the types that have been passed are ordered differently than the default types
        for(i in 1:length(types$varnamesTypes)) {
            t <- which(defaulttypes$varnamesTypes==types$varnamesTypes[i])
            types$defaultNumchar[i] <- defaulttypes$defaultNumchar[t]
            types$defaultNature[i] <- defaulttypes$defaultNature[t]
            types$defaultBinary[i] <- defaulttypes$defaultBinary[t]
            types$defaultInterval[i] <- defaulttypes$defaultInterval[t]
        }
    }
    
    
    # calculating the summary statistics
    mySumStats <- calcSumStats(mydata, types)
    
    k<-ncol(mydata)
    varnames<-names(mydata)
    hold<-list()
    count<-0
    
    
    for(i in 1:k){
        nat <- types$nature[which(types$varnamesTypes==varnames[i])]
        if(nat!="nominal"){
            uniqueValues<-sort(na.omit(unique(mydata[,i])))
            
            if(length(uniqueValues)< histlimit){
                output<- table(mydata[,i])
                hold[[i]]<- list(plottype="bar", plotvalues=output)
            }else{
                output<- density( mydata[,i], n=50, na.rm=TRUE )
                hold[[i]]<- list(plottype="continuous", plotx=output$x, ploty=output$y)
                
            }
            
        }else{
            output<- table(mydata[,i])
            hold[[i]]<- list(plottype="bar", plotvalues=output)
        }
        hold[[i]] <- c(hold[[i]],lapply(mySumStats, `[[`,which(mySumStats$varnamesSumStat==varnames[i])),lapply(types, `[[`,which(types$varnamesTypes==varnames[i])))
    }
    names(hold)<-varnames
    
    datasetLevelInfo<-list(private=FALSE)    # This signifies that that the metadata summaries are not privacy protecting
    
    ## Construct Metadata file that at highest level has list of dataset-level, and variable-level information
    largehold<- list(dataset=datasetLevelInfo, variables=hold)
    
    jsonHold<-rjson:::toJSON(largehold)
    
    return(jsonHold)
}

## calcSumStats is a function that takes as input a dataset and the types for each variable, as returned by typeGuess()
calcSumStats <- function(data, types) {
    
    Mode <- function(x, nat) {
        out <- list(mode=NA, mid=NA, fewest=NA, freqmode=NA, freqfewest=NA, freqmid=NA)
        ux <- unique(x)
        tab <- tabulate(match(x, ux))
        
        out$mode <- ux[which.max(tab)]
        out$freqmode <- max(tab)
        
        out$mid <- ux[which(tab==median(tab))][1] # just take the first
        out$fewest <- ux[which.min(tab)]
        
        out$freqmid <- median(tab)
        out$freqfewest <- min(tab)
        
        return(out)
    }
    
    k <- ncol(data)
    out<-list(varnamesSumStat=colnames(data), median=as.vector(rep(NA,length.out=k)), mean=as.vector(rep(NA,length.out=k)), mode=as.vector(rep(NA,length.out=k)), max=as.vector(rep(NA,length.out=k)), min=as.vector(rep(NA,length.out=k)), invalid=as.vector(rep(NA,length.out=k)), valid=as.vector(rep(NA,length.out=k)), sd=as.vector(rep(NA,length.out=k)), uniques=as.vector(rep(NA,length.out=k)), herfindahl=as.vector(rep(NA,length.out=k)), freqmode=as.vector(rep(NA,length.out=k)), fewest=as.vector(rep(NA,length.out=k)), mid=as.vector(rep(NA,length.out=k)), freqfewest=as.vector(rep(NA,length.out=k)), freqmid=as.vector(rep(NA,length.out=k)) )
    
    for(i in 1:k) {
        
        v <- data[,i]
        nc <- types$numchar[which(types$varnamesTypes==out$varnamesSumStat[i])]
        nat <- types$nature[which(types$varnamesTypes==out$varnamesSumStat[i])]
        
        # this drops the factor
        v <- as.character(v)
        
        out$invalid[i] <- length(which(is.na(v)))
        out$valid[i] <- length(v)-out$invalid[i]
        
        v[v=="" | v=="NULL" | v=="NA" | v=="."]  <- NA
        v <- v[!is.na(v)]
        
        tabs <- Mode(v, nat)
        out$mode[i] <- tabs$mode
        out$freqmode[i] <- tabs$freqmode
        
        out$uniques[i] <- length(unique(v))
        
        if(nc=="character") {
            out$fewest[i] <- tabs$fewest
            out$mid[i] <- tabs$mid
            out$freqfewest[i] <- tabs$freqfewest
            out$freqmid[i] <- tabs$freqmid
            
            herf.t <- table(v)
            out$herfindahl[i] <- Herfindahl(herf.t)
            
            out$median[i] <- "NA"
            out$mean[i] <- "NA"
            out$max[i] <- "NA"
            out$min[i] <- "NA"
            out$sd[i] <- "NA"
            
            next
        }
        
        # if not a character
        v <- as.numeric(v)
        
        out$median[i] <- median(v)
        out$mean[i] <- mean(v)
        out$max[i] <- max(v)
        out$min[i] <- min(v)
        out$sd[i] <- sd(v)
        
        out$mode[i] <- as.character(signif(as.numeric(out$mode[i]), 4))
        out$fewest[i] <- as.character(signif(as.numeric(tabs$fewest,4)))
        out$mid[i] <- as.character(signif(as.numeric(tabs$mid,4)))
        out$freqfewest[i] <- as.character(signif(as.numeric(tabs$freqfewest,4)))
        out$freqmid[i] <- as.character(signif(as.numeric(tabs$freqmid,4)))
        
        herf.t <- table(v)
        out$herfindahl[i] <- Herfindahl(herf.t)
    }
    return(out)
}


## typeGuess() is a function that takes as input a dataset and returns our best guesses at types of variables. numchar is {"numeric" , "character"}, interval is {"continuous" , "discrete"}, nature is {"nominal" , "ordinal" , "interval" , "ratio" , "percent" , "other"}. binary is {"yes" , "no"}. if numchar is "character", then by default interval is "discrete" and nature is "nominal".
typeGuess <- function(data) {
    
    k <- ncol(data)
    out<-list(varnamesTypes=colnames(data), defaultInterval=as.vector(rep(NA,length.out=k)), defaultNumchar=as.vector(rep(NA,length.out=k)), defaultNature=as.vector(rep(NA,length.out=k)), defaultBinary=as.vector(rep("no",length.out=k)))
    
    numchar.values <- c("numeric", "character")
    interval.values <- c("continuous", "discrete")
    nature.values <- c("nominal", "ordinal", "interval", "ratio", "percent", "other")
    binary.values <- c("yes", "no")
    
    
    Decimal <-function(x){
        result <- FALSE
        level <- floor(x)
        if(any(x!=level)) result <- TRUE
        
        return(result)
    }
    
    # Nature() takes a column of data x, and a boolean c that is true if x is continuous, and a vector nat that is the values of nature and returns a guess at the nature field
    Nature <- function(x, c, nat) {
        if(c) { # interval is continuous
            if(all(x >=0 & x <=1)) {
                return(nat[5])
            }
            else if(all(x >=0 & x <=100) & min(x) < 15 & max(x) > 85){
                return(nat[5])
            } else {
                return(nat[4]) # ratio is generally the world we're going to be in
            }
        } else { # interval is discrete
            return(nat[2]) # if it is a continuous, discrete number, assume ordinal
        }
    }
    
    
    for(i in 1:k){
        
        v<- data[,i]
        
        # if variable is a factor or logical, return character
        if(is.factor(v) | is.logical(v)) {
            out$defaultInterval[i] <- interval.values[2]
            out$defaultNumchar[i] <- numchar.values[2]
            out$defaultNature[i] <- nature.values[1]
            
            v <- as.character(v)
            v[v=="" | v=="NULL" | v=="NA"]  <- NA
            v <- v[!is.na(v)]
            
            if(length(unique(v))==2) {out$defaultBinary[i] <- binary.values[1]}
            next
        }
        
        v <- as.character(v)
        v[v=="" | v=="NULL" | v=="NA"]  <- NA
        v <- v[!is.na(v)]
        
        # converts to numeric and if any do not convert and become NA, numchar is character
        v <- as.numeric(v)
        
        if(length(unique(v))==2) {out$defaultBinary[i] <- binary.values[1]} # if there are only two unique values after dropping missing, set binary to "yes"
        
        if(any(is.na(v))) { # numchar is character
            out$defaultNumchar[i] <- numchar.values[2]
            out$defaultNature[i] <- nature.values[1]
            out$defaultInterval[i] <- interval.values[2]
        } else { # numchar is numeric
            out$defaultNumchar[i] <- numchar.values[1]
            
            d <- Decimal(v)
            if(d) { # interval is continuous
                out$defaultInterval[i] <- interval.values[1]
                out$defaultNature[i] <- Nature(v,TRUE, nature.values)
            } else { # interval is discrete
                out$defaultInterval[i] <- interval.values[2]
                out$defaultNature[i] <- Nature(v,FALSE, nature.values)
            }
        }
    }
    
    return(out)
}






