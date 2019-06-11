// function to use d3 to graph density plots with preprocessed data
// brushable added to help in disabling brushing for the DVN production mode

function density(data, node, div, brushable) {
    var mydiv;
  
    if(div=="subset") {
        mydiv = "#tab2ContentArea";
    }
    else if(div=="setx") {
        mydiv = "#setx";
    }
    else if(div=="varSummary") {
        mydiv = "#tab3";
    }
    else {
        return (alert("Error: incorrect div selected for plots"));
    }
    
    var yVals = data.properties.y;
    var xVals = data.properties.x;

    // an array of objects
    var data2 = [];
    for(var i = 0; i < data.properties.x.length; i++) {
        data2.push({x:data.properties.x[i], y:data.properties.y[i]});
    }
    
    data2.forEach(function(d) {
                  d.x = +d.x;
                  d.y = +d.y;
                  });

    var tempWidth = d3.select(mydiv).style("width")
    var width = tempWidth.substring(0,(tempWidth.length-2));
    
    var tempHeight = d3.select(mydiv).style("height")
    var height = tempHeight.substring(0,(tempHeight.length-2));

    var margin = {top: 20, right: 20, bottom: 53, left: 30};

    // Need to fix automatic width and height settings for leftpanel (#tab2, #tab3)

    if(mydiv=="#tab3"){
        width = 0.7 * (width - margin.left - margin.right),
        height = 0.3 * (height - margin.top - margin.bottom);
    }
    else if (mydiv=="#tab2ContentArea" | mydiv=="#setx"){
        width = 200;
        height = 120;
    }
    else{
        width = 0.35 * (width - margin.left - margin.right),
        height = 0.25 * (height - margin.top - margin.bottom);
    };


    var x = d3.scale.linear()
    .domain([d3.min(xVals), d3.max(xVals)])
    .range([0, width]);
    
    var invx = d3.scale.linear()
    .range([d3.min(xVals), d3.max(xVals)])
    .domain([0, width]);

    var y = d3.scale.linear()
    .domain([d3.min(yVals), d3.max(yVals)])
    .range([height, 0]);
    
    var xAxis = d3.svg.axis()
    .scale(x)
    .ticks(5)
    .orient("bottom");
    
    var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left");
    
    var brush = d3.svg.brush()
    .x(x)
    .extent(node.subsetrange)
    .on("brush", brushed);

    var brush2 = d3.svg.brush()
    .x(x)
    .on("brush", brushed2);
    
    var area = d3.svg.area()
    .interpolate("monotone")
    .x(function(d) { return x(d.x); })
    .y0(height)
    .y1(function(d) { return y(d.y); });


// This is cumbersome to treat "tab3" differently, but works for now.
//  tab3, has an issue, that unless width height hardcoded, they grow with each additional graph.
if(mydiv=="#tab3"){
    var plotsvg = d3.select(mydiv)
    .selectAll("svg")
    .remove();

    var plotsvg = d3.select(mydiv)
    .append("svg")
    .attr("id", function(){
          return data.varname.toString().concat(mydiv.substr(1));
          })
    .style("width", 300) //setting height to the height of #main.left
    .style("height", 200)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
}else{
    var plotsvg = d3.select(mydiv)
    .append("svg")
    .attr("id", function(){
          var myname = data.varname.toString();
          //myname = myname.replace(/\(|\)/g, "");
          return myname.concat("_",mydiv.substr(1), "_", node.id);
          })
    .style("width", width + margin.left + margin.right) //setting height to the height of #main.left
    .style("height", height + margin.top + margin.bottom)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
};
 
    plotsvg.append("path")
    .datum(data2)
    .attr("class", "area")
    .attr("d", area);
    
    plotsvg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")")
    .call(xAxis);
 
    plotsvg.append("text")
    .attr("x", (width / 2))
    .attr("y", 0-(margin.top / 2))
    .attr("text-anchor", "middle")
    .style("font-size", "12px")
    .text(data.varname);
    
    // add brush if brushable
    if(brushable){
    // add brush if subset
    if(mydiv=="#tab2ContentArea") {
        plotsvg.append("text")
        .attr("id", "range")
        .attr("x", 25)
        .attr("y", height+40)
        .text(function() {
              return("Range: ".concat(d3.min(xVals).toPrecision(4), " to ", d3.max(xVals).toPrecision(4)));
              });
        
        plotsvg.append("g")
        .attr("class", "x brush")
        .call(brush)
        .selectAll("rect")
        .attr("height", height);
    }
    }
    
    // add z lines and sliders setx
    if(mydiv=="#setx") {        
        plotsvg.append("text")
        .attr("id", "range")
        .attr("x", 25)
        .attr("y", height+40)
        .text(function() {
              return("x: ".concat((+node.mean).toPrecision(4)));
            });
        
        plotsvg.append("text")
        .attr("id", "range2")
        .attr("x", 25)
        .attr("y", height+50)
        .text(function() {
              return("x1: ".concat((+node.mean).toPrecision(4)));
              });

        // create tick marks at all zscores in the bounds of the data
        var lineFunction = d3.svg.line()
                            .x(function(d) { return d.x; })
                            .y(function(d) { return d.y; })
                            .interpolate("linear");
  
        var colSeq = [ "#A2CD5A","orange","red"];  // will cycle through color sequence, and then repeat last color
        var lineData = new Array;

        var zLower = -1*(d3.min(xVals)-node.mean)/node.standardDeviation;  // zscore of lower bound
        var zUpper =(d3.max(xVals)-node.mean)/node.standardDeviation;      // zscore of upper bound

        for (var i = 0; i < zUpper; i++) {
            lineData = [{ "x": x(+node.mean + i*node.standardDeviation),   "y": height*.7},  { "x": x(+node.mean+ i*node.standardDeviation),  "y": height*.9}];
            plotsvg.append("path")
            .attr("d", lineFunction([lineData[0],lineData[1]]))
            .attr("stroke", colSeq[d3.min([i,colSeq.length-1])])
            .attr("stroke-width", 1.5)
            .attr("fill", "none");
        }

        for (var i = 1; i < zLower; i++) {
            lineData = [{ "x": x(+node.mean - i*node.standardDeviation),   "y": height*.7},  { "x": x(+node.mean- i*node.standardDeviation),  "y": height*.9}];
            plotsvg.append("path")
            .attr("d", lineFunction([lineData[0],lineData[1]]))
            .attr("stroke", colSeq[d3.min([i,colSeq.length-1])])
            .attr("stroke-width", 1.5)
            .attr("fill", "none");
        }

        // initialize slider components
        var slideBox = plotsvg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height*.8 + ")")
        .call(d3.svg.axis()
              .scale(x)
              .ticks(0)
              .orient("bottom"))
        
        var slider = plotsvg.append("g")
        .attr("class", "slider")
        .call(brush);

        var handle = slider.append("polygon")
        .attr("class", "handle")
        .attr("transform", "translate(0," + height*.7 + ")")
        .attr("points", function(d){
            var s=6;
              if(node.setxvals[0]=="") {var xnm=x(node.mean);}
              else {var xnm=x(node.setxvals[0])};
            return (xnm-s)+","+(-s)+" "+(xnm+s)+","+(-s)+" "+xnm+","+(s*1.3);}); 

        var slider2 = plotsvg.append("g")
        .attr("class", "slider")
        .call(brush2);

        var handle2 = slider2.append("polygon")
        .attr("class", "handle")
        .attr("transform", "translate(0," + height*.9 + ")")
        .attr("points", function(d){
            var s=6;
              if(node.setxvals[1]=="") {var xnm=x(node.mean);}
              else {var xnm=x(node.setxvals[1])};
            return (xnm-s)+","+s+" "+(xnm+s)+","+s+" "+xnm+","+(-s*1.3);}); 
    }

    // brushing functions
    function brushed() {
        if(mydiv=="#tab2ContentArea") {
        plotsvg.select("text#range")
        .text(function() {
              if(brush.empty()) {return("Range: ".concat(d3.min(xVals).toPrecision(4), " to ", d3.max(xVals).toPrecision(4)));}
              else {return("Range: ".concat((brush.extent()[0]).toPrecision(4), " to ", (brush.extent()[1]).toPrecision(4)));}
              });
        
            if((brush.extent()[0]).toPrecision(4) != (brush.extent()[1]).toPrecision(4)) {
                node.subsetrange=[(brush.extent()[0]).toPrecision(4), (brush.extent()[1]).toPrecision(4)];
            }
            else {
                node.subsetrange=["", ""];
            }
        }
        else if(mydiv=="#setx") {
            var value = brush.extent()[0];
            var s = 6;
            
            if (d3.event.sourceEvent) {
                value = x.invert(d3.mouse(this)[0]);
                brush.extent([value, value]);
            }
            
            // set x position of slider center                     
            var xpos = x(value);
            if(value > d3.max(xVals)) {      // dragged past max
                xpos = x(d3.max(xVals));
            }
            else if(value < d3.min(xVals)) { // dragged past min
                xpos = x(d3.min(xVals));
            }
            else {
                var m = +node.mean;
                var sd = +node.standardDeviation;
                var zScore = (value - m)/sd;          // z-score
                var zRound = Math.round(zScore);      // nearest integer z-score
                if( .1 > Math.abs(zRound - zScore)) { // snap to integer z-score
                    xpos = x(m + (zRound * sd));      
                }
            }      

            // create slider symbol and text
            handle.attr("points", function(d){
                return (xpos-s)+","+(-s)+" "+(xpos+s)+","+(-s)+" "+xpos+","+(s*1.3);}); 
            plotsvg.select("text#range")
            .text(function() {
                return("x: ".concat((invx(xpos)).toPrecision(4)));});
            node.setxvals[1]=(invx(xpos)).toPrecision(4);
        }
    }



    
    function brushed2() {   // certainly a more clever way to do this, but for now it's basically copied with brush and handle changes to brush2 and handle2 and #range to #range2 and setxvals[0] to setxvals[1]
            var value = brush2.extent()[0];
            var s = 6;                            // scaling for triangle shape
            
            if (d3.event.sourceEvent) {
                value = x.invert(d3.mouse(this)[0]);
                brush2.extent([value, value]);
            }
            
            // set x position of slider center 
            var xpos = x(value);
            if(value > d3.max(xVals)) {      // dragged past max
                xpos = x(d3.max(xVals));
            }
            else if(value < d3.min(xVals)) { // dragged past min
                xpos = x(d3.min(xVals));
            }
            else {
                var m = +node.mean;
                var sd = +node.standardDeviation;
                var zScore = (value - m)/sd;          // z-score
                var zRound = Math.round(zScore);      // nearest integer z-score
                if( .1 > Math.abs(zRound - zScore)) { // snap to integer z-score
                    xpos = x(m + (zRound * sd));      
                }
            }      

            // create slider symbol and text
            handle2.attr("points", function(d){
                return (xpos-s)+","+s+" "+(xpos+s)+","+s+" "+xpos+","+(-s*1.3);}); 
            plotsvg.select("text#range2")
            .text(function() {
                return("x1: ".concat((invx(xpos)).toPrecision(4)));});
            node.setxvals[1]=(invx(xpos)).toPrecision(4);
    }
}


