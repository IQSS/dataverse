/* 
 * Javascript to create the necessary HTML, CSS and JS for our widgets
 */

/*
 * 
 * http://stackoverflow.com/questions/1203933/is-there-any-analogue-in-javascript-to-the-file-variable-in-php/1204095#1204095
 * 
 */

var scriptSource = (function() {
    var scripts = document.getElementsByTagName('script');
    return scripts[scripts.length - 1].src
}());

var params = parseQueryString(scriptSource.split('?')[1]);

params.dvUrl; // Dataverse installation
params.dataverseId; // Dataverse ID
params.widget; // Widget type

// Utility function to convert "a=b&c=d" into { a:'b', c:'d' }
function parseQueryString(queryString) {
    var params = {};
    if (queryString) {
        var keyValues = queryString.split('&');
        for (var i=0; i < keyValues.length; i++) {
            var pair = keyValues[i].split('=');
            params[pair[0]] = pair[1];
        }
    }
    return params;
}

document.write('<div>' + scriptSource + '</div>');
document.write('<div>' + params.dataverseId + '</div>');
document.write('<div>' + params.dvUrl + '</div>');
document.write('<div>' + params.widget + '</div>');

if(params.widget === 'search') {
    /*
    * Dataverse Search Box
    */
   document.write('<input type="text"/>&#160;<input value="Find" type="button" onclick="location=&#39;' + params.dvUrl + '/dataverse.xhtml?dataverseId=' + params.dataverseId + '&amp;q=&#39; + this.previousSibling.previousSibling.value + &#39;&#39;" />');
}

if(params.widget === 'iframe') {
    /*
     * Dataverse Listing iFrame
     */
    document.write('<iframe src="iframe.xhtml?dataverseId=' + params.dataverseId + '" width="100%" height="500"></iframe>');
}                        
