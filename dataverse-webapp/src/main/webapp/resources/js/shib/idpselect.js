// created from http://svn.shibboleth.net/view/js-embedded-discovery?view=revision&revision=110 by pdurbin to support showListFirst per http://shibboleth.net/pipermail/users/2014-September/017355.html
/*
    http://www.JSON.org/json2.js
    2011-02-23

    Public Domain.

    NO WARRANTY EXPRESSED OR IMPLIED. USE AT YOUR OWN RISK.

    See http://www.JSON.org/js.html


    This code should be minified before deployment.
    See http://javascript.crockford.com/jsmin.html

    USE YOUR OWN COPY. IT IS EXTREMELY UNWISE TO LOAD CODE FROM SERVERS YOU DO
    NOT CONTROL.


    This file creates a global JSON object containing two methods: stringify
    and parse.

        JSON.stringify(value, replacer, space)
            value       any JavaScript value, usually an object or array.

            replacer    an optional parameter that determines how object
                        values are stringified for objects. It can be a
                        function or an array of strings.

            space       an optional parameter that specifies the indentation
                        of nested structures. If it is omitted, the text will
                        be packed without extra whitespace. If it is a number,
                        it will specify the number of spaces to indent at each
                        level. If it is a string (such as '\t' or '&nbsp;'),
                        it contains the characters used to indent at each level.

            This method produces a JSON text from a JavaScript value.

            When an object value is found, if the object contains a toJSON
            method, its toJSON method will be called and the result will be
            stringified. A toJSON method does not serialize: it returns the
            value represented by the name/value pair that should be serialized,
            or undefined if nothing should be serialized. The toJSON method
            will be passed the key associated with the value, and this will be
            bound to the value

            For example, this would serialize Dates as ISO strings.

                Date.prototype.toJSON = function (key) {
                    function f(n) {
                        // Format integers to have at least two digits.
                        return n < 10 ? '0' + n : n;
                    }

                    return this.getUTCFullYear()   + '-' +
                         f(this.getUTCMonth() + 1) + '-' +
                         f(this.getUTCDate())      + 'T' +
                         f(this.getUTCHours())     + ':' +
                         f(this.getUTCMinutes())   + ':' +
                         f(this.getUTCSeconds())   + 'Z';
                };

            You can provide an optional replacer method. It will be passed the
            key and value of each member, with this bound to the containing
            object. The value that is returned from your method will be
            serialized. If your method returns undefined, then the member will
            be excluded from the serialization.

            If the replacer parameter is an array of strings, then it will be
            used to select the members to be serialized. It filters the results
            such that only members with keys listed in the replacer array are
            stringified.

            Values that do not have JSON representations, such as undefined or
            functions, will not be serialized. Such values in objects will be
            dropped; in arrays they will be replaced with null. You can use
            a replacer function to replace those with JSON values.
            JSON.stringify(undefined) returns undefined.

            The optional space parameter produces a stringification of the
            value that is filled with line breaks and indentation to make it
            easier to read.

            If the space parameter is a non-empty string, then that string will
            be used for indentation. If the space parameter is a number, then
            the indentation will be that many spaces.

            Example:

            text = JSON.stringify(['e', {pluribus: 'unum'}]);
            // text is '["e",{"pluribus":"unum"}]'


            text = JSON.stringify(['e', {pluribus: 'unum'}], null, '\t');
            // text is '[\n\t"e",\n\t{\n\t\t"pluribus": "unum"\n\t}\n]'

            text = JSON.stringify([new Date()], function (key, value) {
                return this[key] instanceof Date ?
                    'Date(' + this[key] + ')' : value;
            });
            // text is '["Date(---current time---)"]'


        JSON.parse(text, reviver)
            This method parses a JSON text to produce an object or array.
            It can throw a SyntaxError exception.

            The optional reviver parameter is a function that can filter and
            transform the results. It receives each of the keys and values,
            and its return value is used instead of the original value.
            If it returns what it received, then the structure is not modified.
            If it returns undefined then the member is deleted.

            Example:

            // Parse the text. Values that look like ISO date strings will
            // be converted to Date objects.

            myData = JSON.parse(text, function (key, value) {
                var a;
                if (typeof value === 'string') {
                    a =
/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*)?)Z$/.exec(value);
                    if (a) {
                        return new Date(Date.UTC(+a[1], +a[2] - 1, +a[3], +a[4],
                            +a[5], +a[6]));
                    }
                }
                return value;
            });

            myData = JSON.parse('["Date(09/09/2001)"]', function (key, value) {
                var d;
                if (typeof value === 'string' &&
                        value.slice(0, 5) === 'Date(' &&
                        value.slice(-1) === ')') {
                    d = new Date(value.slice(5, -1));
                    if (d) {
                        return d;
                    }
                }
                return value;
            });


    This is a reference implementation. You are free to copy, modify, or
    redistribute.
*/

/*jslint evil: true, strict: false, regexp: false */

/*members "", "\b", "\t", "\n", "\f", "\r", "\"", JSON, "\\", apply,
    call, charCodeAt, getUTCDate, getUTCFullYear, getUTCHours,
    getUTCMinutes, getUTCMonth, getUTCSeconds, hasOwnProperty, join,
    lastIndex, length, parse, prototype, push, replace, slice, stringify,
    test, toJSON, toString, valueOf
*/


// Create a JSON object only if one does not already exist. We create the
// methods in a closure to avoid creating global variables.

var JSON;
if (!JSON) {
    JSON = {};
}

