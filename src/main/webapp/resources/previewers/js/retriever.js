var parentUrl = "";
var queryParams = null;
var datasetUrl = null;
var version = null;
var fileDownloadUrl = null;
var returnLabel = null;

function startPreview(retrieveFile) {
	// Retrieve tool launch parameters from URL
	queryParams = new URLSearchParams(window.location.search.substring(1));
	var fileUrl = queryParams.get("siteUrl") + "/api/access/datafile/"
			+ queryParams.get("fileid") + "?gbrecs=false";
	fileDownloadUrl = queryParams.get("siteUrl") + "/api/access/datafile/"
			+ queryParams.get("fileid") + "?gbrecs=true";

	var versionUrl = queryParams.get("siteUrl") + "/api/datasets/"
			+ queryParams.get("datasetid") + "/versions/"
			+ queryParams.get("datasetversion");
	var apiKey = queryParams.get("key");
	if (apiKey != null) {
		fileUrl = fileUrl + "&key=" + apiKey;
		versionUrl = versionUrl + "?key=" + apiKey;
	}
	// Get metadata for dataset/version/file
	$
			.ajax({
				dataType : "json",
				url : versionUrl,
				crossite : true,
				success : function(json, status) {
					var mdFields = json.data.metadataBlocks.citation.fields;

					var title = "";
					var authors = "";
					datasetUrl = json.data.storageIdentifier;
					datasetUrl = datasetUrl
							.substring(datasetUrl.indexOf("//") + 2);
					version = queryParams.get("datasetversion");
					if (version === ":draft") {
						version = "DRAFT";
					}
					// Setup return URL
					setParentUrl();

					for ( var field in mdFields) {
						if (mdFields[field].typeName === "title") {
							title = mdFields[field].value;
						}
						if (mdFields[field].typeName === "author") {
							var authorFields = mdFields[field].value;
							for ( var author in authorFields) {
								if (authors.length > 0) {
									authors = authors + "; ";
								}
								authors = authors
										+ authorFields[author].authorName.value;
							}
						}
					}
					var datafiles = json.data.files;
					var fileIndex = 0;
					for ( var entry in datafiles) {
						if (JSON.stringify(datafiles[entry].dataFile.id) === queryParams
								.get("fileid")) {
							fileIndex = entry;
							if (retrieveFile) {
								$.ajax({
									type : 'GET',
									dataType : 'text',
									crosssite : true,
									url : fileUrl,
									success : function(data, status) {
										writeContentAndData(data, fileUrl,
												datafiles[fileIndex].dataFile,
												title, authors, parentUrl);
									},
									error : function(request, status, error) {
										reportFailure(
												"Unable to retrieve file.",
												status);
									}
								});

							} else {
								writeContent(fileUrl,
										datafiles[entry].dataFile, title,
										authors, parentUrl);
							}
						}
					}
				},
				error : function(jqXHR, textStatus, errorThrown) {
					reportFailure("Unable to retrieve metadata.", textStatus);

				}
			});
}

function setParentUrl() {
	// Setup return URL:w

	var wo = window.opener;
	if (wo != null) {
		parentUrl = window.opener.location.href;
		returnLabel = "Go to Data Project";
		if (parentUrl.indexOf("dataset.xhtml") != -1) {
			returnLabel = "Return to Data Project";
		} else if (parentUrl.indexOf("file.xhtml") != -1) {
			returnLabel = "Return to Data File";
		}
	}

	// Use parentUrl if
	// we got it from
	// the opener, and it points to the dataset or file page
	// otherwise return
	// to the dataset
	// page
	if ((parentUrl == null)
			|| (parentUrl === "")
			|| ((parentUrl.indexOf("dataset.xhtml") == -1) && (parentUrl
					.indexOf("file.xhtml") == -1))) {
		parentUrl = queryParams.get("siteUrl")
				+ "/dataset.xhtml?persistentId=doi:" + datasetUrl + "&version="
				+ version;
	}
}

var filePageUrl = null;
function addStandardPreviewHeader(file, title, authors, parentUrl) {
	filePageUrl = queryParams.get("siteUrl") + "/file.xhtml?";
	if (file.persistentId.length == 0) {
		filePageUrl = filePageUrl + "fileId=" + file.id;
	} else {
		filePageUrl = filePageUrl + "persistentId=" + file.persistentId;
	}
	filePageUrl = filePageUrl + "&version=" + version;
	var header = $('.preview-header').append($('<div/>'));
	header.append($("<div/>").html("Filename: " + file.filename).attr('id',
			'filename'));
	if ((file.description != null) && (file.description.length > 0)) {
		header.append($('<div/>').html("Description: " + file.description));
	}
	header.append($('<div/>').text("In ").append(
			$('<span/>').attr('id', 'dataset').html(title)).append(
			$('<span/>').text(" (version " + version + ")").attr('id',
					'version')).append(
			$('<span/>').text(", by " + authors).attr('id', 'authors')));
	header.append($("<div/>").addClass("btn btn-default").html(
			"<a href='" + fileDownloadUrl + "'>Download File</a>"));
	header.append($("<div/>").addClass("btn btn-default").html(
			"<a href=\"javascript:returnToUrl(parentUrl);\">" + returnLabel
					+ "</a>"));
	header.append($("<div/>").addClass("preview-note").text(
			"File uploaded on " + file.creationDate));
}

function reportFailure(msg, statusCode) {
	var preview = $(".preview");
	preview.addClass("alert alert-danger");
	preview
			.text(msg
					+ " If problem persists (has your login timed out?), report error code: "
					+ statusCode + " to the repository administrator.");
}

function returnToUrl(parentUrl) {
	if (!window.opener) {
		// Opener is gone, just navigate to the dataset in this window
		window.location.assign(parentUrl);
	} else {
		// See if the opener is still showing the dataset
		try {
			if (window.opener.location.href === parentUrl) {
				// Yes - close the opener and reopen the dataset here (since
				// just
				// closing this window may not bring the opener to the front)
				window.opener.close();
				window.open(parentUrl, "_parent");
			} else {
				// No - so leave the opener alone and open the dataset here
				window.location.assign(parentUrl);
			}
		} catch (err) {
			// No, and the opener has navigated to some other site, so just open
			// the
			// dataset here
			window.location.assign(parentUrl);
		}
	}
}
