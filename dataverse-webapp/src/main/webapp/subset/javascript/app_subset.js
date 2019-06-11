// "use strict";

console.log("fileid: "+fileid);
console.log("hostname: "+hostname);

var colors = d3.scale.category20();
var zdata;

var varColor = '#f0f8ff';   //d3.rgb("aliceblue");
var selVarColor = '#fa8072';    //d3.rgb("salmon");
var d3Color = '#1f77b4';  // d3's default blue

var lefttab = "tab1"; //global for current tab in left panel

var ddiurl = hostname+"/api/meta/datafile/"+fileid;
var dataurl = hostname+"/api/access/datafile/"+fileid;

console.log("dataurl: "+dataurl);

// Pre-processed data:
var pURL = dataurl+"?format=prep";

// Uncomment the lines below, if you want to test the code with 
// the sample data files distributed with the
// app in the "data" directory:
//if (!fileid) {
//    //pURL = "data/preprocess2429360.txt";   // This is the Strezhnev Voeten JSON data
//    pURL = "/subset/data/fearonLaitin.txt";     // This is the Fearon Laitin JSON data
//    // pURL = "data/qog_pp.json";   // This is Qual of Gov
//    ddiurl="/subset/data/fearonLaitin.xml"; // This is Fearon Laitin
//}

console.log("ddiurl: "+ddiurl);

var preprocess = {};
var valueKey = [];
var lablArray = [];
var hold = []; 
var allNodes = [];
var nodes = [];
var apicall = "";
var brushable = false; // set to true if you want to turn on brushing over the plots

d3.json(pURL, function(error, json) {
        //console.log("executing d3.json ("+pURL+")");
            if (error) return console.warn(error);
            var jsondata = json;
            
            //copying the object
            for(var key in jsondata) {
                preprocess[key] = jsondata[key];
            }
        
        d3.xml(ddiurl, "application/xml", function(xml) {
            //console.log("executing d3.xml");
               var vars = xml.documentElement.getElementsByTagName("var");
               var temp = xml.documentElement.getElementsByTagName("fileName");
               zdata = temp[0].childNodes[0].nodeValue;
               
               // dataset name trimmed to 12 chars
               var dataname = zdata.replace( /\.(.*)/, "") ;  // regular expression to drop any file extension
               // Put dataset name, from meta-data, into top panel
               d3.select("#leftpaneltitle").selectAll("h3")
               .html(dataname);
               
               for (var i=0;i<vars.length;i++) {
               
               var sumStats = new Object;
               var varStats = [];
               valueKey[i] = vars[i].attributes.name.nodeValue;
               
               if(vars[i].getElementsByTagName("labl").length === 0) {lablArray[i]="no label";}
               else {lablArray[i] = vars[i].getElementsByTagName("labl")[0].childNodes[0].nodeValue;}
               
               varStats = vars[i].getElementsByTagName("sumStat");
               for (var j=0; j<varStats.length; j++) {
                var myType = "";
                myType = varStats[j].getAttribute("type");
                if(myType==null) continue; // no sumStat
                    sumStats[myType] = varStats[j].childNodes[0].nodeValue;
                }
               
               allNodes.push({id:i, reflexive: false, "name": valueKey[i], "varid":vars[i].attributes.ID.nodeValue, "labl": lablArray[i], data: [5,15,20,0,5,15,20], count: hold, "nodeCol":colors(i), "baseCol":colors(i), "strokeColor":selVarColor, "strokeWidth":"1", "varLevel":vars[i].attributes.intrvl.nodeValue, "minimum":sumStats.min, "median":sumStats.medn, "standardDeviation":sumStats.stdev, "mode":sumStats.mode, "valid":sumStats.vald, "mean":sumStats.mean, "maximum":sumStats.max, "invalid":sumStats.invd, "subsetplot":false, "subsetrange":["", ""],"setxplot":false, "setxvals":["", ""], "grayout":false});
               };
               
               scaffolding();
               layout();
               
               });
        
            });


// functions