function bars(data, node, div) {

    // Histogram spacing
    var barPadding = .015;  // Space between bars 
    var topScale =1.2;      // Multiplicative factor to assign space at top within graph - currently removed from implementation

    // Data
    var keys = Object.keys(data.properties.values);
    var yVals = new Array;
    var xVals = new Array;
    for (var i = 0; i < keys.length; i++) {
        yVals[i] = data.properties.values[keys[i]];
        xVals[i] = Number(keys[i]);
    }
    var maxY = d3.max(yVals);
    var minX = d3.min(xVals);
    var maxX = d3.max(xVals);
   
    var mydiv;
    if(div=="subset") {
        mydiv = "#tab2ContentArea";
    }
    else if(div=="setx") {
        mydiv = "#setx";
    }
    else if(div=="varSummary") {
        mydiv = "#tab3";
    }
    else {
        return (alert("Error: incorrect div selected for plots"));
    }
    
    var tempWidth = d3.select(mydiv).style("width")
    var width = tempWidth.substring(0,(tempWidth.length-2));
    
    var tempHeight = d3.select(mydiv).style("height")
    var height = tempHeight.substring(0,(tempHeight.length-2));
      
    var margin = {top: 20, right: 20, bottom: 53, left: 50};

    // Need to fix automatic width and height settings for leftpanel (#tab2, #tab3)

    if(mydiv=="#tab3"){
        width = 0.7 * (width - margin.left - margin.right),
        height = 0.3 * (height - margin.top - margin.bottom);
    }
    else if (mydiv=="#tab2ContentArea" | mydiv=="#setx"){
        width = 200;
        height = 120;
    }
    else{
        width = 0.35 * (width - margin.left - margin.right),
        height = 0.25 * (height - margin.top - margin.bottom);
    };
    
    var x = d3.scale.linear()
    .domain([ minX-0.5 , maxX+0.5])  
    .range([0, width]);

    var invx = d3.scale.linear()
    .range([ minX-0.5 , maxX+0.5])  
    .domain([0, width]);
    
    var y = d3.scale.linear()
    .domain([0, maxY])   
    .range([0, height]);
    
    var xAxis = d3.svg.axis()
    .scale(x)
    .ticks(yVals.length)
    .orient("bottom");
    
    var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left");
  
    var brush = d3.svg.brush()
    .x(x)
    .extent(function(){
            if(node.subsetrange.length==1) {return [node.subsetrange[0], node.subsetrange[0]];}
            else {return node.subsetrange;}
            })
    .on("brush", brushed);

    var brush2 = d3.svg.brush()
    .x(x)
    .on("brush", brushed2);


//Create SVG element

// This is cumbersome to treat "tab3" differently, but works for now.
//  tab3, has an issue, that unless width height hardcoded, they grow with each additional graph.
if(mydiv=="#tab3"){              
    var plotsvg = d3.select(mydiv)
    .selectAll("svg")
    .remove();

    var plotsvg = d3.select(mydiv)
    .append("svg")
    .attr("id", function(){
          return data.varname.toString().concat(mydiv.substr(1));
          })
    .style("width", 300) //setting height to the height of #main.left
    .style("height", 200)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
}
else {
var plotsvg = d3.select(mydiv)
    .append("svg")
    .attr("id", function(){
          var myname = data.varname.toString();
          //myname = myname.replace(/\(|\)/g, "");
          return myname.concat("_",mydiv.substr(1), "_", node.id);
          })
    .style("width", width + margin.left + margin.right) //setting height to the height of #main.left
    .style("height", height + margin.top + margin.bottom)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");    
};

plotsvg.selectAll("rect")
       .data(yVals)
       .enter()
       .append("rect")
       .attr("x", function(d, i) {
        return x(xVals[i]-0.5+barPadding);  
        })     
       .attr("y", function(d) {
        return y(maxY - d);  
        })
       .attr("width", x(minX + 0.5 - 2*barPadding) )  // the "width" is the coordinate of the end of the first bar
       .attr("height", function(d) {
        return y(d);  
        })
       .attr("fill", "#1f77b4")
       ;

    plotsvg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")")
    .call(xAxis);
 
    plotsvg.append("text")
    .attr("x", (width / 2))
    .attr("y", 0-(margin.top / 2))
    .attr("text-anchor", "middle")
    .style("font-size", "12px")
    .text(data.varname);


    // add brush if subset
    if(mydiv=="#tab2ContentArea") {
        plotsvg.append("text")
        .attr("id", "range")
        .attr("x", 25)
        .attr("y", height+40)
        .text(function() {
              return("Range: ".concat(Math.round(d3.min(xVals)), " to ", Math.round(d3.max(xVals))));
              });
        
        plotsvg.append("g")
        .attr("class", "x brush")
        .call(brush)
        .selectAll("rect")
        .attr("height", height);
    }


    if(mydiv=="#setx") {
        plotsvg.append("text")
        .attr("id", "range")
        .attr("x", 25)
        .attr("y", height+40)
        .text(function() {
              return("x: ".concat(Math.round(node.mean)));
              });
        
        plotsvg.append("text")
        .attr("id", "range2")
        .attr("x", 25)
        .attr("y", height+50)
        .text(function() {
              return("x1: ".concat(Math.round(node.mean)));
              });

        // create tick marks at all zscores in the bounds of the data
        var lineFunction = d3.svg.line()
                            .x(function(d) { return d.x; })
                            .y(function(d) { return d.y; })
                            .interpolate("linear");
  
        var colSeq = [ "#A2CD5A","orange","red"];  // will cycle through color sequence, and then repeat last color
        var lineData = new Array;

        var zLower = -1*(minX-node.mean)/node.standardDeviation;  // zscore of lower bound
        var zUpper =(maxX-node.mean)/node.standardDeviation;      // zscore of upper bound

        for (var i = 0; i < zUpper; i++) {
            lineData = [{ "x": x(+node.mean + i*node.standardDeviation),   "y": height*.7},  { "x": x(+node.mean+ i*node.standardDeviation),  "y": height*.9}];
            plotsvg.append("path")
            .attr("d", lineFunction([lineData[0],lineData[1]]))
            .attr("stroke", colSeq[d3.min([i,colSeq.length-1])])
            .attr("stroke-width", 1.5)
            .attr("fill", "none");
        }

        for (var i = 1; i < zLower; i++) {
            lineData = [{ "x": x(+node.mean - i*node.standardDeviation),   "y": height*.7},  { "x": x(+node.mean- i*node.standardDeviation),  "y": height*.9}];
            plotsvg.append("path")
            .attr("d", lineFunction([lineData[0],lineData[1]]))
            .attr("stroke", colSeq[d3.min([i,colSeq.length-1])])
            .attr("stroke-width", 1.5)
            .attr("fill", "none");
        }

        for (var i = d3.min(xVals); i <= d3.max(xVals); i++) {
            lineData = [{ "x": x(i),   "y": height*.75},  { "x": x(i),  "y": height*.85}];
            plotsvg.append("path")
            .attr("d", lineFunction([lineData[0],lineData[1]]))
            .attr("stroke", "black")
            .attr("stroke-width", 1)
            .attr("fill", "none");
        }

        // initialize slider components
        var slideBox = plotsvg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height*.8 + ")")
        .call(d3.svg.axis()
              .scale(x)
              .ticks(0)
              .orient("bottom"))
        
        var slider = plotsvg.append("g")
        .attr("class", "slider")
        .call(brush);

        var handle = slider.append("polygon")
        .attr("class", "handle")
        .attr("transform", "translate(0," + height*.7 + ")")
        .attr("points", function(d){
            var s=6;
              if(node.setxvals[0]=="") {var xnm=x(node.mean);}
              else {var xnm=x(node.setxvals[0])};
            return (xnm-s)+","+(-s)+" "+(xnm+s)+","+(-s)+" "+xnm+","+(s*1.3);}); 

        var slider2 = plotsvg.append("g")
        .attr("class", "slider")
        .call(brush2);

        var handle2 = slider2.append("polygon")
        .attr("class", "handle")
        .attr("transform", "translate(0," + height*.9 + ")")
        .attr("points", function(d){
            var s=6;
              if(node.setxvals[1]=="") {var xnm=x(node.mean);}
              else {var xnm=x(node.setxvals[1])};
            return (xnm-s)+","+s+" "+(xnm+s)+","+s+" "+xnm+","+(-s*1.3);}); 
    }


    function twoSF(x){
      var tsf = d3.format(".2r");                            // format to two significant figures after the decimal place
      return tsf(x).replace( /0+$/, "").replace( /\.$/, "")  // trim trailing zeros after a period, and any orphaned period
    }


    // brushing functions
    function brushed() {
        if(mydiv=="#tab2ContentArea") {
            var brushLow = new Number;
            var brushHigh = new Number;

            if(brush.empty()) {
                node.subsetrange=["", ""]; 
            } 
            else { 
                brushLow= Math.round(brush.extent()[0]);
                brushHigh=Math.round(brush.extent()[1]);
                if(brushHigh > maxX){
                    brushHigh=maxX;
                };    
                node.subsetrange=[brushLow,brushHigh];
            };
            plotsvg.select("text#range")
            .text(function() {
                if(brush.empty()) {return("Range: ".concat(minX, " to ", maxX));}
                else {return("Range: ".concat(brushLow, " to ", brushHigh));}
                });
        } 
        else if(mydiv=="#setx") {
            var value = brush.extent()[0];
            var s = 6;
            
            if (d3.event.sourceEvent) {
                value = x.invert(d3.mouse(this)[0]);
                brush.extent([value, value]);
            }
            
            // set x position of slider center                     
            var xpos = x(value);
            if(value > maxX){            // dragged past max
                xpos = x(maxX);  
            }
            else if(value < minX){       // dragged past min
                xpos = x(minX);      
            }
            else {
                var m = +node.mean;
                var sd = +node.standardDeviation;
                var zScore = (value - m)/sd;                    // z-score
                var zRound = Math.round(zScore);                // nearest integer z-score
                if (.1 > Math.abs( Math.round(value) - value)){ // snap to integer
                    xpos = x(Math.round(value));
                } else if( .1 > Math.abs(zRound - zScore)) {    // snap to integer z-score
                    xpos = x(m + (zRound * sd));      
                } 
            }      

            // create slider symbol and text
            handle.attr("points", function(d){
                return (xpos-s)+","+(-s)+" "+(xpos+s)+","+(-s)+" "+xpos+","+(s*1.3);}); 
            plotsvg.select("text#range")
            .text(function() {
                return("x: ".concat(twoSF(invx(xpos))));});
            node.setxvals[1]=twoSF(invx(xpos));                              
        }
    }
 
    function brushed2() {   // certainly a more clever way to do this, but for now it's basically copied with brush and handle changes to brush2 and handle2 and #range to #range2 and setxvals[0] to setxvals[1]
            var value = brush2.extent()[0];
            var s = 6;                            // scaling for triangle shape
            
            if (d3.event.sourceEvent) {
                value = x.invert(d3.mouse(this)[0]);
                brush2.extent([value, value]);
            }
            
            // set x position of slider center 
            var xpos = x(value);
            if(value > maxX){              // dragged past max
                xpos = x(maxX);            
            }
            else if(value < minX){         // dragged past min
                xpos = x(minX);            
            }
            else {
                var m = +node.mean;
                var sd = +node.standardDeviation;
                var zScore = (value - m)/sd;                     // z-score
                var zRound = Math.round(zScore);                 // nearest integer z-score
                if (.1 > Math.abs( Math.round(value) - value)){  // snap to integer
                    xpos = x(Math.round(value));
                }else if( .1 > Math.abs(zRound - zScore)) {      // snap to integer z-score
                    xpos = x(m + (zRound * sd));      
                }
            }      

            // create slider symbol and text
            handle2.attr("points", function(d){
                return (xpos-s)+","+s+" "+(xpos+s)+","+s+" "+xpos+","+(-s*1.3);}); 
            plotsvg.select("text#range2")
            .text(function() {
                return("x1: ".concat(twoSF(invx(xpos))));});
            node.setxvals[1]=twoSF(invx(xpos));                      
    }

}

