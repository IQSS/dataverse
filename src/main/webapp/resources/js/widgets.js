document.addEventListener('DOMContentLoaded', function() {
    // Function to find the script source
    function findScriptSource() {
        const widgetScript = document.getElementById('dataverse-widget');
        if (widgetScript && widgetScript.src) {
            return widgetScript.src;
        }
        
        const scripts = document.getElementsByTagName('script');
        for (const script of scripts) {
            if (script.src && script.src.includes('widgets.js')) {
                return script.src;
            }
        }
        
        console.error('Could not find the widgets.js script source');
        return null;
    }

    // Function to parse query string
    function parseQueryString(queryString) {
        const params = {};
        if (queryString) {
            const keyValues = queryString.split('&');
            for (const keyValue of keyValues) {
                const [key, value] = keyValue.split('=');
                params[key] = decodeURIComponent(value.replace(/\+/g, " "));
            }
        }
        return params;
    }

    // Function to create and append elements
    function appendElement(tag, attributes = {}, textContent = '') {
        const element = document.createElement(tag);
        Object.assign(element, attributes);
        if (textContent) element.textContent = textContent;
        scriptTag.parentNode.insertBefore(element, scriptTag.nextSibling);
        return element;
    }

    // Find the script source
    const scriptSource = findScriptSource();
    if (!scriptSource) return;

    // Find the script tag in the document
    const scriptTag = Array.from(document.getElementsByTagName('script'))
        .find(script => script.src === scriptSource);
    if (!scriptTag) {
        console.error('Could not find the widgets.js script tag in the document');
        return;
    }

    // Parse parameters
    const params = parseQueryString(scriptSource.split('?')[1]);

    // Load jQuery if not present
    if (!window.jQuery) {
        const jqueryScript = appendElement('script', {
            src: "https://code.jquery.com/jquery-3.6.0.min.js",
            integrity: "sha256-/xUj+3OJU5yExlq6GSYGSHk7tPXikynS7ogEvDej/m4=",
            crossOrigin: "anonymous"
        });
        jqueryScript.onload = initializeWidget;
    } else {
        initializeWidget();
    }

    function initializeWidget() {
        if (params.widget === 'search') {
            const input = appendElement('input', {
                type: "text",
                placeholder: params.text + "...",
                style: "background:#fff; border:1px solid #ccc; border-radius:3px; box-shadow:0 1px 1px rgba(0, 0, 0, 0.075) inset; padding:4px; min-width:180px;"
            });
            input.addEventListener('keydown', function(event) {
                if (event.keyCode === 13) {
                    document.getElementById('btnDataverseSearch').click();
                }
            });

            appendElement('span', {}, ' ');

            const button = appendElement('input', {
                id: "btnDataverseSearch",
                type: "button",
                value: "Find",
                style: "-moz-border-bottom-colors:none; -moz-border-left-colors:none; -moz-border-right-colors:none; -moz-border-top-colors:none; background-color:#f5f5f5; background-image:-moz-linear-gradient(center top , #ffffff, #e6e6e6); background-repeat:repeat-x; border:1px solid #ccc; border-color:#e6e6e6 #e6e6e6 #b3b3b3; border-image:none; border-radius:4px; box-shadow:0 1px 0 rgba(255, 255, 255, 0.2) inset, 0 1px 2px rgba(0, 0, 0, 0.05); color:#333333; cursor:pointer; display:inline-block; font-size:14px; line-height:20px; margin-bottom:0; padding:4px 12px; text-align:center; text-shadow:0 1px 1px rgba(255, 255, 255, 0.75); vertical-align:middle;"
            });
            button.addEventListener('click', function() {
                window.open(params.dvUrl + '/dataverse.xhtml?alias=' + params.alias + '&q=' + input.value, '_blank');
            });
        } else if (params.widget === 'iframe') {
            const iframeUrl = params.persistentId 
                ? params.dvUrl + '/dataset.xhtml?persistentId=' + params.persistentId + '&widget=dataset@' + params.persistentId
                : params.dvUrl + '/dataverse/' + params.alias + '?widget=dataverse@' + params.alias;

            appendElement('iframe', {
                id: params.persistentId ? "dataset-widget" : "dataverse-widget",
                src: iframeUrl,
                width: "100%",
                height: params.heightPx,
                style: "border:0; background:url(" + params.dvUrl + "/resources/images/ajax-loading.gif) no-repeat 50% 50%;"
            });

            appendElement('script', {}, 'var widgetScope = "' + (params.persistentId || params.alias) + '"; var dvUrl = "' + params.dvUrl + '";');
            appendElement('script', { src: params.dvUrl + '/resources/js/widgets-host.js' });
        } else if (params.widget === 'citation') {
            appendElement('iframe', {
                id: "citation-widget",
                src: params.dvUrl + '/iframe.xhtml?persistentId=' + params.persistentId,
                width: "100%",
                height: params.heightPx,
                style: "border:0; background:url(" + params.dvUrl + "/resources/images/ajax-loading.gif) no-repeat 50% 50%;"
            });

            appendElement('script', {}, 'var widgetScope = "' + params.persistentId + '"; var dvUrl = "' + params.dvUrl + '";');
            appendElement('script', { src: params.dvUrl + '/resources/js/widgets-host.js' });
        }
    }
});