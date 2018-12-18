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
  } else {
    if($(".annotation_raw").length==0) {
      $("label[for='metadata_annotations']").parent().find('div').children()
        .next().find('input').off('change');
      $("label[for='metadata_annotationsJSON']").parent()
        .find('div').append($('<div>').text( $("label[for='metadata_annotationsJSON']").parent()
        .find('div').find('textarea').text()).addClass("annotation_raw"));
      $("label[for='metadata_annotationsJSON']").parent()
        .find('div').find('textarea').hide();
      $("label[for='metadata_annotations']").parent().find('div').children()
        .next().find('input').change(function() {
           var url = $("label[for='metadata_annotationsSource']")
              .parent().find('input').val();
           var group = $("label[for='metadata_annotationsGroup']")
              .parent().find('input').val();
           if(group.length>0 && url.length>0) {
           var apikey = prompt("Enter a Hypothesis apiKey to update the Raw Annotations field", "");
           $.ajaxSetup({
             headers : {
               'Authorization' : 'Bearer ' + apikey
             }
           });
           $.getJSON('https://hypothes.is/api/search?group='
             + group + '&uri=' + url, function(json) {
               $(".annotation_raw").remove();
               $("label[for='metadata_annotationsJSON']").parent()
                 .find('div').append($('<div>').text(JSON.stringify(json).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')).addClass("annotation_raw"));
               $("label[for='metadata_annotationsJSON']").parent()
                 .find('div').find('textarea')
                 .text(JSON.stringify(json).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')).hide();
            });
          }
      });
    }
  }
};