(function () {
    "use strict";

    function f(n) {
        // Format integers to have at least two digits.
        return n < 10 ? '0' + n : n;
    }

    if (typeof Date.prototype.toJSON !== 'function') {

        Date.prototype.toJSON = function (key) {

            return isFinite(this.valueOf()) ?
                this.getUTCFullYear()     + '-' +
                f(this.getUTCMonth() + 1) + '-' +
                f(this.getUTCDate())      + 'T' +
                f(this.getUTCHours())     + ':' +
                f(this.getUTCMinutes())   + ':' +
                f(this.getUTCSeconds())   + 'Z' : null;
        };

        String.prototype.toJSON      =
            Number.prototype.toJSON  =
            Boolean.prototype.toJSON = function (key) {
                return this.valueOf();
            };
    }

    var cx = /[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
        escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
        gap,
        indent,
        meta = {    // table of character substitutions
            '\b': '\\b',
            '\t': '\\t',
            '\n': '\\n',
            '\f': '\\f',
            '\r': '\\r',
            '"' : '\\"',
            '\\': '\\\\'
        },
        rep;


    function quote(string) {

// If the string contains no control characters, no quote characters, and no
// backslash characters, then we can safely slap some quotes around it.
// Otherwise we must also replace the offending characters with safe escape
// sequences.

        escapable.lastIndex = 0;
        return escapable.test(string) ? '"' + string.replace(escapable, function (a) {
            var c = meta[a];
            return typeof c === 'string' ? c :
                '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
        }) + '"' : '"' + string + '"';
    }


    function str(key, holder) {

// Produce a string from holder[key].

        var i,          // The loop counter.
            k,          // The member key.
            v,          // The member value.
            length,
            mind = gap,
            partial,
            value = holder[key];

// If the value has a toJSON method, call it to obtain a replacement value.

        if (value && typeof value === 'object' &&
                typeof value.toJSON === 'function') {
            value = value.toJSON(key);
        }

// If we were called with a replacer function, then call the replacer to
// obtain a replacement value.

        if (typeof rep === 'function') {
            value = rep.call(holder, key, value);
        }

// What happens next depends on the value's type.

        switch (typeof value) {
        case 'string':
            return quote(value);

        case 'number':

// JSON numbers must be finite. Encode non-finite numbers as null.

            return isFinite(value) ? String(value) : 'null';

        case 'boolean':
        case 'null':

// If the value is a boolean or null, convert it to a string. Note:
// typeof null does not produce 'null'. The case is included here in
// the remote chance that this gets fixed someday.

            return String(value);

// If the type is 'object', we might be dealing with an object or an array or
// null.

        case 'object':

// Due to a specification blunder in ECMAScript, typeof null is 'object',
// so watch out for that case.

            if (!value) {
                return 'null';
            }

// Make an array to hold the partial results of stringifying this object value.

            gap += indent;
            partial = [];

// Is the value an array?

            if (Object.prototype.toString.apply(value) === '[object Array]') {

// The value is an array. Stringify every element. Use null as a placeholder
// for non-JSON values.

                length = value.length;
                for (i = 0; i < length; i += 1) {
                    partial[i] = str(i, value) || 'null';
                }

// Join all of the elements together, separated with commas, and wrap them in
// brackets.

                v = partial.length === 0 ? '[]' : gap ?
                    '[\n' + gap + partial.join(',\n' + gap) + '\n' + mind + ']' :
                    '[' + partial.join(',') + ']';
                gap = mind;
                return v;
            }

// If the replacer is an array, use it to select the members to be stringified.

            if (rep && typeof rep === 'object') {
                length = rep.length;
                for (i = 0; i < length; i += 1) {
                    if (typeof rep[i] === 'string') {
                        k = rep[i];
                        v = str(k, value);
                        if (v) {
                            partial.push(quote(k) + (gap ? ': ' : ':') + v);
                        }
                    }
                }
            } else {

// Otherwise, iterate through all of the keys in the object.

                for (k in value) {
                    if (Object.prototype.hasOwnProperty.call(value, k)) {
                        v = str(k, value);
                        if (v) {
                            partial.push(quote(k) + (gap ? ': ' : ':') + v);
                        }
                    }
                }
            }

// Join all of the member texts together, separated with commas,
// and wrap them in braces.

            v = partial.length === 0 ? '{}' : gap ?
                '{\n' + gap + partial.join(',\n' + gap) + '\n' + mind + '}' :
                '{' + partial.join(',') + '}';
            gap = mind;
            return v;
        }
    }

// If the JSON object does not yet have a stringify method, give it one.

    if (typeof JSON.stringify !== 'function') {
        JSON.stringify = function (value, replacer, space) {

// The stringify method takes a value and an optional replacer, and an optional
// space parameter, and returns a JSON text. The replacer can be a function
// that can replace values, or an array of strings that will select the keys.
// A default replacer method can be provided. Use of the space parameter can
// produce text that is more easily readable.

            var i;
            gap = '';
            indent = '';

// If the space parameter is a number, make an indent string containing that
// many spaces.

            if (typeof space === 'number') {
                for (i = 0; i < space; i += 1) {
                    indent += ' ';
                }

// If the space parameter is a string, it will be used as the indent string.

            } else if (typeof space === 'string') {
                indent = space;
            }

// If there is a replacer, it must be a function or an array.
// Otherwise, throw an error.

            rep = replacer;
            if (replacer && typeof replacer !== 'function' &&
                    (typeof replacer !== 'object' ||
                    typeof replacer.length !== 'number')) {
                throw new Error('JSON.stringify');
            }

// Make a fake root object containing our value under the key of ''.
// Return the result of stringifying the value.

            return str('', {'': value});
        };
    }


// If the JSON object does not yet have a parse method, give it one.

    if (typeof JSON.parse !== 'function') {
        JSON.parse = function (text, reviver) {

// The parse method takes a text and an optional reviver function, and returns
// a JavaScript value if the text is a valid JSON text.

            var j;

            function walk(holder, key) {

// The walk method is used to recursively walk the resulting structure so
// that modifications can be made.

                var k, v, value = holder[key];
                if (value && typeof value === 'object') {
                    for (k in value) {
                        if (Object.prototype.hasOwnProperty.call(value, k)) {
                            v = walk(value, k);
                            if (v !== undefined) {
                                value[k] = v;
                            } else {
                                delete value[k];
                            }
                        }
                    }
                }
                return reviver.call(holder, key, value);
            }


// Parsing happens in four stages. In the first stage, we replace certain
// Unicode characters with escape sequences. JavaScript handles many characters
// incorrectly, either silently deleting them, or treating them as line endings.

            text = String(text);
            cx.lastIndex = 0;
            if (cx.test(text)) {
                text = text.replace(cx, function (a) {
                    return '\\u' +
                        ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
                });
            }

// In the second stage, we run the text against regular expressions that look
// for non-JSON patterns. We are especially concerned with '()' and 'new'
// because they can cause invocation, and '=' because it can cause mutation.
// But just to be safe, we want to reject all unexpected forms.

// We split the second stage into 4 regexp operations in order to work around
// crippling inefficiencies in IE's and Safari's regexp engines. First we
// replace the JSON backslash pairs with '@' (a non-JSON character). Second, we
// replace all simple value tokens with ']' characters. Third, we delete all
// open brackets that follow a colon or comma or that begin the text. Finally,
// we look to see that the remaining characters are only whitespace or ']' or
// ',' or ':' or '{' or '}'. If that is so, then the text is safe for eval.

            if (/^[\],:{}\s]*$/
                    .test(text.replace(/\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g, '@')
                        .replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g, ']')
                        .replace(/(?:^|:|,)(?:\s*\[)+/g, ''))) {

// In the third stage we use the eval function to compile the text into a
// JavaScript structure. The '{' operator is subject to a syntactic ambiguity
// in JavaScript: it can begin a block or an object literal. We wrap the text
// in parens to eliminate the ambiguity.

                j = eval('(' + text + ')');

// In the optional fourth stage, we recursively walk the new structure, passing
// each name/value pair to a reviver function for possible transformation.

                return typeof reviver === 'function' ?
                    walk({'': j}, '') : j;
            }

// If the text is not JSON parseable, then a SyntaxError is thrown.

            throw new SyntaxError('JSON.parse');
        };
    }
}());

function TypeAheadControl(jsonObj, box, orig, submit, maxchars, getName, getEntityId, geticon, ie6hack, alwaysShow, maxResults, getKeywords)
{
    //
    // Squirrel away the parameters we were given
    //
    this.elementList = jsonObj;
    this.textBox = box;
    this.origin = orig;
    this.submit = submit;
    this.results = 0;
    this.alwaysShow = alwaysShow;
    this.maxResults = maxResults;
    this.ie6hack = ie6hack;
    this.maxchars = maxchars;
    this.getName = getName;
    this.getEntityId = getEntityId;
    this.geticon = geticon;
    this.getKeywords = getKeywords;
}

