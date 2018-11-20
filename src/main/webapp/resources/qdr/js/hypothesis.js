$(document)
    .ready(
        function() {
          window.setInterval(checkHypothesisFields, 5000);
        });

function checkHypothesisFields() {
  var hypo = $(".hypothesis");
  if (hypo.length > 0) {
    if (!hypo.hasClass("prettified")) {
      var json = JSON.parse(hypo.html().replace(/[\n\r]+/g, ''));
      hypo.addClass("prettified");
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
        list
            .append($('<li class="annotation-card">')
                .append(
                    $(
                        '<div class="annotation-card__header">')
                        .append(
                            $(
                                '<div class="annotation-card__timestamp">')
                                .text(created)))
                .append(
                    $(
                        '<blockquote class="annotation-card__quote" title="Annotation quote">')
                        .text(quote))
                .append(
                    $('<div class="annotation-card__text">')
                        .text(json.rows[row].text)));
        var tags = ($('<div class="annotation-card__tags" title="Tags">'))
            .appendTo(list);
        for ( var j in json.rows[row].tags) {
          tags.append($('<div class="annotation-card__tag">').text(
              json.rows[row].tags[j]));
        }
      }
    }
  } else {
	    $("label[for='metadata_annotations']").parent().find('div').children()
        .next().find('input').off('change');
    $("label[for='metadata_annotations']").parent().find('div').children()
        .next().find('input').change(
            function() {
              var url = $(
                  "label[for='metadata_annotationsSource']")
                  .parent().find('input').val();
              var group = $(
                  "label[for='metadata_annotationsGroup']")
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
                $("label[for='metadata_annotations']").parent()
                    .find('div').children().next()
                    .children().next().find('textarea')
                    .html(JSON.stringify(json));
              });
                }
            });

  }
};