function barsSubset(data, node, div, brushable) {
    // if untouched, set node.subsetrange to an empty array, meaning all values selected by default
    if(node.subsetrange[0]=="" & node.subsetrange[1]=="") {
        node.subsetrange=[];
    }
    
    // Histogram spacing
    var barPadding = .015;  // Space between bars
    var topScale =1.2;      // Multiplicative factor to assign space at top within graph - currently removed from implementation
    
    // Variable name
    var myname = data.varname.toString();
    //myname = myname.replace(/\(|\)/g, "");

    
    // Data
    var keys = Object.keys(data.properties.values);
    var yVals = new Array;
    var xVals = new Array;
    for (var i = 0; i < keys.length; i++) {
        yVals[i] = data.properties.values[keys[i]];
        xVals[i] = Number(keys[i]);
    }
    
    var maxY = d3.max(yVals);
    var minX = d3.min(xVals);
    var maxX = d3.max(xVals);
    var gname = ["subsetyes", "subsetno"];
    
    var yVals2 = [];
    var yVals1 = [];
    for(i=0; i<yVals.length; i++) {
        yVals1.push({y0:maxY-yVals[i], y1:yVals[i], col:d3Color});
        yVals2.push({y0:0, y1:maxY-yVals[i], col:"transparent"});
    }
    var freqs = [yVals1, yVals2];
    
    // y0 is the starting point
    // y1 is the length of the bar
    
    var mydiv = "#tab2ContentArea";
    var width = 200;
    var height = 120;
    var margin = {top: 20, right: 20, bottom: 53, left: 50};
    
    var x = d3.scale.linear()
    .domain([ minX-0.5 , maxX+0.5])
    .range([0, width]);
    
    var invx = d3.scale.linear()
    .range([ minX-0.5 , maxX+0.5])
    .domain([0, width]);
    
    var y = d3.scale.linear()
    .domain([0, maxY])
    .range([0, height]);
    
    var xAxis = d3.svg.axis()
    .scale(x)
    .ticks(yVals.length)
    .orient("bottom");
    
    var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left");
    
    //Create SVG element
    
          var plotsvg = d3.select(mydiv)
        .append("svg")
        .attr("id", function(){
                    return myname.concat("_",mydiv.substr(1), "_", node.id);
              })
        .style("width", width + margin.left + margin.right) //setting height to the height of #main.left
    .style("height", height + margin.top + margin.bottom)
        .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    
    var freq = plotsvg.selectAll("g.freq")
    .data(freqs)
    .enter().append("g")
    .attr("class", "freq")
    .attr("name", function(d,i) {
          return myname.concat(gname[i]);
          });
    
    rect = freq.selectAll("rect")
    .data(Object)
    .enter().append("rect")
    .attr("class", "bar")
    .attr("name", function(d, i) {
          return xVals[i];
          })
    .attr("x", function(d, i) {
          return x(xVals[i]-0.5+barPadding);
          })
    .attr("y", function(d) {
          return y(d.y0);
          })
    .attr("width", x(minX + 0.5 - 2*barPadding) )  // the "width" is the coordinate of the end of the first bar
    .attr("height", function(d) {
          return y(d.y1);
          })
    .style("fill", function(d,i){
           if(node.subsetrange.length>0 & d.col===d3Color & $.inArray(xVals[i].toString(), node.subsetrange)>-1) {
            return selVarColor;
           }
           else {return d.col;}
          })
    .on("click", function(){
        if(brushable){ // disabling the brushing
        var selectMe = this;
        var selectName = this.getAttribute("name");
        if(this.parentNode.getAttribute("name")==myname.concat("subsetno")) {
            selectMe = $('[name="' + myname.concat("subsetyes") + '"]').children('[name="' + selectName + '"]')[0];
        }
        d3.select(selectMe)
        .style("fill", function(d,i){
               var myCol="";
               if(this.style.fill===selVarColor) {
                    var myindex = node.subsetrange.indexOf(this.getAttribute("name"));
                    node.subsetrange.splice(myindex, 1);
                    myCol=d3Color;
               }
               else {
                    node.subsetrange.push(this.getAttribute("name"));
                    myCol=selVarColor;
               }
               return myCol;
               });
        plotsvg.select("text#selectrange")
        .text(function() {
              if(node.subsetrange.length==0) {return("Selected: ".concat(xVals));}
              else {return("Selected: ".concat(node.subsetrange));}
              });
        } // end brushable
        });
    
    plotsvg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")")
    .call(xAxis);
    
    plotsvg.append("text")
    .attr("x", (width / 2))
    .attr("y", 0-(margin.top / 2))
    .attr("text-anchor", "middle")
    .style("font-size", "12px")
    .text(data.varname);
    
    plotsvg.append("text")
    .attr("id", "selectrange")
    .attr("x", 25)
    .attr("y", height+40)
    .text(function() {
          if(node.subsetrange.length==0) {return("Selected: ".concat(xVals));}
          else {return("Selected: ".concat(node.subsetrange));}
          });
}


