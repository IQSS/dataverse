$(document)
  .ready(
    function() {
        var queryParams = new URLSearchParams(window.location.search.substring(1));
        $.getJSON(queryParams.get("siteUrl") + "/api/access/datafile/" + queryParams.get("fileid") + "?key=" + queryParams.get("key") + "&gbrecs=false", function(data, status){
            writeHypothesisFields(data);
        });
        
        //Sample - to be updated to get file creation date metadata 
//        $.ajax({
//            type: "GET",
//            url: queryParams.get("siteUrl") + "/api/meta/datafile/" + queryParams.get("fileid"),
//            cache: false,
// accepts: "text/xml,*/*",
//            dataType: "xml",
//            success: function(xml) {
//alert(xml);
           //     $(xml).find('members').each(function(){
             //       var name = $(this).find("name").text()
              //      alert(name);
//}
//                });
        });

function writeHypothesisFields(json) {
  var hypo = $(".hypothesis");
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