TypeAheadControl.prototype.draw = function(setFocus) {

    //
    // Make a closure on this so that the embedded functions
    // get access to it.
    //
    var myThis = this;

    //
    // Set up the 'dropDown'
    //
    this.dropDown = document.createElement('ul');
    this.dropDown.className = 'IdPSelectDropDown';
    this.dropDown.style.visibility = 'hidden';

    // this.dropDown.style.width = this.textBox.offsetWidth;
    this.dropDown.current = -1;
    // document.body.appendChild(this.dropDown);
    document.getElementById('idpSelectIdPEntryTile').appendChild(this.dropDown);

    //
    // mouse listeners for the dropdown box
    //
    this.dropDown.onmouseover = function(event) {
        if (!event) {
            event = window.event;
        }
        var target;
        if (event.target){
            target = event.target;
        }
        if (typeof target == 'undefined') {
            target = event.srcElement;
        }
        myThis.select(target);
    };

    this.dropDown.onmousedown = function(event) {
        if (-1 != myThis.dropDown.current) {
            myThis.textBox.value = myThis.results[myThis.dropDown.current][0];
        }
    };

    //
    // Add the listeners to the text box
    //
    this.textBox.onkeyup = function(event) {
        //
        // get window event if needed (because of browser oddities)
        //
        if (!event) {
            event = window.event;
        }
        myThis.handleKeyUp(event);
    };

    this.textBox.onkeydown = function(event) {
        if (!event) {
            event = window.event;
        }

        myThis.handleKeyDown(event);
    };

    this.textBox.onblur = function() {
        myThis.hideDrop();
    };

    this.textBox.onfocus = function() {
        myThis.handleChange();
    };

    if (null == setFocus || setFocus) {
        this.textBox.focus();
    }
};

//
// Given a name return the first maxresults, or all possibles
//
TypeAheadControl.prototype.getPossible = function(name) {
    var possibles = [];
    var inIndex = 0;
    var outIndex = 0;
    var strIndex = 0;
    var str;
    var ostr;

    name = name.toLowerCase();

    while (outIndex <= this.maxResults && inIndex < this.elementList.length) {
        var hit = false;
        var thisName = this.getName(this.elementList[inIndex]);

        //
        // Check name
        //
        if (thisName.toLowerCase().indexOf(name) != -1) {
            hit = true;
        }
        //
        // Check entityID
        //
        if (!hit && this.getEntityId(this.elementList[inIndex]).toLowerCase().indexOf(name) != -1) {
            hit = true;
        }

        if (!hit) {
            var thisKeywords = this.getKeywords(this.elementList[inIndex]);
            if (null != thisKeywords &&
                thisKeywords.toLowerCase().indexOf(name) != -1) {
                hit = true;
            }
        }

        if (hit) {
            possibles[outIndex] = [thisName, this.getEntityId(this.elementList[inIndex]), this.geticon(this.elementList[inIndex])];
            outIndex ++;
        }

        inIndex ++;
    }
    //
    // reset the cursor to the top
    //
    this.dropDown.current = -1;

    return possibles;
};

TypeAheadControl.prototype.handleKeyUp = function(event) {
    var key = event.keyCode;

    if (27 == key) {
        //
        // Escape - clear
        //
        this.textBox.value = '';
        this.handleChange();
    } else if (8 == key || 32 == key || (key >= 46 && key < 112) || key > 123) {
        //
        // Backspace, Space and >=Del to <F1 and > F12
        //
        this.handleChange();
    }
};

TypeAheadControl.prototype.handleKeyDown = function(event) {

    var key = event.keyCode;

    if (38 == key) {
        //
        // up arrow
        //
        this.upSelect();

    } else if (40 == key) {
        //
        // down arrow
        //
        this.downSelect();
    }
};

TypeAheadControl.prototype.hideDrop = function() {
    var i = 0;
    if (null !== this.ie6hack) {
        while (i < this.ie6hack.length) {
            this.ie6hack[i].style.visibility = 'visible';
            i++;
        }
    }
    this.dropDown.style.visibility = 'hidden';

    if (-1 == this.dropDown.current) {
        this.doUnselected();
    }
};

TypeAheadControl.prototype.showDrop = function() {
    var i = 0;
    if (null !== this.ie6hack) {
        while (i < this.ie6hack.length) {
            this.ie6hack[i].style.visibility = 'hidden';
            i++;
        }
    }
    this.dropDown.style.visibility = 'visible';
};


TypeAheadControl.prototype.doSelected = function() {
    this.submit.disabled = false;
};

TypeAheadControl.prototype.doUnselected = function() {
    this.submit.disabled = true;
};

TypeAheadControl.prototype.handleChange = function() {

    var val = this.textBox.value;
    var res = this.getPossible(val);


    if (0 === val.length ||
        0 === res.length ||
        (!this.alwaysShow && this.maxResults < res.length)) {
        this.hideDrop();
        this.doUnselected();
        this.results = [];
        this.dropDown.current = -1;
    } else {
        this.results = res;
        this.populateDropDown(res);
        if (1 == res.length) {
            this.select(this.dropDown.childNodes[0]);
            this.doSelected();
        } else {
            this.doUnselected();
        }
    }
};

//
// A lot of the stuff below comes from
// http://www.webreference.com/programming/javascript/ncz/column2
//
// With thanks to Nicholas C Zakas
//
TypeAheadControl.prototype.populateDropDown = function(list) {
    this.dropDown.innerHTML = '';
    var i = 0;
    var div;
    var img;
    var str;

    while (i < list.length) {
        div = document.createElement('li');
        str = list[i][0];

	if (null !== list[i][2]) {

	    img = document.createElement('img');
	    img.src = list[i][2];
	    img.width = 16;
	    img.height = 16;
	    img.alt = '';
	    div.appendChild(img);
	    //
	    // trim string back further in this case
	    //
	    if (str.length > this.maxchars - 2) {
		str = str.substring(0, this.maxchars - 2);
	    }
	    str = ' ' + str;
	} else {
	    if (str.length > this.maxchars) {
		str = str.substring(0, this.maxchars);
	    }
	}
        div.appendChild(document.createTextNode(str));

        this.dropDown.appendChild(div);
        i++;
    }
    var off = this.getXY();
    // this.dropDown.style.left = off[0] + 'px';
    // this.dropDown.style.top = off[1] + 'px';
    this.showDrop();
};

TypeAheadControl.prototype.getXY = function() {

    var node = this.textBox;
    var sumX = 0;
    var sumY = node.offsetHeight;

    while(node.tagName != 'BODY') {
        sumX += node.offsetLeft;
        sumY += node.offsetTop;
        node = node.offsetParent;
    }
    //
    // And add in the offset for the Body
    //
    sumX += node.offsetLeft;
    sumY += node.offsetTop;

    return [sumX, sumY];
};

TypeAheadControl.prototype.select = function(selected) {
    var i = 0;
    var node;
    this.dropDown.current = -1;
    this.doUnselected();
    while (i < this.dropDown.childNodes.length) {
        node = this.dropDown.childNodes[i];
        if (node == selected) {
            //
            // Highlight it
            //
            node.className = 'IdPSelectCurrent';
            //
            // turn on the button
            //
            this.doSelected();
            //
            // setup the cursor
            //
            this.dropDown.current = i;
            //
            // and the value for the Server
            //
            this.origin.value = this.results[i][1];
            this.origin.textValue = this.results[i][0];
        } else {
            node.className = '';
        }
        i++;
    }
    this.textBox.focus();
};

