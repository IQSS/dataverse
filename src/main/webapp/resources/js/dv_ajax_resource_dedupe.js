/*
 * Skip scripts and stylesheets that are already loaded when a JSF AJAX response asks for them again.
 *
 * Why this exists: when an AJAX response renders components whose resources have not been
 * rendered yet in that request (for example the lazily loaded contact dialog, which is
 * dynamic="true"), Jakarta Faces adds an <update id="jakarta.faces.Resource"> block listing the
 * jQuery and PrimeFaces scripts. The PrimeFaces 11 client appends that block to <head> without
 * checking whether the scripts are already there (https://github.com/primefaces/primefaces/issues/11714,
 * fixed on the client side in PrimeFaces 14.0.3). The browser then runs jQuery and PrimeFaces core
 * a second time, which drops the handlers that hide the ajax "loading" image, so the spinner stays
 * on screen after "Contact Owner" is clicked.
 *
 * This filters the response before PrimeFaces sees it. It can be removed once PrimeFaces is
 * upgraded to a version that de-duplicates resources itself.
 */
(function ($) {
    "use strict";
    if (!$ || !$.ajaxSettings || !$.ajaxSettings.converters || !$.ajaxSettings.converters["text xml"]) {
        return;
    }

    var RESOURCE_UPDATE_IDS = ["jakarta.faces.Resource", "javax.faces.Resource"];

    // Compare resources by absolute URL, ignoring the ;jsessionid=... that URL rewriting can add.
    function keyOf(url) {
        try {
            var u = new URL(url, document.baseURI);
            return u.origin + u.pathname.replace(/;jsessionid=[^?#]*/i, "") + u.search;
        } catch (e) {
            return url;
        }
    }

    function loadedResourceKeys() {
        var keys = {};
        var els = document.querySelectorAll("script[src], link[href]");
        for (var i = 0; i < els.length; i++) {
            keys[keyOf(els[i].src || els[i].href)] = true;
        }
        return keys;
    }

    function removeLoadedResources(xml) {
        var updates = xml && xml.getElementsByTagName ? xml.getElementsByTagName("update") : [];
        for (var i = 0; i < updates.length; i++) {
            if (RESOURCE_UPDATE_IDS.indexOf(updates[i].getAttribute("id")) === -1) {
                continue;
            }
            // A <template> is inert: its scripts do not run and nothing is fetched while we edit it.
            var template = document.createElement("template");
            template.innerHTML = updates[i].textContent;
            var loaded = loadedResourceKeys();
            var tags = template.content.querySelectorAll("script[src], link[href]");
            for (var j = 0; j < tags.length; j++) {
                if (loaded[keyOf(tags[j].src || tags[j].href)]) {
                    tags[j].parentNode.removeChild(tags[j]);
                }
            }
            updates[i].textContent = template.innerHTML;
        }
        return xml;
    }

    var parseXML = $.ajaxSettings.converters["text xml"];
    $.ajaxSetup({
        converters: {
            "text xml": function (text) {
                return removeLoadedResources(parseXML(text));
            }
        }
    });
})(window.jQuery);
