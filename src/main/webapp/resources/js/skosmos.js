var skosmosSelector = "span[data-cvoc-protocol='skosmos']";
var skosmosInputSelector = "input[data-cvoc-protocol='skosmos']";

$(document).ready(function() {
    expandSkosmos();
    updateSkosmosInputs();
});

function expandSkosmos() {
    // Check each element with class 'skosmos'
    $(skosmosSelector).each(function() {
        var skosmosElement = this;
        // If it hasn't already been processed
        if (!$(skosmosElement).hasClass('expanded')) {
            // Mark it as processed
            $(skosmosElement).addClass('expanded');
            var cvocUrl = $(skosmosElement).attr('data-cvoc-service-url');
            var lang = skosmosElement.hasAttribute("lang") ? $(skosmosElement).attr('lang') : "";
            var id = skosmosElement.textContent;
            // Try to retrieve info about the term
            if (id.startsWith("http")) {
                $.ajax({
                    type: "GET",
                    url: cvocUrl + "rest/v1/data?uri=" + id,
                    dataType: 'json',
                    headers: {
                        'Accept': 'application/json'
                    },
                    success: function(term, status) {
                        var termName = "";
                        var uriArray = term.graph;
                        for (let i = 0; i < uriArray.length; i++) {
                            var def = uriArray[i];
                            if (def.uri == id) {
                                for (let j = 0; j < def.prefLabel.length; j++) {
                                    var label = def.prefLabel[j];
                                    if (label.lang == lang) {
                                        termName = label.value;
                                        break;
                                    }
                                    if (label.lang == "en") {
                                        termName = label.value;
                                        // Don't break so we continue to find one
                                        // matching lang if it exists
                                    }
                                }
                            }
                        }
                        var html = "<a href='" + id + "'  target='_blank' rel='noopener' >" + termName + "</a>";
                        skosmosElement.innerHTML = html;
                    },
                    failure: function(jqXHR, textStatus, errorThrown) {
                        if (jqXHR.status != 404) {
                            console.error("The following error occurred: " + textStatus, errorThrown);
                        }
                    }
                });
            } else {
                // Don't change the display
            }
        }
    });
}

