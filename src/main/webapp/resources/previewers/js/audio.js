function writeContent(fileUrl, label, creationDate, title, authors, parentUrl) {
    addStandardPreviewHeader(label, creationDate, title, authors, parentUrl);
    $('.preview').append($("<audio/>").attr("controls","").attr("src",fileUrl));
}
