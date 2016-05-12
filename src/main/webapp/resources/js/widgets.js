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
params.text; // search input placeholder text

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
   document.write('<input type="text" placeholder="' + params.text + '..." onkeydown="if (event.keyCode == 13) document.getElementById(\'btnDataverseSearch\').click()" style="background:#fff; border:1px solid #ccc; border-radius:3px; box-shadow:0 1px 1px rgba(0, 0, 0, 0.075) inset; padding:4px; min-width:180px;"/>&#160;<input id="btnDataverseSearch" value="Find" type="button" onclick="window.open(&#39;' + params.dvUrl + '/dataverse.xhtml?alias=' + params.alias + '&amp;q=&#39; + this.previousSibling.previousSibling.value + &#39;&#39;, &#39;_blank&#39;);" style="-moz-border-bottom-colors:none; -moz-border-left-colors:none; -moz-border-right-colors:none; -moz-border-top-colors:none; background-color:#f5f5f5; background-image:-moz-linear-gradient(center top , #ffffff, #e6e6e6); background-repeat:repeat-x; border:1px solid #ccc; border-color:#e6e6e6 #e6e6e6 #b3b3b3; border-image:none; border-radius:4px; box-shadow:0 1px 0 rgba(255, 255, 255, 0.2) inset, 0 1px 2px rgba(0, 0, 0, 0.05); color:#333; cursor:pointer; text-shadow:0 1px 1px rgba(255, 255, 255, 0.75); padding:0.3em 1em; line-height:1.4;" />');
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