function updateSkosmosInputs() {
    // For each input element within personInputSelector elements
    $(skosmosInputSelector).each(function() {
        var skosmosInput = this;
        if (!skosmosInput.hasAttribute('data-skosmos')) {
            // Random identifier
            let num = Math.floor(Math.random() * 100000000);

            var cvocUrl = $(skosmosInput).attr('data-cvoc-service-url');
            var lang = skosmosInput.hasAttribute("lang") ? $(skosmosInput).attr('lang') : "";
            var langParam = skosmosInput.hasAttribute("lang") ? "&lang=" + $(skosmosInput).attr('lang') : "";
            var vocabs = JSON.parse($(skosmosInput).attr('data-cvoc-vocabs'));
            var managedFields = JSON.parse($(skosmosInput).attr('data-cvoc-managedfields'));
            var parentField = $(skosmosInput).attr('data-cvoc-parent');
            var termParentUri = $(skosmosInput).attr('data-cvoc-filter');
            var selectId = "skosmosAddSelect_" + num;
            var vocab = Object.keys(vocabs)[0];

            var anchorSib = skosmosInput;
            if ($("[data-cvoc-parentfield='" + parentField + "']").length > 0) {
                // Hide the actual input and give it a data-person number so we can find it
                anchorSib = $(skosmosInput).parentsUntil("[data-cvoc-parentfield='" + parentField + "']").last();
            }
            $(anchorSib).parent().children().hide();
            console.log(parentField + " " + num);
            $(skosmosInput).attr('data-skosmos', num);
            // Todo: if not displayed, wait until it is to then create the select 2
            // with a non-zero width

            // ToDo - get vocab selection
            var vocabId = "skosmosVocabSelect_" + num;
            $(anchorSib).before($('<div/>').addClass('cvoc-vocab col-sm-3'));
            $(anchorSib).parent().find('.cvoc-vocab').append($('<label/>').text('Vocabulary'));
            $(anchorSib).parent().find('.cvoc-vocab').append(
                '<select id=' + vocabId + ' class="form-control add-resource select2" tabindex="-1" aria-hidden="true"  style="width: 100px">');
            for (var key in vocabs) {
                $("#" + vocabId).append($('<option>').attr('value', key).html($('<a>').attr('href', vocabs[key]).attr('target', '_blank').attr('rel', 'noopener').text(key)));
            }
            $("#" + vocabId).select2({
                theme: "bootstrap",
                tags: false,
                delay: 500,
                templateResult: function(item) {
                    // No need to template the searching text
                    if (item.loading) {
                        return item.text;
                    }
                    var term = '';
                    if (typeof(query) !== 'undefined') {
                        term = query.term;
                    }
                    // markMatch bolds the search term if/where it
                    // appears in the result
                    var $result = markMatch(item.text, term);
                    return $result;
                },
                placeholder: "Select a vocabulary",
                minimumInputLength: 0,
            });
            $('#' + vocabId).on('select2:select', function(e) {
                var data = e.params.data;
                vocab = data.id;
                $('#' + selectId).attr('data-cvoc-cur-vocab', vocab);
                $("#" + selectId).text('');
                $('#' + selectId).val(null).trigger('change');
            });
            if (Object.keys(vocabs).length == 1 || $(skosmosInput).val().startsWith("http")) {
                $(anchorSib).parent().find('.cvoc-vocab').hide();
            }

            // Add a select2 element to allow search and provide a list of choices
            if ($(anchorSib).parent().find('.cvoc-vocab').length != 0) {
                $(anchorSib).parent().find('.cvoc-vocab').after($('<div/>').addClass('cvoc-term col-sm-9').append($('<label/>').text('Term')).append(
                    '<select id=' + selectId + ' class="form-control add-resource select2" tabindex="-1" aria-hidden="true" style="width: 400px">'));
                $('#' + selectId).attr('data-cvoc-cur-vocab', vocab);
                if (Object.keys(vocabs).length == 1 || $(skosmosInput).val().startsWith("http")) {
                    $(anchorSib).parent().find('.cvoc-term > label').hide();
                    $('.cvoc-term').removeClass('col-sm-9');
                }
            } else {
                $(anchorSib).after(
                    '<select id=' + selectId + ' class="form-control add-resource select2" tabindex="-1" aria-hidden="true"  style="width: 400px">');
            }
            $("#" + selectId).select2({
                theme: "bootstrap",
                tags: true,
                delay: 500,
                templateResult: function(item) {
                    // No need to template the searching text
                    if (item.loading) {
                        return item.text;
                    }
                    var term = '';
                    if (typeof(query) !== 'undefined') {
                        term = query.term;
                    }
                    // markMatch bolds the search term if/where it appears in the result
                    var $result = markMatch(item.text, term);
                    return $result;
                },
                templateSelection: function(item) {
                    // For a selection, add HTML to make the term uri a link
                    if (item.text != item.id) {
                        var pos = item.text.search(/http[s]?:\/\//);
                        if (pos >= 0) {
                            var termuri = item.text.substr(pos);
                            return $('<span></span>').append(item.text.replace(termuri, "<a href='" + termuri + "'  target='_blank' rel='noopener'>" + termuri + "</a>"));
                        }
                    }
                    return item.text;
                },
                language: {
                    searching: function(params) {
                        // Change this to be appropriate for your application
                        return 'Search by term or related term…';
                    }
                },
                placeholder: "Add a Term",
                minimumInputLength: 2,
                allowClear: true,
                ajax: {
                    url: function() {
                        return cvocUrl + 'rest/v1/search?unique=true&vocab=' + $('#' + selectId).attr('data-cvoc-cur-vocab') + termParentUri + langParam;
                    },
                    dataType: "json",
                    data: function(params) {
                        // Used in templateResult
                        term = params.term;
                        return "&query=" + params.term + "*";
                    },
                    processResults: function(data, page) {
                        return {
                            results: data.results
                                // Sort to bring recently used entries to the top of the list
                                .sort((a, b) => (localStorage.getItem(b['orcid-id'])) ? 1 : -1)
                                .map(
                                    function(x) {
                                        return {
                                            text: x.prefLabel + ((x.hasOwnProperty('altLabel') && x.altLabel.length > 0) ? " (" + x.altLabel + "), " : ", ") +
                                                x.uri,
                                            name: x.prefLabel,
                                            id: x.uri,
                                            // Since clicking in the selection re-opens the
                                            // choice list, one has to use a right click/open in
                                            // new tab/window to view the ORCID page
                                            // Using title to provide that hint as a popup
                                            title: 'Open in new tab to view Term page'
                                        }
                                    })
                        };
                    }
                }
            });
            // If the input has a value already, format it the same way as if it
            // were a new selection
            var id = $(skosmosInput).val();
            if (id.startsWith("http")) {
                $.ajax({
                    type: "GET",
                    url: cvocUrl + "rest/v1/data?uri=" + id,
                    dataType: 'json',
                    headers: {
                        'Accept': 'application/json'
                    },
                    success: function(term, status) {
                        var termName = "";
                        var uriArray = term.graph;
                        for (let i = 0; i < uriArray.length; i++) {
                            var def = uriArray[i];
                            if (def.uri == id) {
                                for (let j = 0; j < def.prefLabel.length; j++) {
                                    var label = def.prefLabel[j];
                                    if (label.lang == lang) {
                                        termName = label.value;
                                        break;
                                    }
                                    if (label.lang == "en") {
                                        termName = label.value;
                                        // Don't break so we continue to find one matching
                                        // lang if it exists
                                    }
                                }
                            }
                        }
                        var text = termName + ", " + id;
                        var newOption = new Option(text, id, true, true);
                        newOption.title = 'Open in new tab to view Term page';
                        $('#' + selectId).append(newOption).trigger('change');


                        // ToDo asssume vocab from variable
                        // can't get altLabel from this api call
                    },
                    failure: function(jqXHR, textStatus, errorThrown) {
                        if (jqXHR.status != 404) {
                            console.error("The following error occurred: " + textStatus, errorThrown);
                        }
                    }
                });
            } else {
                // If the initial value is not a managed term (legacy, or if tags are
                // enabled), just display it as is
                var newOption = new Option(id, id, true, true);
                $('#' + selectId).append(newOption).trigger('change');
            }
            // Could start with the selection menu open
            // $("#" + selectId).select2('open');
            // When a selection is made, set the value of the hidden input field
            $('#' + selectId).on('select2:select', function(e) {
                var data = e.params.data;
                // For terms, the id and text are different, but we just store the
                // data.id in either case (controlled uri or plain text)
                // if(data.id != data.text) {
                $("input[data-skosmos='" + num + "']").val(data.id);
                if (Object.keys(vocabs).length > 1) {
                    $(anchorSib).parent().find('.cvoc-vocab').hide();
                    $(anchorSib).parent().find('.cvoc-term > label').hide();
                    $('.cvoc-term').removeClass('col-sm-9');
                }
                if ($("[data-cvoc-parentfield='" + parentField + "']").length > 0) {
                    console.log('num: ' + num);
                    console.log('In: ' + $("input[data-skosmos='" + num + "']").length);
                    var parent = $("input[data-skosmos='" + num + "']").closest("[data-cvoc-parentfield='" + parentField + "']");
                    //var parent = $("[data-cvoc-parentfield='" + parentField +"']");
                    console.log('parent len: ' + parent.length);
                    $(parent).attr('data-test', num);
                    for (var key in managedFields) {
                        if (key == 'vocabularyName') {
                            console.log("Setting value for " + key + " : " + managedFields[key] + " : " + $('#' + selectId).attr('data-cvoc-cur-vocab'));
                            console.log("Other vals: " + vocabs[$('#' + selectId).attr('data-cvoc-cur-vocab')] + " : " + data.name);
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', $('#' + selectId).attr('data-cvoc-cur-vocab'));
                        } else if (key == 'vocabularyUri') {
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', vocabs[$('#' + selectId).attr('data-cvoc-cur-vocab')]);
                        } else if (key == 'termName') {
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', data.name);
                        }
                    }
                }

            });
            // When a selection is cleared, clear the hidden input
            $('#' + selectId).on('select2:clear', function(e) {
                $("input[data-skosmos='" + num + "']").attr('value', '');
                $('#' + selectId).text('');
                if (Object.keys(vocabs).length > 1) {
                    $(anchorSib).parent().find('.cvoc-vocab').show();
                    $(anchorSib).parent().find('.cvoc-term > label').show();
                    $('.cvoc-term').addClass('col-sm-9');
                }

                if ($("[data-cvoc-parentfield='" + parentField + "']").length > 0) {

                    var parent = $("input[data-skosmos='" + num + "']").closest("[data-cvoc-parentfield='" + parentField + "']");
                    for (var key in managedFields) {
                        if (key == 'vocabularyName') {
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', '');
                        } else if (key == 'vocabularyUri') {
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', '');
                        } else if (key == 'termName') {
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', '');
                        }
                    }
                }

            });
        }
    });
}

// Put the text in a result that matches the term in a span with class
// select2-rendered__match that can be styled (e.g. bold)
function markMatch(text, term) {
    // Find where the match is
    var match = text.toUpperCase().indexOf(term.toUpperCase());
    var $result = $('<span></span>');
    // If there is no match, move on
    if (match < 0) {
        return $result.text(text);
    }

    // Put in whatever text is before the match
    $result.text(text.substring(0, match));

    // Mark the match
    var $match = $('<span class="select2-rendered__match"></span>');
    $match.text(text.substring(match, match + term.length));

    // Append the matching text
    $result.append($match);

    // Put in whatever is after the match
    $result.append(text.substring(match + term.length));

    return $result;
}