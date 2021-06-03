// depends on jQuery ajax
$(document).ready(function() {
  updateSkosmosInputs();
  //updateSkosmosDisplays();
});

function updateSkosmosInputs() {
	var num=0;
  //For each input element within personInputSelector elements 
    $('input[data-cvoc-protocol="skosmos"]').each(function() {
    	num=num+1;
    	var skosmosInput = this;
      console.log('Found field using vocabs: ' + $(skosmosInput).attr('data-cvoc-vocabs'));
        var cvocUrl = $(skosmosInput).attr('data-cvoc-service-url');
        var cvocLang = $(skosmosInput).attr('lang');
        var vocabs = JSON.parse($(skosmosInput).attr('data-cvoc-vocabs'));
        var termParentUri = $(skosmosInput).attr('data-cvoc-filter');
        var vocabSize = Object.keys( vocabs ).length;
        var managedFields = JSON.parse($(skosmosInput).attr('data-cvoc-managedfields'));
        //Hide the actual input and give it a data-skosmos number so we can find it
        $(skosmosInput).hide();
        $(skosmosInput).attr('data-skosmos', num);
        
        //Hide other managed Fields as well
        for(var val in Object.values(managedFields)) {
        	$('#' + val).find('input').hide();
        }
        
        if(vocabSize>1) {
        	var data = [];
        	for(var key in Object( vocabs).keys()) { 
        		data.push(
        	    {
        	        id: key,
        	        text: key
        	    });
        }
        	 var selectId = "skosmosAddSelect_" + num;
             $(skosmosInput).after(
               '<select id=' + selectId + ' class="form-control add-resource select2" tabindex="-1" aria-hidden="true">');
             $("#" + selectId).select2({
        		  data: data
        		})
        
        //When a selection is made, set the value of the hidden input field
        $('#' + selectId).on('select2:select', function (e) {
          var data = e.params.data;
          //For ORCIDs, the id and text are different
          console.log (data + ' is the vocab that has been selected');
          });
        }
/*
    var selectedVocab = "";
        var vocabFieldValue = $("#akmi_#{valCount.index+1}_#{cvoc.get(typeid).get('child-fields')[0].getString()}").find("input[name*='cv_vocabs_'").val();
        if (vocabFieldValue == '' && vocabsize == 1) {
            selectedVocab = "#{cvoc.get(typeid).get('child-fields')[0].getString()}";
            $("#akmi_#{valCount.index+1}_#{cvoc.get(typeid).get('child-fields')[0].getString()}").find("input[name*='cv_vocabs_'").val(selectedVocab);
        }
        $("#akmi_#{valCount.index+1}_#{cvoc.get(typeid).get('child-fields')[0].getString()}").find("input[name*='cv_vocabs_'").on("focusout", function() {
            selectedVocab = $(this).val();
        });

        var mapquery = "#{cvoc.get(typeid).getString('map-query','prefLabel')}";
        var mapid = "#{cvoc.get(typeid).getString('map-id', 'uri')}";
        var mapping = {
            query: mapquery,
            id: mapid
        };
        // Using jQuery UI Autocomplete for the term input
        $("#akmi_#{valCount.index+1}_#{cvoc.get(typeid).get('child-fields')[1].getString()}").find("input[name*='cv_term_']").autocomplete({
            source: function(request, response) {
                cv = {
                    'cvocUrl': cvocUrl,
                    'protocol': cvocProtocol,
                    'lang': cvocLang,
                    'termParentUri': termParentUri,
                    'selectedVocab': selectedVocab,
                    'term': request.term
                };
                autointerface(request, response, cv, mapping);
            },
            minLength: # {
                cvoc.get(typeid).getInt('minChars')
            },
            select: function(event, ui) {
                $.each(ui, function(i, v) {
                    $("#akmi_#{valCount.index+1}_#{cvoc.get(typeid).getString('term-uri-field')}").find("input[name*='cv_url_']").val(v.id);
                })
            }
        });
    }
    */
});
}

var mapquery = 'prefLabel';
var mapid = 'uri';
var mapping = {
    query: mapquery,
    id: mapid
};

// autocomplete calls this
function autointerface(request, response, cv, mapping) {
    var protocol = cv.protocol;
    if (!protocol) {
        protocol = 'skosmos';
    }; //default
    if (protocol == 'skosmos') {
        return (skosmos(request, response, cv, mapping));
    }
    if (protocol == 'example') {
        return (autoexample(request, response));
    };
};

function skosmos(request, response, cv, mapping) {
    var termParentUri = "";
    if (cv.termParentUri != "")
        termParentUri = "&parent=" + cv.termParentUri;
    var langParam = "";
    if (cv.lang != "")
        langParam = "&lang=" + cv.lang;

    var result = [];
    var tmp = $.ajax({
        url: cv.cvocUrl + '/rest/v1/search?unique=true&vocab=' + cv.selectedVocab + termParentUri + langParam,
        dataType: "json",
        data: {
            query: request.term + '*'
        },
        success: function(data) {
            var results = data.results;
            var queries = [];
            var array = [];
            $.each(results, function(i, item) {
                queries.push(item.prefLabel);
                array.push({
                    value: item[mapping.query],
                    id: item[mapping.id]
                });
            });

            response(array);
            console.log(array);
        }
    })
};

function autoexample(request, response) {
    $.ajax({
        url: "https://jqueryui.com/resources/demos/autocomplete/search.php",
        dataType: "jsonp",
        data: {
            term: request.term
        },
        success: function(data) {
            response(data);
            console.log(data);
        }
    });
};