function scaffolding() {
    //console.log("executing scaffolding");
    var count = 0;
    d3.select("#tab1").selectAll("p")
    .data(valueKey)
    .enter()
    .append("p")
    .attr("id",function(d){
          return d.replace(/\W/g, "_"); // replace non-alphanumerics for selection purposes
          }) // perhapse ensure this id is unique by adding '_' to the front?
    .text(function(d){return d;})
    .style('background-color',function(d) {
           if(findNodeIndex(d) > 2) {return varColor;}
           else {return hexToRgba(selVarColor);}
           })
    .attr("data-container", "body")
    .attr("data-toggle", "popover")
    .attr("data-trigger", "hover")
    .attr("data-placement", "right")
    .attr("data-html", "true")
    .attr("onmouseover", "$(this).popover('toggle');")
    .attr("onmouseout", "$(this).popover('toggle');")
    .attr("data-original-title", "Summary Statistics");
    
    populatePopover(); // pipes in the summary stats
    
}


function populatePopover () {
    //console.log("executing populatePopover");
    
    d3.select("#tab1").selectAll("p")
    .attr("data-content", function(d) {
        var onNode = findNodeIndex(d);
        return popoverContent(allNodes[onNode]);
    });
}

function popoverContent(d) {
    //console.log("executing popoverContent");
    
    var rint = d3.format("r");
    return "<div class='row'><label class='col-sm-4 control-label'>Label</label><div class='col-sm-6'><p class='form-control-static'><i>" + d.labl + "</i></p></div></div>" +
    
    "<div class='row'><label class='col-sm-4 control-label'>Mean</label><div class='col-sm-6'><p class='form-control-static'>" + (+d.mean).toPrecision(4).toString() + "</p></div></div>" +
    
    "<div class='row'><label class='col-sm-4 control-label'>Median</label><div class='col-sm-6'><p class='form-control-static'>" + (+d.median).toPrecision(4).toString() + "</p></div></div>" +
    
    "<div class='row'><label class='col-sm-4 control-label'>Mode</label><div class='col-sm-6'><p class='form-control-static'>" + (+d.mode).toPrecision(4).toString() + "</p></div></div>" +
    
    "<div class='row'><label class='col-sm-4 control-label'>Stand Dev</label><div class='col-sm-6'><p class='form-control-static'>" + (+d.standardDeviation).toPrecision(4).toString() + "</p></div></div>" +
    
    "<div class='row'><label class='col-sm-4 control-label'>Maximum</label><div class='col-sm-6'><p class='form-control-static'>" + (+d.maximum).toPrecision(4).toString() + "</p></div></div>" +
    
    "<div class='row'><label class='col-sm-4 control-label'>Minimum</label><div class='col-sm-6'><p class='form-control-static'>" + (+d.minimum).toPrecision(4).toString() + "</p></div></div>" +
    
    "<div class='row'><label class='col-sm-4 control-label'>Invalid</label><div class='col-sm-6'><p class='form-control-static'>" + rint(d.invalid) + "</p></div></div>" +
    
    "<div class='row'><label class='col-sm-4 control-label'>Valid</label><div class='col-sm-6'><p class='form-control-static'>" + rint(d.valid) + "</p></div></div>" ;
}

function layout() {
    //console.log("executing layout");
    var myValues=[];
    nodes = [];
    
        if(allNodes.length > 2) {
            nodes = [allNodes[0], allNodes[1], allNodes[2]];
        }
        else if(allNodes.length === 2) {
            nodes = [allNodes[0], allNodes[1]];
        }
        else if(allNodes.length === 1){
            nodes = [allNodes[0]];
        }
        else {
            alert("There are zero variables in the metadata.");
            return;
        }
    
    
    panelPlots(); // after nodes is populated, add subset and setx panels
    
    
    //  add listerners to leftpanel.left.  every time a variable is clicked, nodes updates and background color changes.  mouseover shows summary stats or model description.
    d3.select("#tab1").selectAll("p")
    .on("click", function varClick(){
        d3.select(this)
        .style('background-color',function(d) {
               var myText = d3.select(this).text();
               var myColor = d3.select(this).style('background-color');
               
               if(d3.rgb(myColor).toString() === varColor.toString()) { // adding a variable
                nodes.push(findNode(myText));
                return hexToRgba(selVarColor);
               }
               else { // dropping a variable
                nodes.splice(nodeIndex(myText), 1);
                return varColor;
               }
            });
     panelPlots();
     });
    
} // end layout