function densityNode(data, node, obj) {

    var yVals = data.properties.y;
    var xVals = data.properties.x;
    
    // an array of objects
    var data2 = [];
    for(var i = 0; i < data.properties.x.length; i++) {
        data2.push({x:data.properties.x[i], y:data.properties.y[i]});
    }
    
    data2.forEach(function(d) {
                  d.x = +d.x;
                  d.y = +d.y;
                  });
    
    
    var width = 60;  // NOTE: hardcoded, should be set automatically
    var height = 30;
    var margin = {top: 20, right: 10, bottom: 53, left: 10};
    
    var x = d3.scale.linear()
    .domain([d3.min(xVals), d3.max(xVals)])
    .range([0, width]);

    var y = d3.scale.linear()
    .domain([d3.min(yVals), d3.max(yVals)])
    .range([height, 0]);
    
    var area = d3.svg.area()
    .interpolate("monotone")
    .x(function(d) { return x(d.x); })
    .y0(height)
    .y1(function(d) { return y(d.y); });
    
    var plotsvg = d3.select(obj)
    .append("svg")
    .attr("x", -40)  // NOTE: Not sure exactly why these numbers work, but these hardcoded values seem to position the plot inside g correctly.  this shouldn't be hardcoded in the future
    .attr("y", -45)
    .attr("id", function(){
            return data.varname.toString().concat("nodeplot");
            })
    .style("width", width)
    .style("height", height)
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
  
    plotsvg.append("path")
    .datum(data2)
    .attr("class", "area")
    .attr("d", area);
}

