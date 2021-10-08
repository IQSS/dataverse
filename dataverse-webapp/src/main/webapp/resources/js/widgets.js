/* 
 * Javascript to create the necessary HTML, CSS and JS for our widgets
 */

var scriptSource = (function() {
    var scripts = document.getElementsByTagName('script');
    return scripts[scripts.length - 1].src;
}());

var params = parseQueryString(scriptSource.split('?')[1]);

params.widget; // Widget type
params.alias; // Dataverse Alias
params.persistentId; // persistentId
params.dvUrl; // Dataverse Installation URL
params.heightPx; // iframe height in pixels
params.text = params.text || 'Search my dataverse'; // search input label/placeholder text
params.buttonText = params.buttonText || 'Find'; // search button text

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
};

if (!window.jQuery) {
  // Path to jquery.js file, eg. Google hosted version
  document.write('<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>');
}

if(params.widget === 'search') {
    /*
    * Dataverse Search Box
    */
   document.write('<input type="text" aria-label="' + params.text + '" placeholder="' + params.text + '..." onkeydown="if (event.keyCode == 13) document.getElementById(\'btnDataverseSearch\').click()" style="min-width:180px;"/>&#160;<input id="btnDataverseSearch" value="' + params.buttonText + '" type="button" onclick="window.open(&#39;' + params.dvUrl + '/dataverse.xhtml?alias=' + params.alias + '&amp;q=&#39; + this.previousSibling.previousSibling.value + &#39;&#39;, &#39;_blank&#39;);" />');
}

if(params.widget === 'iframe' && params.alias) {
    /*
     * Dataverse Listing iFrame
     */
    document.write('<iframe id="dataverse-widget" src="' + params.dvUrl + '/dataverse/' + params.alias + '?widget=dataverse@' + params.alias + '" width="100%" height="' + params.heightPx + '" style="border:0; background:url(' + params.dvUrl + '/resources/images/ajax-loading.gif) no-repeat 50% 50%;"></iframe><script>var widgetScope = "' + params.alias + '"; var dvUrl = "' + params.dvUrl + '";</script><script src="' + params.dvUrl + '/resources/js/widgets-host.js"></script>');
}

if(params.widget === 'iframe' && params.persistentId) {
    /*
     * Dataset 'Full' iFrame
     */
    document.write('<iframe id="dataset-widget" src="' + params.dvUrl + '/dataset.xhtml?persistentId=' + params.persistentId + '&widget=dataset@' + params.persistentId + '" width="100%" height="' + params.heightPx + '" style="border:0; background:url(' + params.dvUrl + '/resources/images/ajax-loading.gif) no-repeat 50% 50%;"></iframe><script>var widgetScope = "' + params.alias + '"; var dvUrl = "' + params.dvUrl + '";</script><script src="' + params.dvUrl + '/resources/js/widgets-host.js"></script>');
}

if(params.widget === 'citation') {
    /*
    * Dataset Citation iFrame
    */
   document.write('<iframe id="citation-widget" src="' + params.dvUrl + '/iframe.xhtml?persistentId=' + params.persistentId + '" width="100%" height="' + params.heightPx + '" style="border:0; background:url(' + params.dvUrl + '/resources/images/ajax-loading.gif) no-repeat 50% 50%;"></iframe><script>var widgetScope = "' + params.persistentId + '"; var dvUrl = "' + params.dvUrl + '";</script><script src="' + params.dvUrl + '/resources/js/widgets-host.js"></script>');
}
