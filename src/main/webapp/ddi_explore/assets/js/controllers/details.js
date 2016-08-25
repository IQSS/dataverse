'use strict';
angular.module('odesiApp').controller('detailsCtrl', function($scope,$cookies, $http, $modal, $location, searchParams,filterService, variableQuery,variableClick,sharedVariableStore){	
	$scope.showPaging =true;
	$scope.chartTemplatePath = 'templates/chart.html';
	var detailsURL = $location.search();
	$scope.detailsURL = detailsURL;
	$scope.currentTablePage = [];//stored with each survey file
	$scope.tablePageSize = 10;
	$scope.loadingDetails = true;
	$scope.showVariables = true;//original default was false
	$scope.active = {abstract: true}; 
	$scope._variables;
	$scope.variableClick = variableClick
	$scope.variableQuery = variableQuery;
	$cookies.variableCompare = "";
	$scope.searchParams = searchParams;	
	$scope._variableData;
	$scope._filtered_num=0;
	$scope.sortReverse=true; 
	$scope.has_no_selection=true;
	$scope.sortReverse=true; 
	//
	$scope.citation="";
	//
	if($scope.variableClick.params == true) {
		$scope.active = {matches: true};
	} else {
		$scope.active = {abstract: true};
	};
	var populateVariables = function() {
		//get a piece of the citatin for display
		var citation_pieces=$scope.details.stdydscr.citation.biblcit["#text"].split(",")
		$scope.citation=citation_pieces[0]+", "+citation_pieces[1]+", "+citation_pieces[2]
		//create a reference to a specific link for dataverse
		$scope.surveyVariables = [];
		if ($scope.details.datadscr ){
			var counter=0
			for (var i = 0; i < $scope.details.datadscr['var'].length; i++){
				counter++
				//join the variable data
				$scope.details.datadscr['var'][i].variable_data=$scope._variableData[$scope.details.datadscr['var'][i].name]
				
				var chartable=false;
				if(typeof($scope.details.datadscr['var'][i].variable_data.plotvalues)!="undefined" && typeof($scope.details.datadscr['var'][i].catgry)!="undefined"){
						chartable=true
					}
				
				//if ($scope.details.datadscr['var'][i].labl){
					
					//exception for dataverse
					var labl=""
					if ($scope.details.datadscr['var'][i].labl && $scope.details.datadscr['var'][i].labl["#text"]) {
						labl= $scope.details.datadscr['var'][i].labl["#text"]				
					}else if($scope.details.datadscr['var'][i].labl) {
						labl= $scope.details.datadscr['var'][i].labl				
					} 
					
					$scope.details.datadscr['var'][i].labl=labl;
					$scope.surveyVariables.push({
						id : parseFloat($scope.details.datadscr['var'][i].id.substring(1)),
						vid : $scope.details.datadscr['var'][i].id,
						label : labl,
						chartable : chartable,
						name : $scope.details.datadscr['var'][i].name,							
						fullData : $scope.details.datadscr['var'][i]
					});				
					//If a location element exists - need to catagorize the questions by files
					if(typeof($scope.details.datadscr['var'][i].location)!="undefined" && $scope.details.datadscr['var'][i].location.fileid){
						//get the file description 
						var file_id=$scope.details.datadscr['var'][i].location.fileid;
						
						var uri,file_note;
						//create an object array for the files (if it doesn't alreay exist)
						if(typeof($scope.survey_files)=="undefined"){
							$scope.survey_files=[]
						}
						//check if the file is in the array
						var file_array_pos=-1;
						for( var j = 0; j <$scope.survey_files.length; j++ ) {
							//console.log("files", $scope.survey_files[j],file_id)
							if( $scope.survey_files[j][0] === file_id ) {
								file_array_pos=j;
								break;
							}
						}
						//console.log($scope.survey_files)
						//if the file is not in the location array
						if(file_array_pos==-1)	{
							//console.log($scope.details.filedscr)
							for(var j=0;j<$scope.details.filedscr.length;j++){
								//console.log(file_id,$scope.details.filedscr[j].id)
								if(file_id==$scope.details.filedscr[j].id){
									var file_obj=$scope.details.filedscr[j]
									//take the new url for better linking - note this simply downloads the data in the case of dataverse
									uri=file_obj.URI[0]
									//console.log(file_obj)
									//console.log(file_obj)
									//create a new variable "vl.file_note" for file title display
									for(var k=0;k<file_obj.notes.length;k++){
										if(file_obj.notes[k].type=="vdc:category"){
											file_note=file_obj.notes[k]._
											break;
										}
									}
									break
								}
							}
							$scope.survey_files.push([file_id,{uri:uri,file_note:file_note,file_num:$scope.survey_files.length},[]])
							file_array_pos=$scope.survey_files.length-1
							$scope.currentTablePage.push(0);//all individual file page managmented
						}
						//store the survey item within the surveys files array
						var var_obj=$scope.surveyVariables[$scope.surveyVariables.length-1]
						//console.log($scope.surveyVariables.length,var_obj)
						$scope.survey_files[file_array_pos][2].push(var_obj);
						//keep a reference to the parent 
						var_obj.file_obj=$scope.survey_files[$scope.survey_files.length-1]
						//
						if(typeof($scope.filterBy)!="undefined"){
							if(typeof($scope.survey_files_filtered)=="undefined"){
								$scope.survey_files_filtered=[];//create container if it doesn't already exist
							}
							if($scope.filterBy.indexOf(var_obj.id)>-1){
								//make sure there is a file object container
								var file_array_pos_filtered=-1
								for(var j=0;j<$scope.survey_files_filtered.length;j++){
									if(file_id==$scope.survey_files_filtered[j][0]){
										file_array_pos_filtered=j;
										break;
									}
								}
								if(file_array_pos_filtered==-1){
									//create the container
									$scope.survey_files_filtered.push([file_id,{uri:uri,file_note:file_note,file_num:$scope.survey_files.length},[]])
									file_array_pos_filtered=$scope.survey_files_filtered.length-1
								}
								$scope.survey_files_filtered[file_array_pos_filtered][2].push(var_obj);
							}
						}
					//}
					//
					var index = $scope.surveyVariables.length - 1;
					//since DLIMF does not have a sumstat - check if it exists first before looping
					if(typeof($scope.details.datadscr['var'][i].sumstat) !="undefined"){
						for (var j = 0; j < $scope.details.datadscr['var'][i].sumstat.length; j++){
							if (!$scope.details.datadscr['var'][i].sumstat[j].wgtd){
								if ($scope.details.datadscr['var'][i].sumstat[j].type == 'vald'){
									$scope.surveyVariables[index].valid = Math.round($scope.details.datadscr['var'][i].sumstat[j]["#text"]);
								}else if ($scope.details.datadscr['var'][i].sumstat[j].type == 'invd'){
									$scope.surveyVariables[index].missing = Math.round($scope.details.datadscr['var'][i].sumstat[j]["#text"]);
								}else if ($scope.details.datadscr['var'][i].sumstat[j].type == 'min'){
									$scope.surveyVariables[index].min = Math.round($scope.details.datadscr['var'][i].sumstat[j]["#text"]);
								}else if ($scope.details.datadscr['var'][i].sumstat[j].type == 'max'){
									$scope.surveyVariables[index].max = Math.round($scope.details.datadscr['var'][i].sumstat[j]["#text"]);
								}
							}
						}
						
					}else{
						
						if(typeof($scope.details.datadscr['var'][i].valrng) !="undefined") {
							var obj=$scope.details.datadscr['var'][i].valrng[0].range[0];
							$scope.surveyVariables[index].min = Math.round(obj.min);
							$scope.surveyVariables[index].max = Math.round(obj.max);
						}
					}
				}
			}
		}
		$scope.numberOfTablePages = function(_vars){
			return Math.ceil(_vars/$scope.tablePageSize);                
		}
		//Allow filtering search results
		$scope.updateFilter = function(value){
		  filterService.setFilter(value)
		  $scope.currentTablePage[0]=0
		}
		$scope.filterResults = function(vl) {
			return vl.label.toLowerCase().indexOf(filterService.getFilter().toLowerCase()) !== -1 || vl.name.toLowerCase().indexOf(filterService.getFilter().toLowerCase()) !== -1 || vl.vid.toLowerCase().indexOf(filterService.getFilter().toLowerCase()) !== -1
		};
		
		//if only one file - treat it as if there were none by removing file reference
		if(typeof($scope.survey_files)!="undefined" && $scope.survey_files.length==1){
			$scope.survey_files=null;
		}
		//set variables for display
		if(!$scope.survey_files){
			$scope._variables=$scope.surveyVariables;//set a common variable to handle the sharing of the survey-variables page
			$scope._file_num=0;//to reference the first slot in the currentTablePage array
			$scope.currentTablePage[0]=0
			//loop through the array of $scope.filterBy and add them to the _variables_filtered array
			if(typeof($scope.filterBy)!="undefined"){
				$scope._variables_filtered=[];
				for(var i = 0;i<$scope._variables.length;i++){
					if($scope.filterBy.indexOf($scope._variables[i].id)>-1){
						$scope._variables_filtered.push($scope._variables[i])
					}
				}
			}
		}
		sharedVariableStore.setVariableStore($scope.surveyVariables);//so that they are accessible to the chart for comparison
	}
	$scope.gotoPage = function(num,pg){
		$scope.currentTablePage[num] = pg;
	}
	$scope.resetCurrentPage = function(limit){
		$scope.tablePageSize=limit
		for(var i=0;i<$scope.currentTablePage.length;i++){
			$scope.currentTablePage[i]=0;
		}
	}
$scope.viewVariable = function (vl) {
	if(vl){
		$scope.selectedVariable = vl.fullData;
	}else{
		$scope.selectedVariable=null	
	}
	var modalInstance = $modal.open({
		templateUrl: $scope.chartTemplatePath,
		controller: ModalInstanceCtrl,
		size: 'lg',
		resolve: {
			items: function () {
			  return $scope.selectedVariable;
			}
		}
	});
};
$scope.my_option = 0;
$scope.downloadData = function (my_option) {
	var url=base_url+file_id//api/access/datafile/$id
	switch(Number(my_option)) {
    case 1:
        url+="?format=original"
        break;
    case 2:
       //add nothing to download the tab file
	    url+="?"
        break;
	case 3:
		 url+="?format=RData"
		break;
	case 4:
	
		//need to prep the url a bit - should look like //https://sand9.scholarsportal.info/api/meta/datafile/15
		var base_url_api=base_url.substring(0,base_url.indexOf("/api/"));
		url=base_url_api+"/api/meta/datafile/"+file_id+"?"
		break;
    default:
		return
	}
	//add the key
	url+="&key="+detailsURL.key
	window.location.assign(url);
};
$scope.goToTwoRavens =function (){
	var base_url_api=base_url.substring(0,base_url.indexOf("/api/"));
	var	url=base_url_api+"/dataexplore/gui.html?dfId="+file_id+"&key="+detailsURL.key

	window.open(url, "new");
}
$scope.reset = function() {
	$('#download').val( 0 );
};
$scope.selectFilter=function(){
	$(".search_field").select()
}
$scope.clearField=function(){
	$(".search_field").val("")
	$(".search_field").trigger( "change" );
}
$scope.downloadMyVariables = function (vl) {
	var temp_array=$cookies.variableCompare.split(",")
	window.location.assign(base_url+file_id+"?key="+detailsURL.key+"&variables="+temp_array.join(","));
};
$scope.chartMyVariables = function (vl) {
	$scope.viewVariable()
};
$scope.isChecked=function(vid){
		var temp_array=$cookies.variableCompare.split(",")
		return temp_array.indexOf(vid) !== -1
}
$scope.toggleButtons=function(vid){
	var temp_array=$cookies.variableCompare.split(",");
	if(temp_array.length>0 && temp_array[0]!=""){
		$scope.has_no_selection=false
	}else{
		$scope.has_no_selection=true
	}	
}
	
 var ModalInstanceCtrl = function ($scope, $modalInstance, items) {
  $scope.selectedVariable = items

  $scope.ok = function () {
	$modalInstance.close();
  };

  $scope.cancel = function () {
	$modalInstance.dismiss('cancel');
  };
};  

// this traverses $scope.details object. 
var traverse = function(o,func) {
	for (var i in o) {
		func.apply(this,[i,o[i]]);  
		if (o[i] !== null && typeof(o[i])=="object") {
			//going on step down in the object tree!!
			traverse(o[i],func);
		}
	}
}     
	
	  detailsURL.uri=getParameterByName("uri")
	detailsURL.key=getParameterByName("key")
	$http({
		url: detailsURL.uri, 
		method: "GET",
		//params: {requestURL: detailsURL.uri}
	}).success(function(data, status, headers, config){	
		var xml = $.parseXML(data);		
		$scope.details = xmlToJson(xml).codebook;
		connectVariablesAndData();					
	});
	

	var file_id=detailsURL.uri.match("datafile\/(.*)\/metadata")[1];
	var base_url=detailsURL.uri.substr(0,detailsURL.uri.indexOf("datafile/")+9);
	//
	$http({
		url: base_url+file_id+"?format=prep&key="+detailsURL.key, 
		method: "GET",
		//params: {requestURL: detailsURL.uri}
	}).success(function(data, status, headers, config){	
		$scope._variableData=data;
		connectVariablesAndData();
	});
	
	var loadcount=0
		function connectVariablesAndData(){
			//wait till both variables and data are loaded
			loadcount++
			if(loadcount>1){
				populateVariables();          		
				$scope.loadingDetails = false;
				$(".overlay").fadeOut();
			}
		}		
			   
}).controller('AccordionCtrl', function($scope){
$scope.show = {};
	$scope.toggle = function(index) {
		$scope.show[index] = !$scope.show[index];
	};
}).controller('CheckboxCtrl', function ($scope, $cookies) {
	 $scope.deselectAll = function(){
		//loop through page unchecking boxes
		 var temp_array=$cookies.variableCompare.split(",");
		 for(var i = 0; i< temp_array.length; i++){
			 $("#"+temp_array[i]+"_checkbox").checked = false;
		 }
		 
		 $cookies.variableCompare=""; 			 
	 }
	 $scope.selectAll = function(){	
		$('.checkbox').each(function(){ //iterate all listed checkbox items
			if(!$(this).is(":checked")){
				$(this).prop('checked', true);
				$scope.updateCompareList($(this).attr("id").substring(0,$(this).attr("id").indexOf("_checkbox")))
			}
		});		
	 }
	//keep track of the variables the user has selected
	//remember that cookies can not be arrays so we'll need to split them
$scope.updateCompareList = function(id){
	var temp_array=$cookies.variableCompare.split(",");
	
	if($cookies.variableCompare==""){
		//prevent blanks
		temp_array=[] 
	}
	if( temp_array.indexOf(id)>-1){
		//remove the item from the array
		temp_array.splice( temp_array.indexOf(id),1)
	}else{
		temp_array.push(id);
	}
	//reset cookie to a string
	$cookies.variableCompare = temp_array.join(",")
	
	$scope.toggleButtons();
	}
})
.controller('PagerCtrl', function($scope){
	$scope.showPaging =true;
})

function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}
				
// Changes XML to JSON		
function xmlToJson(xml) {
	// Create the return object
	var obj = {};
	if (xml.nodeType == 1) { // element
		// do attributes
		if (xml.attributes.length > 0) {
			for (var j = 0; j < xml.attributes.length; j++) {
				var attribute = xml.attributes.item(j);
				obj[attribute.nodeName.toLowerCase()] = attribute.nodeValue;
			}
		}
	} else if (xml.nodeType == 3) { // text
		obj = xml.nodeValue;
	}
	// do children
	if (xml.hasChildNodes()) {
		for(var i = 0; i < xml.childNodes.length; i++) {
			var item = xml.childNodes.item(i);
			var nodeName = item.nodeName.toLowerCase();;
			if (typeof(obj[nodeName]) == "undefined") {
				obj[nodeName] = xmlToJson(item);
			} else {
				if (typeof(obj[nodeName].push) == "undefined") {
					var old = obj[nodeName];
					obj[nodeName] = [];
					obj[nodeName].push(old);
				}
				obj[nodeName].push(xmlToJson(item));
			}
		}
	}
	return obj;
};