// depends on jQuery ajax
var mapquery = 'prefLabel';
var mapid = 'uri';
var mapping = { query: mapquery,  id: mapid };

// autocomplete calls this
function autointerface(request, response, cv, mapping) {
    var protocol = cv.protocol;
    if (!protocol) { protocol = 'skosmos'; };//default
    if (protocol == 'skosmos') {
        return(skosmos(request, response, cv, mapping)); }
    if (protocol == 'example') {
        return(autoexample(request, response)); };
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
        url:  cv.cvocUrl + '/rest/v1/search?unique=true&vocab=' + cv.selectedVocab + termParentUri + langParam,
        dataType: "json",
        data: { query: request.term + '*' },
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

	    response( array );
            console.log( array );
        } 
    })
};

function autoexample (request, response) {
    $.ajax( {
          url: "https://jqueryui.com/resources/demos/autocomplete/search.php",
          dataType: "jsonp",
          data: {
            term: request.term
          },
          success: function( data ) {
            response( data );
            console.log( data );
          }
        } );
};