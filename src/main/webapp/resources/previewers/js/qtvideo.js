function writeContent(fileUrl, label, creationDate, title, authors, parentUrl) {
addStandardPreviewHeader(label, creationDate, title, authors, parentUrl);
          $(".preview").append($("<embed/>").attr("CONTROLLER","true").attr("loop","false").attr("PLUGINSPAGE","http://www.apple.com/quicktime").attr("src",fileUrl));
}
