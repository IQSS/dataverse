'use strict';
angular.module('odesiApp').controller('chartCtrl', function($scope, $cookies,sharedVariableStore){
		$scope.lang = (!$cookies.language || $cookies.language === 'en') ? en : fr;
        //if there are other charts selected show them too!
		$scope.variableCompare=[];//all the selected variables
		var temp_array=[]
		if($cookies.variableCompare){
			var temp_array=$cookies.variableCompare.split(",");//because they are stored in an serialized array	
		}
		if(!$scope.selectedVariable){
			//create an array with the selection
			for(var i=0;i<temp_array.length;i++){
				//find the the variable in the survey
				for(var j=0;j<sharedVariableStore.getVariableStore().length;j++){
					if(sharedVariableStore.getVariableStore()[j].vid==temp_array[i]){
						$scope.variableCompare.push(sharedVariableStore.getVariableStore()[j]);
					}
				}
			}
		}else{
			//Show just the one
			for(var j=0;j<sharedVariableStore.getVariableStore().length;j++){
				if(sharedVariableStore.getVariableStore()[j].vid==$scope.selectedVariable.id){
					$scope.variableCompare.push(sharedVariableStore.getVariableStore()[j]);
				}
			}
		}
		//
		$scope.$watch ('selectedVariable', function(){
			//get the data for the selected variable
				for(var k=0;k<$scope.variableCompare.length;k++){
					
					var _variableData=$scope.variableCompare[k].fullData.variable_data
					var obj_full=$scope.variableCompare[k].fullData;
					
					var obj=$scope.variableCompare[k];
					//store all properties with the variableCompare object
					var data = obj_full.catgry;
					var sumstat = obj_full.sumstat;
					var rows = [];
					var table = [];
					var summary = [];
					//
					try{
						//access all the summary statistics
						for (var i = 0; i < sumstat.length; i++){ 
							summary.push({c:[]});
							if(!sumstat[i].wgtd && !isNaN(parseFloat(sumstat[i]["#text"]))) {
								summary[i].c.push({v: sumstat[i].type});
								summary[i].c.push({v: parseFloat(sumstat[i]["#text"])});	
								
							} 	
						
						}
						//sort the summary
						var summary_order=["vald","invd","max","min","mean","medn","mode","stdev"];
						var summary_ordered=[]
						for (var i = 0; i < summary.length; i++){ 
							//make sure the value is available
							if(typeof(summary[i].c[0])!=="undefined"){
								summary_ordered[$.inArray(summary[i].c[0].v,summary_order)]=summary[i]
							}
						}
						summary=summary_ordered
						obj.summary = summary;
					}catch(e){
						console.log(e);
					}
					//exception for dataverse
					if(typeof (obj_full.labl._)!=="undefined"){
						obj.labl= [obj_full.labl._]
					}else{
						obj.labl=[obj_full.labl]
					}
					//
					var non_sequential=false;
					//need to blend the descriptions with the freqency data (stored in the _variableData)
					var total=parseFloat(_variableData.valid);
					if(typeof(data)=="undefined" ){//there is no 'catgry' values
						if(typeof(_variableData.plotvalues)=="undefined"){
							return	
						}else{
							//artificially create data obj - we likely have value and freq
							var temp_data=[]
							for (var i in _variableData.plotvalues){
								temp_data.push({labl:{"#text":i}, catvalu:{"#text":i},freq:_variableData.plotvalues[i]})
							}
							data = temp_data
						}
						
					}
					for (var i = 0; i < data.length; i++){
						rows.push({c:[]});
						table.push({c:[]});
						var num=rows.length-1//use a separate number since counts may be missaligned
						var table_num=table.length-1
						//
						var labl="";
						if(typeof (data[i].labl["#text"])!=="undefined"){
							labl=data[i].labl["#text"]
						}else{
							//missing catagories
							labl=i+1
							if(data[i].missing) {
								labl="missing"
							}
							non_sequential=true
						}
						//
						if(!data[i].missing) {
							rows[num].c.push({v: labl});
							//need to match the id of the response with the frequency
							try{
								if(typeof(_variableData.plotvalues[data[i].catvalu["#text"]])!="undefined"){
									rows[num].c.push({v: parseFloat(_variableData.plotvalues[data[i].catvalu["#text"]])});
								}else{
									rows[num].c.push({v:data[i].freq})
								}								
							}catch(e){
								rows.pop();
								data[i].missing=true;
								//no data to show
							}
						}
						//
						if(!data[i].missing || data[i].catvalu) {
							//table structure Values,Categories,N
							//keep track of the items location
							table[table_num].n=num
							//
							if (data[i].catvalu["#text"]) {
								table[table_num].c.push({v: data[i].catvalu["#text"]});
							} else {
								table[table_num].c.push({v: 0});	
							}
							table[table_num].c.push({v:labl});
							//make sure there are frequency values
							if( _variableData.plotvalues && typeof(_variableData.plotvalues[data[i].catvalu["#text"]])!="undefined"){
								if(!isNaN(_variableData.plotvalues[data[i].catvalu["#text"]])){
									table[table_num].c.push({v: parseFloat(_variableData.plotvalues[data[i].catvalu["#text"]])});
								}else{
									table[num].c.push({v: 0});	
								}
							}else{
								table[table_num].c.push({v: ""});	
							}
							if(non_sequential){
								//check if the value is a number
								if(!isNaN(table[table_num].c[0])){
									//adjust the number order
									var temp_obj= jQuery.extend({},table[table_num].c)
									table[table_num].c[0]=temp_obj[1]
									table[table_num].c[1]=temp_obj[2]
									table[table_num].c[2]=temp_obj[0]
									rows[num].c[1]=temp_obj[0]
								}
							}
							//add color if valid value
							if(rows[num] && rows[num].c.length>0){
								rows[num].c.push({v: getColor(i)})
								var display_val="";
								if(total>0){
									//Calculate the percent to 1 decimal
									display_val=Math.round(Number(table[table_num].c[2].v)/total*1000)/10+"%";
								}else{
									display_val= table[table_num].c[2].v
								}
								rows[num].c.push({v:display_val})
								//also add 'value' for initial ordering
								rows[num].c.push({v:data[i].catvalu["#text"]})
								
							}
						}
					}
					
					
					
					//get a count of valid rows for proper charting
					var valid_rows=0
					//keep track of the invalid rows
					//make sure all the rows have data
					var invalid_rows=[];
					for(var i = 0;i<rows.length;i++){
						if(rows[i].c.length>1){
							//add the number where it resides
							rows[i].n=valid_rows
							//increment the row number
							valid_rows++
						}else{
							invalid_rows.push(i)
						}
					} 
					//loop backwards removing any invalid rows
					for(var i = invalid_rows.length-1;i>=0;i--){	
						rows.splice(invalid_rows[i],1)
					}
					//resort the responses based on the value ascending
					if(table.length>0){
						table.sort(function(a, b) {  return (b.c[0].v < a.c[0].v);});
					}
					if(rows.length>0){
						rows.sort(function(a, b) {return (b.c[4].v < a.c[4].v);});
					}

					//
					obj.chart= {
							type : 'BarChart',
							display : false,
							cssStyle : "height:350px; width:100%;",
							legend: 'none',
							hAxis : { textPosition : 'in' },
							options : {				
								"displayExactValues": true,
								chartArea:{left:150, top: 10,width:"100%",height:"100%"},
							},
							data : {
								cols : [
									{
										id: 'cat',
										label: 'Categories',
										type: 'string'
									},
									{
										id: 'freq',
										label: 'Frequency [unweighted]',
										type: 'number'
									},
									 {role: "style", type: "string"},
									 { role: 'annotation', type: "string" }
								],
								rows : []
							}
						};
					obj.chart_text={};
					if(valid_rows==0){
						//there is no data to graph - show a notice!
						obj.chart.cssStyle="display:none;";
						obj.chart_text.cssStyle="display:block; width:100%;text-align: center;";
					}else{
						obj.chart_text.cssStyle="display:none; width:100%;text-align: center;";
						if(valid_rows<=2){
							obj.chart.type="PieChart"
						}else{
							//set the height based on the number of columns
							obj.chart.cssStyle="height:"+valid_rows*40+"px; width:100%;";
						}
						obj.chart.data.rows = rows;	
					}
	
					//
					obj.table = table;
					//
					
				}
			
			var sort_states=["default","desc","asc"];
			$scope.sortgraph = function(num) {
				var obj=$scope.variableCompare[num]
				if(typeof(obj.sort_state)=="undefined"){
					obj.sort_state=0;
				}
				//cycle between 3 sortings - default, asc and desc
				obj.sort_state++
		        if(obj.sort_state>=sort_states.length){
		        	obj.sort_state=0;
		        }
		        var vid=obj.vid
		        if(obj.sort_state==1){
		        	//descending
		        	$("#"+vid+"_sort_up").hide();
		        	$("#"+vid+"_sort_down").show();
		        	obj.table.sort(function(a, b) {  return (b.c[2].v > a.c[2].v);});
		        	obj.chart.data.rows.sort(function(a, b) {return (b.c[1].v > a.c[1].v);});
		        }else if(obj.sort_state==2){
		        	//ascending
		        	$("#"+vid+"_sort_up").show();
		        	$("#"+vid+"_sort_down").hide();
		        	obj.table.sort(function(a, b) {  return (b.c[2].v < a.c[2].v);});
		        	obj.chart.data.rows.sort(function(a, b) {return (b.c[1].v < a.c[1].v);});
		        }else{
		        	//default
		        	$("#"+vid+"_sort_up").show();
		        	$("#"+vid+"_sort_down").show();
		        	obj.chart.data.rows.sort(function(a, b) {return (b.n < a.n);});
		        	obj.table.sort(function(a, b) {return (b.n < a.n);});
		        
		        } 
		    }

		});
		
	})
///
var color_array=["#3366cc","#dc3912","#ff9900","#109618","#990099","#0099c6","#dd4477","#66aa00","#b82e2e","#316395","#994499","#22aa99","#aaaa11","#6633cc","#e67300","#8b0707","#651067","#329262","#5574a6","#3b3eac","#b77322","#16d620","#b91383","#f4359e","#9c5935","#a9c413","#2a778d","#668d1c","#bea413","#0c5922","#743411"];
function getColor(num){
	var color;
	if(num<color_array.length){
		color=color_array[num]
	}else{
		color=getRandomColor()
	}
	return color
}
function getRandomColor() {
    var letters = '0123456789ABCDEF'.split('');//'0123456789ABCDEF'
    var color = '#';
    for (var i = 0; i < 6; i++ ) {
        color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
}
