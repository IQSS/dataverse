$(document)
    .ready(
        function() {
          var queryParams = new URLSearchParams(
              window.location.search.substring(1));
          var fileUrl = queryParams.get("siteUrl")
              + "/api/access/datafile/"
              + queryParams.get("fileid") + "?gbrecs=false";
          var versionUrl = queryParams.get("siteUrl")
              + "/api/datasets/" + queryParams.get("datasetid")
              + "/versions/" + queryParams.get("datasetversion");
          var apiKey = queryParams.get("key");
          if (apiKey != null) {
            fileUrl = fileUrl + "&key=" + apiKey;
            versionUrl = versionUrl + "?key=" + apiKey;
          }
          $
              .getJSON(
                  fileUrl,
                  function(data, status) {
                    $
                        .getJSON(
                            versionUrl,
                            function(json, status) {
                              var mdFields = json.data.metadataBlocks.citation.fields;

                              var title = "";
                              var authors = "";
                              var datasetUrl = json.data.storageIdentifier;
                              datasetUrl = datasetUrl
                                  .substring(datasetUrl
                                      .indexOf("//") + 2);
                              for ( var field in mdFields) {
                                if (mdFields[field].typeName === "title") {
                                  title = mdFields[field].value;
                                }
                                if (mdFields[field].typeName === "author") {
                                  var authorFields = mdFields[field].value;
                                  for ( var author in authorFields) {
                                    if (authors.length > 0) {
                                      authors = authors
                                          + "; ";
                                    }
                                    authors = authors
                                        + authorFields[author].authorName.value;
                                  }
                                }
                              }
                              var datafiles = json.data.files;
                              for ( var entry in datafiles) {
                                if (JSON
                                    .stringify(datafiles[entry].dataFile.id) === queryParams
                                    .get("fileid")) {
                                  writeHypothesisFields(
                                      data,
                                      datafiles[entry].dataFile.creationDate,
                                      title,
                                      authors,
                                      datasetUrl);
                                }
                              }
                            });
                  });
        });

function writeHypothesisFields(json, date, title, authors, datasetUrl) {
  //Order by TextPositionSelector.start
  json.rows.sort(annotationCompare);

  //Create header block
  var hypo = $(".hypothesis");
  var url = json.rows[0].target[0].source;
  var header = $("<div/>").addClass("annotation-header");
  header.append($("<div/>").html(
      "This is the annotations-only view of the ATI data project <a href=\""
          + datasetUrl + "\">" + title + "</a> by " + authors + "."));
  header.append($("<div/>").addClass("btn btn-default").append(
      $("<a/>").attr("href", json.rows[0].links.incontext).text(
          "View Annotations In Context")));

  header
      .append($("<div/>")
          .addClass("btn btn-default")
          .html(
              "<a href=\"javascript:window.close();\">Return To The Data Project.</a>"));
  header.append($("<div/>").addClass("annotation-note").text(
      json.total + " annotations, retrieved on " + date));

  hypo.before(header);

  if (hypo.length > 0) {
    var converter = new showdown.Converter({
      extensions : [ 'xssFilter' ]
    });
    hypo.html("");


    //Display annotations
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
      list
          .append($('<li class="annotation-card">')
              .append(
                  $(
                      '<blockquote class="annotation-card__quote" title="Annotation quote">')
                      .text(quote))
              .append(
                  $('<div class="annotation-card__text">')
                      .html(
                          converter
                              .makeHtml(json.rows[row].text))));
      var tags = ($('<div class="annotation-card__tags" title="Tags">'))
          .appendTo(list);
      for ( var j in json.rows[row].tags) {
        tags.append($('<div class="annotation-card__tag">').text(
            json.rows[row].tags[j]));
      }
    }
    $('.annotation-card__text a').attr("rel", "noopener nofollow").attr(
        "target", "_blank");
  }
}

function annotationCompare(a, b) {
    var aPosition = 0;
    var bPosition = 0;
    for ( var j in a.target[0].selector) {
      if (a.target[0].selector[j].type === "TextPositionSelector") {
        aPosition = a.target[0].selector[j].start;
      }
    }
    for ( var j in b.target[0].selector) {
      if (b.target[0].selector[j].type === "TextPositionSelector") {
        bPosition = b.target[0].selector[j].start;
      }
    }
    if (aPosition < bPosition)
      return -1;
    if (aPosition > bPosition)
      return 1;
    return 0;
  }
