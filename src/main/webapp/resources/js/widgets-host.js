/* 
 * Javascript necessary for hosted widgets
 */

var pageSource = window.location.href;

var params = parseQueryString(pageSource.split('?')[1]);

params.alias; // widgetScope
params.persistentId; // persistentId

// Utility function to convert "a=b&c=d" into { a:'b', c:'d' }
function parseQueryString(queryString) {
    var params = {};
    var pl     = /\+/g;
    if (queryString) {
        var keyValues = queryString.split('&');
        for (var i=0; i < keyValues.length; i++) {
            var pair = keyValues[i].split('=');
            params[pair[0]] = pair[1].replace(pl, " ");
        }
    }
    return params;
}

if(params.alias) {
    $('#dataverse-widget').attr({src: dvUrl + '/dataverse/' + params.alias + '?widget=dataverse@' + widgetScope});
}

if(params.persistentId) {
    $('#dataverse-widget').attr({src: dvUrl + '/dataset.xhtml?persistentId=' + params.persistentId + '&widget=dataverse@' + widgetScope});
}
