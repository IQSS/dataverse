/* 
 * Javascript to create the necessary HTML, CSS and JS for our widgets
 */

var scriptSource = (function() {
    var scripts = document.getElementsByTagName('script');
    return scripts[scripts.length - 1].src
}());

var params = parseQueryString(scriptSource.split('?')[1]);

params.alias; // Dataverse Alias
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
   document.write('<input type="text" placeholder="Search my dataverse..." onkeydown="if (event.keyCode == 13) document.getElementById(\'btnDataverseSearch\').click()" style="background:#fff; border:1px solid #ccc; border-radius:3px; box-shadow:0 1px 1px rgba(0, 0, 0, 0.075) inset; padding:4px; min-width:180px;"/>&#160;<input id="btnDataverseSearch" value="Find" type="button" onclick="window.open(&#39;' + params.dvUrl + '/dataverse.xhtml?alias=' + params.alias + '&amp;q=&#39; + this.previousSibling.previousSibling.value + &#39;&#39;, &#39;_blank&#39;);" style="-moz-border-bottom-colors:none; -moz-border-left-colors:none; -moz-border-right-colors:none; -moz-border-top-colors:none; background-color:#f5f5f5; background-image:-moz-linear-gradient(center top , #ffffff, #e6e6e6); background-repeat:repeat-x; border:1px solid #ccc; border-color:#e6e6e6 #e6e6e6 #b3b3b3; border-image:none; border-radius:4px; box-shadow:0 1px 0 rgba(255, 255, 255, 0.2) inset, 0 1px 2px rgba(0, 0, 0, 0.05); color:#333; cursor:pointer; text-shadow:0 1px 1px rgba(255, 255, 255, 0.75); padding:0.3em 1em; line-height:1.4;" />');
}

if(params.widget === 'iframe') {
    /*
     * Dataverse Listing iFrame
     */
    document.write('<iframe src="' + params.dvUrl + '/iframe.xhtml?alias=' + params.alias + '" width="100%" height="500" style="padding:4px; border:0; background:url(' + params.dvUrl + '/resources/images/ajax-loading.gif) no-repeat 50% 50%;"></iframe>');
}

