var parentUrl = "";
$(document)
    .ready(
        function() {
          var wo = window.opener;
          if(wo!=null) {
            parentUrl = window.opener.location.href;
          }
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
          $.getJSON(
                            versionUrl,
                            function(json, status) {
                              var mdFields = json.data.metadataBlocks.citation.fields;

                              var title = "";
                              var authors = "";
                              var datasetUrl = json.data.storageIdentifier;
                              datasetUrl = datasetUrl
                                  .substring(datasetUrl
                                      .indexOf("//") + 2);
                              var version = queryParams
                                  .get("datasetversion");
                              if (version === ":draft") {
                                version = "DRAFT";
                              }
                              // Use parentUrl if
                              // we got it from
                              // the opener,
                              // otherwise return
                              // to the dataset
                              // page
                              if ((parentUrl == null)
                                  || (parentUrl === "")) {
                                parentUrl = queryParams
                                    .get("siteUrl")
                                    + "/dataset.xhtml?persistentId=doi:"
                                    + datasetUrl
                                    + "&version="
                                    + version;
                              }
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
          $("#videoPreview").append($("<video/>").attr("controls","").attr("src",fileUrl).attr("width","800"));
                                }
                              }
                            })
                        .fail(
                            function(jqXHR) {
                              reportFailure(
                                  "Unable to retrieve metadata.",
                                  jqXHR.status);
                            });
        });

function returnToDataset(parentUrl) {
  if (!window.opener) {
    //Opener is gone, just navigate to the dataset in this window
    window.location.assign(parentUrl);
  } else {
    //See if the opener is still showing the dataset
    try {
      if (window.opener.location.href === parentUrl) {
        //Yes - close the opener and reopen the dataset here (since just closing this window may not bring the opener to the front)
        window.opener.close();
        window.open(parentUrl, "_parent");
      } else {
      //No - so leave the opener alone and open the dataset here
        window.location.assign(parentUrl);
      }
    } catch (err) {
      //No, and the opener has navigated to some other site, so just open the dataset here  
      window.location.assign(parentUrl);
    }
  }
}


