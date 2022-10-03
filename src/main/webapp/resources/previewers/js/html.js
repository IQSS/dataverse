$(document).ready(function() {
startPreview(true);
});

function writeContentAndData(data, fileUrl, file, title, authors, parentUrl) {
    addStandardPreviewHeader(file,title, authors, parentUrl);
options = {"stripIgnoreTag":true,
           "stripIgnoreTagBody":['script','head']};  // Custom rules

    $('.preview').append($("<div/>").html(filterXSS(data,options)));
}
