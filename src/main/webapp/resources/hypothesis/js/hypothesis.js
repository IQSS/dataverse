$(document)
  .ready(
    function() {
        var queryParams = new URLSearchParams(window.location.search.substring(1));
        $.getJSON(queryParams.get("siteUrl") + "/api/access/datafile/" + queryParams.get("fileid") + "?key=" + queryParams.get("key") + "&gbrecs=false", function(data, status){
            $.getJSON(queryParams.get("siteUrl") + "/api/datasets/" + queryParams.get("datasetid") + "/versions/" + queryParams.get("datasetversion") + "/files"  + "?key=" + queryParams.get("key"),
               function(json, status) {
                 var datafiles=json.data;
                 for(var entry in datafiles) {
                   if(JSON.stringify(datafiles[entry].dataFile.id) === queryParams.get("fileid")) {
                     writeHypothesisFields(data,datafiles[entry].dataFile.creationDate);
                   }
                 }
            });
        });
    });

function writeHypothesisFields(json, date) {
  var hypo = $(".hypothesis");
  var url = json.rows[0].target[0].source;
  var header = $("<div/>").addClass("annotation-header");
  header.append($("<div/>").text("Source Document: ").append($("<a/>").attr("href",url).attr("target","_blank").text(url)));
  header.append($("<div/>").text("Created in Group: ").append($("<a/>").attr("href","https://hypothes.is/groups/" + json.rows[0].group).attr("target","_blank").text(json.rows[0].group)));
  header.append($("<div/>").text("Current as of Date: " + date));
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
