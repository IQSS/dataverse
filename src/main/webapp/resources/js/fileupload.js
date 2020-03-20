var fileList = [];
var observer2=null;
var datasetId=null;
//How many files have started being processed but aren't yet being uploaded
var filesInProgress=0;
//The # of the current file being processed (total number of files for which upload has at least started)
var curFile=0;
//The number of upload ids that have been assigned in the files table
var fileUpId=0;
//How many files are completely done
var filesFinished=0;

function setupDirectUpload(enabled, theDatasetId) {
  if(enabled) {
    datasetId=theDatasetId;
    $('.ui-fileupload-upload').hide();
    $('.ui-fileupload-cancel').hide();
    //Catch files entered via upload dialog box. Since this 'select' widget is replaced by PF, we need to add a listener again when it is replaced
    var fileInput=document.getElementById('datasetForm:fileUpload_input');
    fileInput.addEventListener('change', function(event) {
        fileList=[];
        for(var i=0;i<fileInput.files.length;i++) {
          queueFileForDirectUpload(fileInput.files[i], datasetId);
        }
      }, {once:false});
    //Add support for drag and drop. Since the fileUploadForm is not replaced by PF, catching changes with a mutationobserver isn't needed  
    var fileDropWidget=document.getElementById('datasetForm:fileUpload');
    fileDropWidget.addEventListener('drop', function(event) {
	        fileList=[];
        for(var i=0;i<event.dataTransfer.files.length;i++) {
          queueFileForDirectUpload(event.dataTransfer.files[i], datasetId);
        }
      }, {once:false});

    var config={childList: true};
    var callback = function(mutations) {
      mutations.forEach(function(mutation) {
        for(i=0; i<mutation.addedNodes.length;i++) {
          //Add a listener on any replacedment file 'select' widget
          if(mutation.addedNodes[i].id == 'datasetForm:fileUpload_input') {
            fileInput=mutation.addedNodes[i];
            mutation.addedNodes[i].addEventListener('change', function(event) {
              for(var j=0;j<mutation.addedNodes[i].files.length;j++) {
                queueFileForDirectUpload(mutation.addedNodes[i].files[j], datasetId);
              }
            }, {once:false});
          }
        }
      });
    };
    if(observer2 !=null) {
      observer2.disconnect();
    }
    observer2 = new MutationObserver(callback);
    observer2.observe(document.getElementById('datasetForm:fileUpload'),config);
  } //else ?
}

function queueFileForDirectUpload(file, datasetId) {
  if(fileList.length === 0) {uploadWidgetDropRemoveMsg();}
  fileList.push(file);
  //Fire off the first 4 to start (0,1,2,3)
  if(filesInProgress < 4 ) {
    filesInProgress= filesInProgress+1;
    requestDirectUploadUrl();
  }
}

function uploadFileDirectly(url, storageId) {
  //Pick the 'first-in' pending file 
  var file = fileList.shift();
  //This appears to be the earliest point when the file table has been populated, and, since we don't know how many table entries have had ids added already, we check
  var filerows =  $('.ui-fileupload-files .ui-fileupload-row');
  //Add an id attribute to each entry so we can later match progress and errors with the right entry
  for(i=0;i< filerows.length;i++) {
    var upid=filerows[i].getAttribute('upid');
    if(typeof upid === "undefined" || upid === null || upid === '') {
      filerows[i].setAttribute('upid', fileUpId);
      fileUpId = fileUpId+1;
    }
  }
  //Get the list of files to upload
  var files =  $('.ui-fileupload-files');
  //Find the corresponding row (assumes that the file order and the order of rows is the same)
  var fileNode = files.find("[upid='"+curFile+"']");
  //Increment count of files being processed
  curFile=curFile+1;
  //Decrement number queued for processing
  filesInProgress=filesInProgress-1;
  var progBar = fileNode.find('.ui-fileupload-progress');
  progBar.html('');
  progBar.append($('<progress/>').attr('class', 'ui-progressbar ui-widget ui-widget-content ui-corner-all'));
  $.ajax({
    url: url,
    headers: {"x-amz-tagging":"dv-state=temp"},
    type: 'PUT',
    data: file,
    cache: false,
    processData: false,
    success: function () {
    reportUpload(storageId, file)
    },
    error: function(jqXHR, textStatus, errorThrown) {
      uploadFailure();
    },
    xhr: function() {
      var myXhr = $.ajaxSettings.xhr();
      if(myXhr.upload) {
        myXhr.upload.addEventListener('progress', function(e) {
          if(e.lengthComputable) {
            var doublelength = 2 * e.total;
            progBar.children('progress').attr({
              value:e.loaded,
              max:doublelength
            });
          }
        });
      }
      return myXhr;
      }
  });
}

function reportUpload(storageId, file){

  getMD5(
    file,
    prog => {console.log("Progress: " + prog);

    var current = 1 + prog;
    $('progress').attr({
              value:current,
              max:2
            });
    }
  ).then(
    md5 => {
      //storageId is not the location - has a : separator and no path elements from dataset
      //(String uploadComponentId, String fullStorageIdentifier, String fileName, String contentType, String checksumType, String checksumValue)
      handleExternalUpload([{name:'uploadComponentId', value:'datasetForm:fileUpload'}, {name:'fullStorageIdentifier', value:storageId}, {name:'fileName', value:file.name}, {name:'contentType', value:file.type}, {name:'checksumType', value:'MD5'}, {name:'checksumValue', value:md5}]);
    },
    err => console.error(err)
  );
}


function removeErrors() {
  var errors = document.getElementsByClassName("ui-fileupload-error");
  for(i=errors.length-1; i >=0; i--) {
    errors[i].parentNode.removeChild(errors[i]);
  }
}

var observer=null;

