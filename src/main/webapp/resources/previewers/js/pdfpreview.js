var url="";

var pdfDoc = null,
    pageNum = 1,
    pageRendering = false,
    pageNumPending = null,
    scale = 1.0,
    canvas = null,
    ctx = null;

$(document)
    .ready(
        function() {

    canvas = document.getElementById('the-canvas');
    ctx = canvas.getContext('2d');
document.getElementById('prev').addEventListener('click', onPrevPage);
document.getElementById('next').addEventListener('click', onNextPage);


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
// Loaded via <script> tag, create shortcut to access PDF.js exports.
var pdfjsLib = window['pdfjs-dist/build/pdf'];

// The workerSrc property shall be specified.
pdfjsLib.GlobalWorkerOptions.workerSrc = '/resources/previewers/js/pdf.worker.js';

/**
 * Asynchronously downloads PDF.
 */
pdfjsLib.getDocument(fileUrl).promise.then(function(pdfDoc_) {
  pdfDoc = pdfDoc_;
  document.getElementById('page_count').textContent = pdfDoc.numPages;

  // Initial/first page rendering
  renderPage(pageNum);
});

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

/**
 * Get page info from document, resize canvas accordingly, and render page.
 * @param num Page number.
 */
function renderPage(num) {
  pageRendering = true;
  // Using promise to fetch the page
  pdfDoc.getPage(num).then(function(page) {
    var viewport = page.getViewport({scale: scale});
    canvas.height = viewport.height;
    canvas.width = viewport.width;

    // Render PDF page into canvas context
    var renderContext = {
      canvasContext: ctx,
      viewport: viewport
    };
    var renderTask = page.render(renderContext);

    // Wait for rendering to finish
    renderTask.promise.then(function() {
      pageRendering = false;
      if (pageNumPending !== null) {
        // New page rendering is pending
        renderPage(pageNumPending);
        pageNumPending = null;
      }
    });
  });

  // Update page counters
  document.getElementById('page_num').textContent = num;
}

/**
 * If another page rendering in progress, waits until the rendering is
 * finised. Otherwise, executes rendering immediately.
 */
function queueRenderPage(num) {
  if (pageRendering) {
    pageNumPending = num;
  } else {
    renderPage(num);
  }
}

/**
 * Displays previous page.
 */
function onPrevPage() {
  if (pageNum <= 1) {
    return;
  }
  pageNum--;
  queueRenderPage(pageNum);
}

/**
 * Displays next page.
 */
function onNextPage() {
  if (pageNum >= pdfDoc.numPages) {
    return;
  }
  pageNum++;
  queueRenderPage(pageNum);
}

