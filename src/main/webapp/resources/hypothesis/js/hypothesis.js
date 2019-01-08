$(document)
  .ready(
    function() {
        var queryParams = new URLSearchParams(window.location.search.substring(1));
        var fileUrl = queryParams.get("siteUrl") + "/api/access/datafile/" + queryParams.get("fileid") + "?gbrecs=false";
        var versionUrl= queryParams.get("siteUrl") + "/api/datasets/" + queryParams.get("datasetid") + "/versions/" + queryParams.get("datasetversion");
        var apiKey = queryParams.get("key");
        if(apiKey!=null) {
            fileUrl = fileUrl + "&key=" + apiKey;
            versionUrl = versionUrl + "?key=" + apiKey;
        }
        $.getJSON(fileUrl, function(data, status){
            $.getJSON(versionUrl,
               function(json, status) {
                var mdFields = json.data.metadataBlocks.citation.fields;

                var title="";
                var authors="";
                for(var field in mdFields) {
                if(mdFields[field].typeName === "title") {
                    title = JSON.stringify(mdFields[field].value);
                  }
                  if(mdFields[field].typeName === "author") {
                    var authorFields = mdFields[field].value;
                    for(var author in authorFields) {
                      if(authors.length>0) {
                        authors = authors + "; ";
                      }
                      authors = authors + JSON.stringify(authorFields[author].authorName.value);
                    }
                  }
                }
                var datafiles=json.data.files;
                for(var entry in datafiles) {
                   if(JSON.stringify(datafiles[entry].dataFile.id) === queryParams.get("fileid")) {
                     writeHypothesisFields(data,datafiles[entry].dataFile.creationDate, title, authors);
                   }
                 }
            });
        });
    });


function writeHypothesisFields(json, date, title, authors) {
  var hypo = $(".hypothesis");
  var url = json.rows[0].target[0].source;
  var header = $("<div/>").addClass("annotation-header");
  header.append($("<div/>").text("This is the annotations-only view of the ATI data project " + title + " by " + authors + "."));
  header.append($("<div/>").text(JSON.stringify(json.total) + " annotations retrieved on " + date));
  
  alert(JSON.stringify(json.rows[0].links.incontext);
  header.append($("<div/>").append($("<a/>").attr("href",JSON.stringify(json.rows[0].links.incontext)).text("View annotations in context")));
  header.append($("<div/>").text("Close this tab to return to the data project."));
  
  hypo.before(header);

  if (hypo.length > 0) {
    var converter = new showdown.Converter({ extensions: ['xssFilter'] });
    hypo.html("");
    var list = $("<ol>").appendTo(hypo);
    for ( var row in json.rows) {
      var selectors = json.rows[row].target[0].selector;
      var quote = "";
      for ( var k in selectors) {
        if (selectors[k].type == "TextQuoteSelector") {
          quote = selectors[k].exact;
        }
      }
      var created = json.rows[row].created;
      list.append($('<li class="annotation-card">')
        .append($('<div class="annotation-card__header">')
        .append($('<div class="annotation-card__timestamp">')
        .text(created)))
        .append($('<blockquote class="annotation-card__quote" title="Annotation quote">')
        .text(quote))
        .append($('<div class="annotation-card__text">')
        .html(converter.makeHtml(json.rows[row].text))));
      var tags = ($('<div class="annotation-card__tags" title="Tags">'))
        .appendTo(list);
      for ( var j in json.rows[row].tags) {
        tags.append($('<div class="annotation-card__tag">').text(
          json.rows[row].tags[j]));
      }
    }
    $('.annotation-card__text a').attr("rel","noopener nofollow").attr("target","_blank");
  }
}
