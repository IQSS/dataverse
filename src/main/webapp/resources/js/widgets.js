/* 
 * Javascript to create the necessary HTML, CSS and JS for our widgets
 */

var scriptSource = (function() {
    var scripts = document.getElementsByTagName('script');
    return scripts[scripts.length - 1].src
}());

var params = parseQueryString(scriptSource.split('?')[1]);

params.id; // Dataverse ID
params.dvUrl; // Dataverse Installation URL
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

if(params.widget === 'search') {
    /*
    * Dataverse Search Box
    */
   document.write('<input type="text"/>&#160;<input value="Find" type="button" onclick="window.open(&#39;' + params.dvUrl + '/dataverse.xhtml?id=' + params.id + '&amp;q=&#39; + this.previousSibling.previousSibling.value + &#39;&#39;, &#39;_blank&#39;);" />');
}

if(params.widget === 'iframe') {
    /*
     * Dataverse Listing iFrame
     */
    document.write('<iframe src="' + params.dvUrl + '/iframe.xhtml?id=' + params.id + '" width="100%" height="500"></iframe>');
}