function barsNode(data, node, obj) {
    
    // Histogram spacing
    var barPadding = .015;  // Space between bars
    var topScale =1.2;      // Multiplicative factor to assign space at top within graph - currently removed from implementation
    
    // Data
    var keys = Object.keys(data.properties.values);
    var yVals = new Array;
    var xVals = new Array;
    for (var i = 0; i < keys.length; i++) {
        yVals[i] = data.properties.values[keys[i]];
        xVals[i] = Number(keys[i]);
    }
    var maxY = d3.max(yVals);
    var minX = d3.min(xVals);
    var maxX = d3.max(xVals);
    
    var width = 60;
    var height = 30;
    var margin = {top: 20, right: 10, bottom: 53, left: 10};
    
    var x = d3.scale.linear()
    .domain([ minX-0.5 , maxX+0.5])
    .range([0, width]);
    
    var invx = d3.scale.linear()
    .range([ minX-0.5 , maxX+0.5])
    .domain([0, width]);
    
    var y = d3.scale.linear()
    .domain([0, maxY])
    .range([0, height]);
    
    
    //Create SVG element
    var plotsvg = d3.select(obj)
        .append("svg")
        .attr("x", -40)
        .attr("y", -45)
    
        .attr("id", function(){
              return data.varname.toString().concat("nodeplot");
              })
        .style("width", width) //setting height to the height of #main.left
        .style("height", height)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    
    plotsvg.selectAll("rect")
    .data(yVals)
    .enter()
    .append("rect")
    .attr("x", function(d, i) {
          return x(xVals[i]-0.5+barPadding);
          })
    .attr("y", function(d) {
          return y(maxY - d);
          })
    .attr("width", x(minX + 0.5 - 2*barPadding) )  // the "width" is the coordinate of the end of the first bar
    .attr("height", function(d) {
          return y(d);
          })
    .attr("fill", "#1f77b4");
}