function uploadStarted() {
  // If this is not the first upload, remove error messages since
  // the upload of any files that failed will be tried again.
  removeErrors();
  var curId=0;
  //Find the upload table body
  var files =  $('.ui-fileupload-files .ui-fileupload-row');
  //Add an id attribute to each entry so we can later match errors with the right entry
  for(i=0;i< files.length;i++) {
    files[i].setAttribute('upid', curId);
    curId = curId+1;
  }
  //Setup an observer to watch for additional rows being added
  var config={childList: true};
  var callback = function(mutations) {
    //Add an id attribute to all new entries
    mutations.forEach(function(mutation) {
      for(i=0; i<mutation.addedNodes.length;i++) {
        mutation.addedNodes[i].setAttribute('upid',curId);
        curId=curId+1;
      }
      //Remove existing error messages since adding a new entry appears to cause a retry on previous entries
      removeErrors();
    });
  };
  //uploadStarted appears to be called only once, but, if not, we should stop any current observer
  if(observer !=null) {
    observer.disconnect();
  }
  observer = new MutationObserver(callback);
  observer.observe(files[0].parentElement,config);
}

function uploadFinished(fileupload) {
    if (fileupload.files.length === 0) {
        $('button[id$="AllUploadsFinished"]').trigger('click');
        //stop observer when we're done
        if(observer !=null) {
          observer.disconnect();
          observer=null;
        }
    }
}

function directUploadFinished() {
    filesFinished = filesFinished+1;
    if (fileList.length === 0) {
      if(curFile === filesFinished) {
        $('button[id$="AllUploadsFinished"]').trigger('click');
        //stop observer when we're done
        if(observer !=null) {
          observer.disconnect();
          observer=null;
        }
      }
    }  else {
      if((filesInProgress < 4) && (filesInProgress < fileList.length)) {
        filesInProgress= filesInProgress+1;
        requestDirectUploadUrl();
      }
    }
}

function uploadFailure(fileUpload) {
	// This handles HTTP errors (non-20x reponses) such as 0 (no connection at all), 413 (Request too large),
	// and 504 (Gateway timeout) where the upload call to the server fails (the server doesn't receive the request)
	// It notifies the user and provides info about the error (status, statusText)
	// On some browsers, the status is available in an event: window.event.srcElement.status
	// but others, (Firefox) don't support this. The calls below retrieve the status and other info 
	// from the call stack instead (arguments to the fail() method that calls onerror() that calls this function
	
	//Retrieve the error number (status) and related explanation (statusText)
	var status = arguments.callee.caller.caller.arguments[1].jqXHR.status;
    var statusText = arguments.callee.caller.caller.arguments[1].jqXHR.statusText;
    //statusText for error 0 is the unhelpful 'error'
    if(status == 0) statusText='Network Error';
    
    // There are various metadata available about which file the error pertains to
    // including the name and size. 
    // However, since the table rows created by PrimeFaces only show name and approximate size, 
    // these may not uniquely identify the affected file. Therefore, we set a unique upid attribute
    // in uploadStarted (and the MutationObserver there) and look for that here. The files array has
    // only one element and that element includes a description of the row involved, including it's upid.
    
    var name = arguments.callee.caller.caller.arguments[1].files[0].name;
	var id = arguments.callee.caller.caller.arguments[1].files[0].row[0].attributes.upid.value;
	//Log the error 
	console.log('Upload error:' + name + ' upid=' + id + ', Error ' + status + ': ' + statusText );
    //Find the table
    var rows  =  $('.ui-fileupload-files .ui-fileupload-row');
    //Create an error element
    var node = document.createElement("TD");
    //Add a class to make finding these errors easy
    node.classList.add('ui-fileupload-error');
    //Add the standard error message class for formatting purposes
    node.classList.add('ui-message-error');
    var textnode = document.createTextNode("Upload unsuccessful (" + status + ": " + statusText + ").");
    node.appendChild(textnode);
    //Add the error message to the correct row
    for(i=0;i<rows.length;i++) {
       	if(rows[i].getAttribute('upid') == id) {
       		//Remove any existing error message/only show last error (have seen two error 0 from one network disconnect)
       		var err = rows[i].getElementsByClassName('ui-fileupload-error');
       		if(err.length != 0) {
       			err[0].remove();
       		}
         	rows[i].appendChild(node);
         	break;
       	}
    }
}

//MD5 Hashing functions

function readChunked(file, chunkCallback, endCallback) {
  var fileSize   = file.size;
  var chunkSize  = 64 * 1024 * 1024; // 64MB
  var offset     = 0;
  
  var reader = new FileReader();
  reader.onload = function() {
    if (reader.error) {
      endCallback(reader.error || {});
      return;
    }
    offset += reader.result.length;
    // callback for handling read chunk
    // TODO: handle errors
    chunkCallback(reader.result, offset, fileSize); 
    if (offset >= fileSize) {
      endCallback(null);
      return;
    }
    readNext();
  };

  reader.onerror = function(err) {
    endCallback(err || {});
  };

  function readNext() {
    var fileSlice = file.slice(offset, offset + chunkSize);
    reader.readAsBinaryString(fileSlice);
  }
  readNext();
}

function getMD5(blob, cbProgress) {
  return new Promise((resolve, reject) => {
    var md5 = CryptoJS.algo.MD5.create();
    readChunked(blob, (chunk, offs, total) => {
      md5.update(CryptoJS.enc.Latin1.parse(chunk));
      if (cbProgress) {
        cbProgress(offs / total);
      }
    }, err => {
      if (err) {
        reject(err);
      } else {
        // TODO: Handle errors
        var hash = md5.finalize();
        var hashHex = hash.toString(CryptoJS.enc.Hex);
        resolve(hashHex);
      }
    });
  });
}


