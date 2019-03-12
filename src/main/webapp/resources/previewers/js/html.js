
$(document).ready(function() {
startPreview(true);
});

function writeContentAndData(data, fileUrl, file, title, authors, parentUrl) {
    addStandardPreviewHeader(file,title, authors, parentUrl);

    $('.preview').append($("<div/>").html(filterXSS(data)));
}