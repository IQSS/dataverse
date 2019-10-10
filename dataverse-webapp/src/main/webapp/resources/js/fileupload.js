
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