TypeAheadControl.prototype.downSelect = function() {
    if (this.results.length > 0) {

        if (-1 == this.dropDown.current) {
            //
            // mimic a select()
            //
            this.dropDown.current = 0;
            this.dropDown.childNodes[0].className = 'IdPSelectCurrent';
            this.doSelected();
            this.origin.value = this.results[0][1];
            this.origin.textValue = this.results[0][0];

        } else if (this.dropDown.current < (this.results.length-1)) {
            //
            // turn off highlight
            //
            this.dropDown.childNodes[this.dropDown.current].className = '';
            //
            // move cursor
            //
            this.dropDown.current++;
            //
            // and 'select'
            //
            this.dropDown.childNodes[this.dropDown.current].className = 'IdPSelectCurrent';
            this.doSelected();
            this.origin.value = this.results[this.dropDown.current][1];
            this.origin.textValue = this.results[this.dropDown.current][0];

        }
    }
};


TypeAheadControl.prototype.upSelect = function() {
    if ((this.results.length > 0) &&
        (this.dropDown.current > 0)) {

            //
            // turn off highlight
            //
            this.dropDown.childNodes[this.dropDown.current].className = '';
            //
            // move cursor
            //
            this.dropDown.current--;
            //
            // and 'select'
            //
            this.dropDown.childNodes[this.dropDown.current].className = 'IdPSelectCurrent';
            this.doSelected();
            this.origin.value = this.results[this.dropDown.current][1];
            this.origin.textValue = this.results[this.dropDown.current][0];
        }
};
function IdPSelectUI() {
    //
    // module locals
    //
    var idpData;
    var base64chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
    var idpSelectDiv;
    var lang;
    var majorLang;
    var defaultLang;
    var langBundle;
    var defaultLangBundle;
    var defaultLogo;
    var defaultLogoWidth;
    var defaultLogoHeight;
    var minWidth;
    var minHeight;
    var maxWidth;
    var maxHeight;
    var bestRatio;

    //
    // Parameters passed into our closure
    //
    var preferredIdP;
    var maxPreferredIdPs;
    var helpURL;
    var ie6Hack;
    var samlIdPCookieTTL;
    var maxIdPCharsDropDown;
    var maxIdPCharsButton;
    var maxIdPCharsAltTxt;
    var alwaysShow;
    var maxResults;
    var ignoreKeywords;
    var showListFirst;

    //
    // The cookie contents
    //
    var userSelectedIdPs;
    //
    // Anchors used inside autofunctions
    //
    var idpEntryDiv;
    var idpListDiv;
    var idpSelect;
    var listButton;

    //
    // local configuration
    //
    var idPrefix = 'idpSelect';
    var classPrefix = 'IdPSelect';
    var dropDownControl;

    //
    // DS protocol configuration
    //
    var returnString = '';
    var returnBase='';
    var returnParms= [];
    var returnIDParam = 'entityID';

    // *************************************
    // Public functions
    // *************************************

    /**
       Draws the IdP Selector UI on the screen.  This is the main
       method for the IdPSelectUI class.
    */
    this.draw = function(parms){

        if (!setupLocals(parms)) {
            return;
        }

        idpSelectDiv = document.getElementById(parms.insertAtDiv);
        if(!idpSelectDiv){
            fatal(getLocalizedMessage('fatal.divMissing'));
            return;
        }

        if (!load(parms.dataSource)) {
            return;
        }
        stripHidden(parms.hiddenIdPs);

        idpData.sort(function(a,b) {return getLocalizedName(a).localeCompare(getLocalizedName(b));});

        var idpSelector = buildIdPSelector();
        idpSelectDiv.appendChild(idpSelector);
        dropDownControl.draw(parms.setFocusTextBox);
    } ;

    // *************************************
    // Private functions
    //
    // Data Manipulation
    //
    // *************************************

    /**
       Copies the "parameters" in the function into namesspace local
       variables.  This means most of the work is done outside the
       IdPSelectUI object
    */

    var setupLocals = function (paramsSupplied) {
        //
        // Copy parameters in
        //
        var suppliedEntityId;

        preferredIdP = paramsSupplied.preferredIdP;
        maxPreferredIdPs = paramsSupplied.maxPreferredIdPs;
        helpURL = paramsSupplied.helpURL;
        ie6Hack = paramsSupplied.ie6Hack;
        samlIdPCookieTTL = paramsSupplied.samlIdPCookieTTL;
        alwaysShow = paramsSupplied.alwaysShow;
        maxResults = paramsSupplied.maxResults;
        ignoreKeywords = paramsSupplied.ignoreKeywords;
        if (paramsSupplied.showListFirst) {
            showListFirst = paramsSupplied.showListFirst;
        } else {
            showListFirst = false;
        }
        defaultLogo = paramsSupplied.defaultLogo;
        defaultLogoWidth = paramsSupplied.defaultLogoWidth;
        defaultLogoHeight = paramsSupplied.defaultLogoHeight;
        minWidth = paramsSupplied.minWidth;
        minHeight = paramsSupplied.minHeight;
        maxWidth = paramsSupplied.maxWidth;
        maxHeight = paramsSupplied.maxHeight;
        bestRatio = paramsSupplied.bestRatio;
        maxIdPCharsButton = paramsSupplied.maxIdPCharsButton;
        maxIdPCharsDropDown = paramsSupplied.maxIdPCharsDropDown;
        maxIdPCharsAltTxt = paramsSupplied.maxIdPCharsAltTxt;

        if (typeof navigator == 'undefined') {
            lang = paramsSupplied.defaultLanguage;
        } else {
            lang = navigator.language || navigator.userLanguage || paramsSupplied.defaultLanguage;
        }
        if (lang.indexOf('-') > 0) {
            majorLang = lang.substring(0, lang.indexOf('-'));
        }

        defaultLang = paramsSupplied.defaultLanguage;

        if (typeof paramsSupplied.langBundles[lang] != 'undefined') {
            langBundle = paramsSupplied.langBundles[lang];
        } else if (typeof majorLang != 'undefined' && typeof paramsSupplied.langBundles[majorLang] != 'undefined') {
            langBundle = paramsSupplied.langBundles[majorLang];
        }
        defaultLangBundle = paramsSupplied.langBundles[paramsSupplied.defaultLanguage];

        //
        // Setup Language bundles
        //
        if (!defaultLangBundle) {
            fatal('No languages work');
            return false;
        }
        if (!langBundle) {
            debug('No language support for ' + lang);
            langBundle = defaultLangBundle;
        }

        if (paramsSupplied.testGUI) {
            //
            // no policing of parms
            //
            return true;
        }
        //
        // Now set up the return values from the URL
        //
        var policy = 'urn:oasis:names:tc:SAML:profiles:SSO:idpdiscovery-protocol:single';
        var i;
        var isPassive = false;
        var parms;
        var parmPair;
        var win = window;
        while (null !== win.parent && win !== win.parent) {
            win = win.parent;
        }
        var loc = win.location;
        var parmlist = loc.search;

        //
        // No parameters, so just collect the defaults:
        // (we've made a small change to the original code here;
        // we are now *always* setting these default values,
        // wheather the URL has any parameters or not.
        // the way it was working before, if there were any
        // ?... parameters, the defaults were not getting set -
        // so the code was bombing, because we are not supplying
        // them as URL parameters either -- L.A. Nov. 13 2014)
        //
        suppliedEntityId  = paramsSupplied.myEntityID;
        returnString = paramsSupplied.defaultReturn;
        if (null != paramsSupplied.defaultReturnIDParam) {
           returnIDParam = paramsSupplied.defaultReturnIDParam;
        }

        if (null == parmlist || 0 == parmlist.length || parmlist.charAt(0) != '?') {

            if (null == paramsSupplied.defaultReturn) {

                fatal(getLocalizedMessage('fatal.noparms'));
                return false;
            }

        } else {
            parmlist = parmlist.substring(1);

            //
            // protect against various hideousness by decoding. We re-encode just before we push
            //

            parms = parmlist.split('&');
            if (parms.length === 0) {

                fatal(getLocalizedMessage('fatal.noparms'));
                return false;
            }

            for (i = 0; i < parms.length; i++) {
                parmPair = parms[i].split('=');
                if (parmPair.length != 2) {
                    continue;
                }
                if (parmPair[0] == 'entityID') {
                    suppliedEntityId = decodeURIComponent(parmPair[1]);
                } else if (parmPair[0] == 'return') {
                    returnString = decodeURIComponent(parmPair[1]);
                } else if (parmPair[0] == 'returnIDParam') {
                    returnIDParam = decodeURIComponent(parmPair[1]);
                } else if (parmPair[0] == 'policy') {
                    policy = decodeURIComponent(parmPair[1]);
                } else if (parmPair[0] == 'isPassive') {
                    isPassive = (parmPair[1].toUpperCase() == "TRUE");
                }
            }
        }
        if (policy != 'urn:oasis:names:tc:SAML:profiles:SSO:idpdiscovery-protocol:single') {
            fatal(getLocalizedMessage('fatal.wrongProtocol'));
            return false;
        }
        if (paramsSupplied.myEntityID !== null && paramsSupplied.myEntityID != suppliedEntityId) {
            fatal(getLocalizedMessage('fatal.wrongEntityId') + '"' + suppliedEntityId + '" != "' + paramsSupplied.myEntityID + '"');
            return false;
        }
        if (null === returnString || returnString.length === 0) {
            fatal(getLocalizedMessage('fatal.noReturnURL'));
            return false;
        }
        if (!validProtocol(returnString)) {
            fatal(getLocalizedMessage('fatal.badProtocol'));
            return false;
        }

        //
        // isPassive
        //
        if (isPassive) {
            var prefs = retrieveUserSelectedIdPs();
            if (prefs.length == 0) {
                //
                // no preference, go back
                //
                location.href = returnString;
                return false;
            } else {
                var retString = returnIDParam + '=' + encodeURIComponent(prefs[0]);
                //
                // Compose up the URL
                //
                if (returnString.indexOf('?') == -1) {
                    retString = '?' + retString;
                } else {
                    retString = '&' + retString;
                }
                location.href = returnString + retString;
                return false;
            }
        }

        //
        // Now split up returnString
        //
        i = returnString.indexOf('?');
        if (i < 0) {
            returnBase = returnString;
            return true;
        }
        returnBase = returnString.substring(0, i);
        parmlist = returnString.substring(i+1);
        parms = parmlist.split('&');
        for (i = 0; i < parms.length; i++) {
            parmPair = parms[i].split('=');
            if (parmPair.length != 2) {
                continue;
            }
            parmPair[1] = decodeURIComponent(parmPair[1]);
            returnParms.push(parmPair);
        }
        return true;
    };

    /**
       Strips the supllied IdP list from the idpData
    */
    var stripHidden = function(hiddenList) {

        if (null == hiddenList || 0 == hiddenList.length) {
            return;
        }
        var i;
        var j;
        for (i = 0; i < hiddenList.length; i++) {
            for (j = 0; j < idpData.length; j++) {
                if (getEntityId(idpData[j]) == hiddenList[i]) {
                    idpData.splice(j, 1);
                    break;
                }
            }
        }
    }


    /**
     * Strip the "protocol://host" bit out of the URL and check the protocol
     * @param the URL to process
     * @return whether it starts with http: or https://
     */

    var validProtocol = function(s) {
        if (null === s) {
            return false;
        }
        var marker = "://";
        var protocolEnd = s.indexOf(marker);
        if (protocolEnd < 0) {
            return false;
        }
        s = s.substring(0, protocolEnd);
        if (s == "http" || s== "https") {
            return true;
        }
        return false;
    };

    /**
     * We need to cache bust on IE.  So how do we know?  Use a bigger hammer.
     */
    var isIE = function() {
        if (null == navigator) {
            return false;
        }
        var browserName = navigator.appName;
        if (null == browserName) {
            return false;
        }
        return (browserName == 'Microsoft Internet Explorer') ;
    } ;


    /**
       Loads the data used by the IdP selection UI.  Data is loaded
       from a JSON document fetched from the given url.

       @param {Function} failureCallback A function called if the JSON
       document can not be loaded from the source.  This function will
       passed the {@link XMLHttpRequest} used to request the JSON data.
    */
    var load = function(dataSource){
        var xhr = null;

        try {
            xhr = new XMLHttpRequest();
        } catch (e) {}
        if (null == xhr) {
            //
            // EDS24. try to get 'Microsoft.XMLHTTP'
            //
            try {
                xhr = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (e) {}
        }
        if (null == xhr) {
            //
            // EDS35. try to get 'Microsoft.XMLHTTP'
            //
            try {
                xhr = new  ActiveXObject('MSXML2.XMLHTTP.3.0');
            } catch (e) {}
        }
        if (null == xhr) {
            fatal(getLocalizedMessage('fatal.noXMLHttpRequest'));
            return false;
        }

        if (isIE()) {
            //
            // cache bust (for IE)
            //
            dataSource += '?random=' + (Math.random()*1000000);
        }

        //
        // Grab the data
        //
        xhr.open('GET', dataSource, false);
        if (typeof xhr.overrideMimeType == 'function') {
            xhr.overrideMimeType('application/json');
        }
        xhr.send(null);

        if(xhr.status == 200){
            //
            // 200 means we got it OK from as web source
            // if locally loading its 0.  Go figure
            //
            var jsonData = xhr.responseText;
            if(jsonData === null){
                fatal(getLocalizedMessage('fatal.noData'));
                return false;
            }

            //
            // Parse it
            //

            idpData = JSON.parse(jsonData);

        }else{
            fatal(getLocalizedMessage('fatal.loadFailed') + dataSource + '.');
            return false;
        }
        return true;
    };

    /**
       Returns the idp object with the given name.

       @param (String) the name we are interested in
       @return (Object) the IdP we care about
    */

    var getIdPFor = function(idpName) {

        for (var i = 0; i < idpData.length; i++) {
            if (getEntityId(idpData[i]) == idpName) {
                return idpData[i];
            }
        }
        return null;
    };

    /**
       Returns a suitable image from the given IdP

       @param (Object) The IdP
       @return Object) a DOM object suitable for insertion

       TODO - rather more careful selection
    */

    var getImageForIdP = function(idp) {

        var getBestFit = function(language) {
            //
            // See GetLocalizedEntry
            //
            var bestFit = null;
            var i;
            if (null == idp.Logos) {
                return null;
            }
            for (i in idp.Logos) {
                if (idp.Logos[i].lang == language &&
                    idp.Logos[i].width != null &&
                    idp.Logos[i].width >= minWidth &&
                    idp.Logos[i].height != null &&
                    idp.Logos[i].height >= minHeight) {
                    if (bestFit === null) {
                        bestFit = idp.Logos[i];
                    } else {
                        me = Math.abs(bestRatio - Math.log(idp.Logos[i].width/idp.Logos[i].height));
                        him = Math.abs(bestRatio - Math.log(bestFit.width/bestFit.height));
                        if (him > me) {
                            bestFit = idp.Logos[i];
                        }
                    }
                }
            }
            return bestFit;
        } ;

        var bestFit = null;
        var img = document.createElement('img');

        bestFit = getBestFit(lang);
        if (null === bestFit && typeof majorLang != 'undefined') {
            bestFit = getBestFit(majorLang);
        }
        if (null === bestFit) {
            bestFit = getBestFit(null);
        }
        if (null === bestFit) {
            bestFit = getBestFit(defaultLang);
        }


        if (null === bestFit) {
            img.src = defaultLogo;
            img.width = defaultLogoWidth;
            img.height = defaultLogoHeight;
            img.alt = getLocalizedMessage('defaultLogoAlt');
            return img;
        }

        img.src = bestFit.value;
        var altTxt = getLocalizedName(idp);
        if (altTxt.length > maxIdPCharsAltTxt) {
            altTxt = altTxt.substring(0, maxIdPCharsAltTxt) + '...';
        }
        img.alt = altTxt;

        var w = bestFit.width;
        var h = bestFit.height;
        if (w>maxWidth) {
            h = (maxWidth/w) * h;
            w = maxWidth;
        }
        if (h> maxHeight) {
            w = (maxHeight/h) * w;
            w = maxHeight;
        }

        img.setAttribute('width', w);
        img.setAttribute('height', h);
        return img;
    };

    // *************************************
    // Private functions
    //
    // GUI Manipulation
    //
    // *************************************

    /**
       Builds the IdP selection UI.

       Three divs. PreferredIdPTime, EntryTile and DropdownTile

       @return {Element} IdP selector UI
    */
    var buildIdPSelector = function(){
        var containerDiv = buildDiv('IdPSelector');
        var preferredTileExists;
        preferredTileExists = buildPreferredIdPTile(containerDiv);
        buildIdPEntryTile(containerDiv, preferredTileExists);
        buildIdPDropDownListTile(containerDiv, preferredTileExists);
        return containerDiv;
    };

    /**
      Builds a button for the provided IdP
        <div class="preferredIdPButton">
          <a href="XYX" onclick=setparm('ABCID')>
            <div class=
            <img src="https:\\xyc.gif"> <!-- optional -->
            XYX Text
          </a>
        </div>

      @param (Object) The IdP

      @return (Element) preselector for the IdP
    */

    var composePreferredIdPButton = function(idp, uniq) {
        var div = buildDiv(undefined, 'PreferredIdPButton');
        var aval = document.createElement('a');
        var retString = returnIDParam + '=' + encodeURIComponent(getEntityId(idp));
        var retVal = returnString;
        var img = getImageForIdP(idp);
        //
        // Compose up the URL
        //
        if (retVal.indexOf('?') == -1) {
            retString = '?' + retString;
        } else {
            retString = '&' + retString;
        }
        aval.href = retVal + retString;
        aval.onclick = function () {
            selectIdP(getEntityId(idp));
        };
        var imgDiv=buildDiv(undefined, 'PreferredIdPImg');
        imgDiv.appendChild(img);
        aval.appendChild(imgDiv);

        var nameDiv = buildDiv(undefined, 'TextDiv');
        var nameStr = getLocalizedName(idp);
        if (nameStr.length > maxIdPCharsButton) {
            nameStr = nameStr.substring(0, maxIdPCharsButton) + '...';
        }
        div.title = nameStr;
        nameDiv.appendChild(document.createTextNode(nameStr));
        aval.appendChild(nameDiv);

        div.appendChild(aval);
        return div;
    };

    /**
     * Builds and populated a text Div
     */
    var buildTextDiv = function(parent, textId)
    {
        var div  = buildDiv(undefined, 'TextDiv');
        var introTxt = document.createTextNode(getLocalizedMessage(textId));
        div.appendChild(introTxt);
        parent.appendChild(div);
    } ;

    var setSelector = function (selector, selected) {
        if (null === selected || 0 === selected.length || '-' == selected.value) {
            return;
        }
        var i = 0;
        while (i < selector.options.length) {
            if (selector.options[i].value == selected) {
                selector.options[i].selected = true;
                break;
            }
            i++;
        }
    }

    /**
       Builds the preferred IdP selection UI (top half of the UI w/ the
       IdP buttons)

       <div id=prefix+"PreferredIdPTile">
          <div> [see comprosePreferredIdPButton </div>
          [repeated]
       </div>

       @return {Element} preferred IdP selection UI
    */
    var buildPreferredIdPTile = function(parentDiv) {

        var preferredIdPs = getPreferredIdPs();
        if (0 === preferredIdPs.length) {
            return false;
        }

        var preferredIdPDIV = buildDiv('PreferredIdPTile');

        // buildTextDiv(preferredIdPDIV, 'idpPreferred.label');

        for(var i = 0 ; i < maxPreferredIdPs && i < preferredIdPs.length; i++){
            if (preferredIdPs[i]) {
                var button = composePreferredIdPButton(preferredIdPs[i],i);
                preferredIdPDIV.appendChild(button);
            }
        }

        parentDiv.appendChild(preferredIdPDIV);
        return true;
    };

    /**
     * Build the <form> from the return parameters
     */

    var buildSelectForm = function ()
    {
        var form = document.createElement('form');
        idpEntryDiv.appendChild(form);

        form.action = returnBase;
        form.method = 'GET';
        form.setAttribute('autocomplete', 'OFF');
        var i = 0;
        for (i = 0; i < returnParms.length; i++) {
            var hidden = document.createElement('input');
            hidden.setAttribute('type', 'hidden');
            hidden.name = returnParms[i][0];
            hidden.value= returnParms[i][1];
            form.appendChild(hidden);
        }

        return form;
    } ;


    /**
       Build the manual IdP Entry tile (bottom half of UI with
       search-as-you-type field).

       <div id = prefix+"IdPEntryTile">
         <form>
           <input type="text", id=prefix+"IdPSelectInput/> // select text box
           <input type="hidden" /> param to send
           <input type="submit" />


       @return {Element} IdP entry UI tile
    */
    var buildIdPEntryTile = function(parentDiv, preferredTile) {


        idpEntryDiv = buildDiv('IdPEntryTile');
        if (showListFirst) {
            idpEntryDiv.style.display = 'none';
        }

        //if (preferredTile) {
            //buildTextDiv(idpEntryDiv, 'idpEntry.label');
        //} else {
            // buildTextDiv(idpEntryDiv, 'idpEntry.NoPreferred.label');
        //}

        var form = buildSelectForm();

        var textInput = document.createElement('input');
        form.appendChild(textInput);

        textInput.type='text';
        setID(textInput, 'Input');

        var hidden = document.createElement('input');
        hidden.setAttribute('type', 'hidden');
        form.appendChild(hidden);

        hidden.name = returnIDParam;
        hidden.value='-';

        var button = buildContinueButton('Select');
        button.disabled = true;
        form.appendChild(button);

        form.onsubmit = function () {
            //
            // Make sure we cannot ask for garbage
            //
            if (null === hidden.value || 0 === hidden.value.length || '-' == hidden.value) {
                return false;
            }
            //
            // And always ask for the cookie to be updated before we continue
            //
            textInput.value = hidden.textValue;
            selectIdP(hidden.value);
            return true;
        };

        dropDownControl = new TypeAheadControl(idpData, textInput, hidden, button, maxIdPCharsDropDown, getLocalizedName, getEntityId, geticon, ie6Hack, alwaysShow, maxResults, getKeywords);

        var a = document.createElement('a');
        a.appendChild(document.createTextNode(getLocalizedMessage('idpList.showList')));
        a.href = '#';
        setClass(a, 'DropDownToggle');
        a.onclick = function() {
            idpEntryDiv.style.display='none';
            setSelector(idpSelect, hidden.value);
            idpListDiv.style.display='block';
            listButton.focus();
        };
        idpEntryDiv.appendChild(a);
        buildHelpText(idpEntryDiv);

        parentDiv.appendChild(idpEntryDiv);
    };

    /**
       Builds the drop down list containing all the IdPs from which a
       user may choose.

       <div id=prefix+"IdPListTile">
          <label for="idplist">idpList.label</label>
          <form action="URL from IDP Data" method="GET">
          <select name="param from IdP data">
             <option value="EntityID">Localized Entity Name</option>
             [...]
          </select>
          <input type="submit"/>
       </div>

       @return {Element} IdP drop down selection UI tile
    */
    var buildIdPDropDownListTile = function(parentDiv, preferredTile) {
        idpListDiv = buildDiv('IdPListTile');
        if (!showListFirst) {
            idpListDiv.style.display = 'none';
        }

        //if (preferredTile) {
            // buildTextDiv(idpListDiv, 'idpList.label');
        //} else {
            // buildTextDiv(idpListDiv, 'idpList.NoPreferred.label');
        //}

        idpSelect = document.createElement('select');
        setID(idpSelect, 'Selector');
        idpSelect.name = returnIDParam;
        idpListDiv.appendChild(idpSelect);

        var idpOption = buildSelectOption('-', getLocalizedMessage('idpList.defaultOptionLabel'));
        idpOption.selected = true;

        idpSelect.appendChild(idpOption);

        var idp;
        for(var i=0; i<idpData.length; i++){
            idp = idpData[i];
            idpOption = buildSelectOption(getEntityId(idp), getLocalizedName(idp));
            idpSelect.appendChild(idpOption);
        }

        var form = buildSelectForm();

        form.appendChild(idpSelect);

        form.onsubmit = function () {
            //
            // The first entery isn't selectable
            //
            if (idpSelect.selectedIndex < 1) {
                return false;
            }
            //
            // otherwise update the cookie
            //
            selectIdP(idpSelect.options[idpSelect.selectedIndex].value);
            return true;
        };

        var button = buildContinueButton('List');
        listButton = button;
        form.appendChild(button);

        idpListDiv.appendChild(form);

        //
        // The switcher
        //
        var a = document.createElement('a');
        a.appendChild(document.createTextNode(getLocalizedMessage('idpList.showSearch')));
        a.href = '#';
        setClass(a, 'DropDownToggle');
        a.onclick = function() {
            idpEntryDiv.style.display='block';
            idpListDiv.style.display='none';
        };
        idpListDiv.appendChild(a);
        buildHelpText(idpListDiv);

        parentDiv.appendChild(idpListDiv);
    };

    /**
       Builds the 'continue' button used to submit the IdP selection.

       @return {Element} HTML button used to submit the IdP selection
    */
    var buildContinueButton = function(which) {
        var button  = document.createElement('input');
        button.setAttribute('type', 'submit');
        button.value = getLocalizedMessage('submitButton.label');
        setID(button, which + 'Button');

        return button;
    };

    /**
       Builds an aref to point to the helpURL
    */

    var buildHelpText = function(containerDiv) {
        var aval = document.createElement('a');
        aval.href = helpURL;
        aval.appendChild(document.createTextNode(getLocalizedMessage('helpText')));
        setClass(aval, 'HelpButton');
//        containerDiv.appendChild(aval);
    } ;

    /**
       Creates a div element whose id attribute is set to the given ID.

       @param {String} id ID for the created div element
       @param {String} [class] class of the created div element
       @return {Element} DOM 'div' element with an 'id' attribute
    */
    var buildDiv = function(id, whichClass){
        var div = document.createElement('div');
        if (undefined !== id) {
            setID(div, id);
        }
        if(undefined !== whichClass) {

            setClass(div, whichClass);
        }
        return div;
    };

    /**
       Builds an HTML select option element

       @param {String} value value of the option when selected
       @param {String} label displayed label of the option
    */
    var buildSelectOption = function(value, text){
        var option = document.createElement('option');
        option.value = value;
        if (text.length > maxIdPCharsDropDown) {
            text = text.substring(0, maxIdPCharsDropDown);
        }
        option.appendChild(document.createTextNode(text));
        return option;
    };

    /**
       Sets the attribute 'id' on the provided object
       We do it through this function so we have a single
       point where we can prepend a value

       @param (Object) The [DOM] Object we want to set the attribute on
       @param (String) The Id we want to set
    */

    var setID = function(obj, name) {
        obj.id = idPrefix + name;
    };

    var setClass = function(obj, name) {
        obj.setAttribute('class', classPrefix + name);
    };

    /**
       Returns the DOM object with the specified id.  We abstract
       through a function to allow us to prepend to the name

       @param (String) the (unprepended) id we want
    */
    var locateElement = function(name) {
        return document.getElementById(idPrefix + name);
    };

    // *************************************
    // Private functions
    //
    // GUI actions.  Note that there is an element of closure going on
    // here since these names are invisible outside this module.
    //
    //
    // *************************************

    /**
     * Base helper function for when an IdP is selected
     * @param (String) The UN-encoded entityID of the IdP
    */

    var selectIdP = function(idP) {
        updateSelectedIdPs(idP);
        saveUserSelectedIdPs(userSelectedIdPs);
    };

    // *************************************
    // Private functions
    //
    // Localization handling
    //
    // *************************************

    /**
       Gets a localized string from the given language pack.  This
       method uses the {@link langBundles} given during construction
       time.

       @param {String} messageId ID of the message to retrieve

       @return (String) the message
    */
    var getLocalizedMessage = function(messageId){

        var message = langBundle[messageId];
        if(!message){
            message = defaultLangBundle[messageId];
        }
        if(!message){
            message = 'Missing message for ' + messageId;
        }

        return message;
    };

    var getEntityId = function(idp) {
        return idp.entityID;
    };

    /**
       Returns the icon information for the provided idp

       @param (Object) an idp.  This should have an array 'names' with sub
        elements 'lang' and 'name'.

       @return (String) The localized name
    */
    var geticon = function(idp) {
        var i;

        if (null == idp.Logos) {
            return null;
        }
        for (i =0; i < idp.Logos.length; i++) {
	    var logo = idp.Logos[i];

	    if (logo.height == "16" && logo.width == "16") {
		if (null == logo.lang ||
		    lang == logo.lang ||
		    (typeof majorLang != 'undefined' && majorLang == logo.lang) ||
		    defaultLang == logo.lang) {
		    return logo.value;
		}
	    }
	}

	return null;
    } ;

    /**
       Returns the localized name information for the provided idp

       @param (Object) an idp.  This should have an array 'names' with sub
        elements 'lang' and 'name'.

       @return (String) The localized name
    */
    var getLocalizedName = function(idp) {
        var res = getLocalizedEntry(idp.DisplayNames);
        if (null !== res) {
            return res;
        }
        debug('No Name entry in any language for ' + getEntityId(idp));
        return getEntityId(idp);
    } ;

    var getKeywords = function(idp) {
        if (ignoreKeywords || null == idp.Keywords) {
            return null;
        }
        var s = getLocalizedEntry(idp.Keywords);

        return s;
    }

    var getLocalizedEntry = function(theArray){
        var i;

        //
        // try by full name
        //
        for (i in theArray) {
            if (theArray[i].lang == lang) {
                return theArray[i].value;
            }
        }
        //
        // then by major language
        //
        if (typeof majorLang != 'undefined') {
            for (i in theArray) {
                if (theArray[i].lang == majorLang) {
                    return theArray[i].value;
                }
            }
        }
        //
        // then by null language in metadata
        //
        for (i in theArray) {
            if (theArray[i].lang == null) {
                return theArray[i].value;
            }
        }

        //
        // then by default language
        //
        for (i in theArray) {
            if (theArray[i].lang == defaultLang) {
                return theArray[i].value;
            }
        }

        return null;
    };


    // *************************************
    // Private functions
    //
    // Cookie and preferred IdP Handling
    //
    // *************************************

    /**
       Gets the preferred IdPs.  The first elements in the array will
       be the preselected preferred IdPs.  The following elements will
       be those past IdPs selected by a user.  The size of the array
       will be no larger than the maximum number of preferred IdPs.
    */
    var getPreferredIdPs = function() {
        var idps = [];
        var offset = 0;
        var i;
        var j;

        //
        // populate start of array with preselected IdPs
        //
        if(null != preferredIdP){
            for(i=0; i < preferredIdP.length && i < maxPreferredIdPs-1; i++){
                idps[i] = getIdPFor(preferredIdP[i]);
                offset++;
            }
        }

        //
        // And then the cookie based ones
        //
        userSelectedIdPs = retrieveUserSelectedIdPs();
        for (i = offset, j=0; i < userSelectedIdPs.length && i < maxPreferredIdPs; i++, j++){
            idps.push(getIdPFor(userSelectedIdPs[j]));
        }
        return idps;
    };

    /**
       Update the userSelectedIdPs list with the new value.

       @param (String) the newly selected IdP
    */
    var updateSelectedIdPs = function(newIdP) {

        //
        // We cannot use split since it does not appear to
        // work as per spec on ie8.
        //
        var newList = [];

        //
        // iterate through the list copying everything but the old
        // name
        //
        while (0 !== userSelectedIdPs.length) {
            var what = userSelectedIdPs.pop();
            if (what != newIdP) {
                newList.unshift(what);
            }
        }

        //
        // And shove it in at the top
        //
        newList.unshift(newIdP);
        userSelectedIdPs = newList;
        return;
    };

    /**
       Gets the IdP previously selected by the user.

       @return {Array} user selected IdPs identified by their entity ID
    */
    var retrieveUserSelectedIdPs = function(){
        var userSelectedIdPs = [];
        var i, j;
        var cookies;

        cookies = document.cookie.split( ';' );
        for (i = 0; i < cookies.length; i++) {
            //
            // Do not use split('='), '=' is valid in Base64 encoding!
            //
            var cookie = cookies[i];
            var splitPoint = cookie.indexOf( '=' );
            var cookieName = cookie.substring(0, splitPoint);
            var cookieValues = cookie.substring(splitPoint+1);

            if ( '_saml_idp' == cookieName.replace(/^\s+|\s+$/g, '') ) {
                cookieValues = cookieValues.replace(/^\s+|\s+$/g, '').split('+');
                for(j=0; j< cookieValues.length; j++){
                    if (0 === cookieValues[j].length) {
                        continue;
                    }
                    var dec = base64Decode(decodeURIComponent(cookieValues[j]));
                    if (dec.length > 0) {
                        userSelectedIdPs.push(dec);
                    }
                }
            }
        }

        return userSelectedIdPs;
    };

    /**
       Saves the IdPs selected by the user.

       @param {Array} idps idps selected by the user
    */
    var saveUserSelectedIdPs = function(idps){
        var cookieData = [];
        var length = idps.length;
        if (length > 5) {
            length = 5;
        }
        for(var i=0; i < length; i++){
            if (idps[i].length > 0) {
                cookieData.push(encodeURIComponent(base64Encode(idps[i])));
            }
        }

        var expireDate = null;
        if(samlIdPCookieTTL){
            var now = new Date();
            cookieTTL = samlIdPCookieTTL * 24 * 60 * 60 * 1000;
            expireDate = new Date(now.getTime() + cookieTTL);
        }

        document.cookie='_saml_idp' + '=' + cookieData.join('+') + '; path = /' +
            ((expireDate===null) ? '' : '; expires=' + expireDate.toUTCString());
    };

    /**
       Base64 encodes the given string.

       @param {String} input string to be encoded

       @return {String} base64 encoded string
    */
    var base64Encode = function(input) {
        var output = '', c1, c2, c3, e1, e2, e3, e4;

        for ( var i = 0; i < input.length; ) {
            c1 = input.charCodeAt(i++);
            c2 = input.charCodeAt(i++);
            c3 = input.charCodeAt(i++);
            e1 = c1 >> 2;
            e2 = ((c1 & 3) << 4) + (c2 >> 4);
            e3 = ((c2 & 15) << 2) + (c3 >> 6);
            e4 = c3 & 63;
            if (isNaN(c2)){
                e3 = e4 = 64;
            } else if (isNaN(c3)){
                e4 = 64;
            }
            output += base64chars.charAt(e1) +
                base64chars.charAt(e2) +
                base64chars.charAt(e3) +
                base64chars.charAt(e4);
        }

        return output;
    };

    /**
       Base64 decodes the given string.

       @param {String} input string to be decoded

       @return {String} base64 decoded string
    */
    var base64Decode = function(input) {
        var output = '', chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;

        // Remove all characters that are not A-Z, a-z, 0-9, +, /, or =
        var base64test = /[^A-Za-z0-9\+\/\=]/g;
        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, '');

        do {
            enc1 = base64chars.indexOf(input.charAt(i++));
            enc2 = base64chars.indexOf(input.charAt(i++));
            enc3 = base64chars.indexOf(input.charAt(i++));
            enc4 = base64chars.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64) {
                output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64) {
                output = output + String.fromCharCode(chr3);
            }

            chr1 = chr2 = chr3 = '';
            enc1 = enc2 = enc3 = enc4 = '';

        } while (i < input.length);

        return output;
    };

    // *************************************
    // Private functions
    //
    // Error Handling.  we'll keep it separate with a view to eventual
    //                  exbedding into log4js
    //
    // *************************************
    /**

    */

    var fatal = function(message) {
        alert('Internal Error - ' + message);
        var txt = document.createTextNode(message);
        idpSelectDiv.appendChild(txt);
    };

    var debug = function() {
        //
        // Nothing
    };
}
(new IdPSelectUI()).draw(new IdPSelectUIParms());
