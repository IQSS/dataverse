function writeContent(fileUrl, label, creationDate, title, authors, parentUrl) {
addStandardPreviewHeader(label, creationDate, title, authors, parentUrl);
$('.preview').append($('<img/>').attr('style','max-width:100%').attr('src',fileUrl).attr('id','previewImage'));
    $("#previewImage")
    .wrap('<span style="display:inline-block"></span>')
    .css('display', 'block')
    .parent()
    .zoom({on:'grab'});
}

