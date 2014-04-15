##
##  preprocess.r
##
##  10/3/14
##


preprocess<-function(hostname=NULL, fileid=NULL, testdata=NULL, filename=NULL){
    
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
    
    
    k<-ncol(mydata)
    varnames<-names(mydata)
    
    hold<-list()

    count<-0
    
    for(i in 1:k){
        if(is.numeric(mydata[,i])){
            uniqueValues<-sort(na.omit(unique(mydata[,i])))
            
            if(length(uniqueValues)< histlimit){
                output<- table(mydata[,i])
                hold[[i]]<- list(type="bar", values=output)
            }else{
                output<- density( mydata[,i], n=50, na.rm=TRUE )
                hold[[i]]<- list(type="continuous", x=output$x, y=output$y)
                
            }
            
        }else{
            hold[[i]]<-list(type="character")
        }
    }
    names(hold)<-varnames
    jsonHold<-toJSON(hold)
    
    return(jsonHold)
}