// returns id
var findNodeIndex = function(nodeName) {
    for (var i in allNodes) {
        if(allNodes[i]["name"] === nodeName) {return allNodes[i]["id"];}
    };
}


// function to convert color codes
function hexToRgba(hex) {
    var h=hex.replace('#', '');
    
    var bigint = parseInt(h, 16);
    var r = (bigint >> 16) & 255;
    var g = (bigint >> 8) & 255;
    var b = bigint & 255;
    var a = '0.5';
    
    return "rgba(" + r + "," + g + "," + b + "," + a + ")";
}


function panelPlots() {
    //console.log("executing panelPlots");
    
    // build arrays from nodes in main
    var dataArray = [];
    var varArray = [];
    var idArray = [];
    var varidArray = [];
    
    for(var j=0; j < nodes.length; j++ ) {
        dataArray.push({varname: nodes[j].name, properties: preprocess[nodes[j].name]});
        varArray.push(nodes[j].name);
        idArray.push(nodes[j].id);
        varidArray.push(nodes[j].varid);
    }
    
    for (var i = 0; i < varArray.length; i++) {
        if (dataArray[i].properties.type === "continuous" & allNodes[idArray[i]].subsetplot==false) {
            allNodes[idArray[i]].subsetplot=true;
            density(dataArray[i], allNodes[idArray[i]], "subset", brushable);
        }
        else if (dataArray[i].properties.type === "bar" & allNodes[idArray[i]].subsetplot==false) {
            allNodes[idArray[i]].subsetplot=true;
            barsSubset(dataArray[i], allNodes[idArray[i]], "subset", brushable);
        }
    }
    
    // this is where apicall is built
    apicall=dataurl+"?variables="+varidArray.toString();
    console.log('apicall ' + apicall);
    
    // update the plots in subset tab
    d3.select("#tab2ContentArea").selectAll("svg")
    .each(function(){
          d3.select(this);
          var regstr = /(.+)_tab2ContentArea_(\d+)/;
          var myname = regstr.exec(this.id);
          var nodeid = myname[2];
          myname = myname[1];

          var j = varArray.indexOf(myname);
          
          if(j == -1) {
            allNodes[nodeid].subsetplot=false;
            var temp = "#".concat(myname,"_tab2ContentArea_",nodeid);
            d3.select(temp)
            .remove();
          }
    });
}


var nodeIndex = function(nodeName) {
    for (var i in nodes) {
        if(nodes[i]["name"] === nodeName) {return i;}
    }
}

var findNode = function(nodeName) {
    for (var i in allNodes) {if (allNodes[i]["name"] === nodeName) return allNodes[i]};
}


function tabLeft(tab) {
    //console.log("executing tableft");
    
    if(tab == "tab3") {
        API();
        return;
    }
    else {lefttab=tab;}
    var tabi = tab.substring(3);
    
    document.getElementById('tab1').style.display = 'none';
    document.getElementById('tab2').style.display = 'none';
    document.getElementById('tab3').style.display = 'none';
    
    if(tab==="tab1") {
        document.getElementById('btnSubset').setAttribute("class", "btn btn-default");
        document.getElementById('btnVariables').setAttribute("class", "btn active");
        document.getElementById('txtSubset').style.display = 'none';
    }
    else if (tab==="tab2") {
        document.getElementById('btnVariables').setAttribute("class", "btn btn-default");
        document.getElementById('btnSubset').setAttribute("class", "btn active");
        document.getElementById('txtSubset').style.display = 'block';
    }

    document.getElementById(tab).style.display = 'block';
}

function API() {
    window.open(apicall);
}
