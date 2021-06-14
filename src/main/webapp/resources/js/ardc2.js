var ardcDisplaySelector = "span[data-cvoc-protocol='ardc']";
var ardcInputSelector = "input[data-cvoc-protocol='ardc']";

$(document).ready(function() {
    getData().then((data) => {
        expandARDC(data);
        updateARDCInputs(data);
    });
});

function getData() {
    return new Promise((resolve, reject) => {
    	//Until CORS is supported for this URI, get a local copy at a hardcoded URL
        //          fetch("https://dataverse-tools.ada.edu.au/api/forCode")
        fetch("http://ec2-3-238-111-188.compute-1.amazonaws.com/resources/js/forCode")
        //That URI gives a 201 response but the local URL just sends 200
            //.then((dd) => dd.status === 201 && dd.json())
            .then((dd) => dd.status === 200 && dd.json())
            .then((data) => resolve(data))
            .catch((err) => console.log(err));
    });
}

function expandARDC(data) {
    // Check each selected element
    $(ardcDisplaySelector).each(function() {
        var ardcDisplayElement = this;
        // If it hasn't already been processed
        if (!$(ardcDisplayElement).hasClass('expanded')) {
            // Mark it as processed
            $(ardcDisplayElement).addClass('expanded');
            //Retrieve parameters from the element's attributes
            //The service URL to contact using this protocol
            var cvocUrl = $(ardcDisplayElement).attr('data-cvoc-service-url');
            //The language requested
            var lang = ardcDisplayElement.hasAttribute("lang") ? $(ardcDisplayElement).attr('lang') : "";
            //The value in the element. Currently, this must be either the URI of a term or plain text - with the latter not being formatted at all by this script
            var id = ardcDisplayElement.textContent;
            let names = findTerm(id, data);
            console.log(JSON.stringify(names));
            var html = "<a href='" + id + "'  target='_blank' rel='noopener' title='" + names.vocabName + "'>" + names.termName + "</a>";
            console.log(html);
            $(ardcDisplayElement).html(html);
        }
    });
}

function updateARDCInputs(data) {
    // For each input element within personardcInputSelector elements
    $(ardcInputSelector).each(function() {
        var ardcInput = this;
        if (!ardcInput.hasAttribute('data-ardc')) {
            // Random identifier
            let num = Math.floor(Math.random() * 100000000000);

            var cvocUrl = $(ardcInput).attr('data-cvoc-service-url');
            var lang = ardcInput.hasAttribute("lang") ? $(ardcInput).attr('lang') : "";
            var langParam = ardcInput.hasAttribute("lang") ? "&lang=" + $(ardcInput).attr('lang') : "";
            var vocabs = JSON.parse($(ardcInput).attr('data-cvoc-vocabs'));
            var managedFields = JSON.parse($(ardcInput).attr('data-cvoc-managedfields'));
            var parentField = $(ardcInput).attr('data-cvoc-parent');
            var termParentUri = $(ardcInput).attr('data-cvoc-filter');
            var selectId = "ardcAddSelect_" + num;
            var vocab = Object.keys(vocabs)[0];

            var anchorSib = ardcInput;
            if ($("[data-cvoc-parentfield='" + parentField + "']").length > 0) {
                // Hide the actual input and give it a data-person number so we can find it
                anchorSib = $(ardcInput).parentsUntil("[data-cvoc-parentfield='" + parentField + "']").last();
            }
            $(anchorSib).parent().children().hide();
            console.log(parentField + " " + num);
            $(ardcInput).attr('data-ardc', num);
            // Todo: if not displayed, wait until it is to then create the select 2
            // with a non-zero width

            // ToDo - get vocab selection
            var vocabId = "ardcVocabSelect_" + num;
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
                console.log(vocab);
                $('#' + selectId).attr('data-cvoc-vocab-changed', 'true');
                $('#' + selectId).attr('data-cvoc-cur-vocab', vocab);
                $("#" + selectId).text('');
                console.log('Triggering change');
                $('#' + selectId).val(null).trigger('change');
            });
            if (Object.keys(vocabs).length == 1 || $(ardcInput).val().startsWith("http")) {
                $(anchorSib).parent().find('.cvoc-vocab').hide();
            }

            // Add a select2 element to allow search and provide a list of choices
            if ($(anchorSib).parent().find('.cvoc-vocab').length != 0) {
                $(anchorSib).parent().find('.cvoc-vocab').after($('<div/>').addClass('cvoc-term col-sm-9').append($('<label/>').text('Term')).append(
                    '<select id=' + selectId + ' class="form-control add-resource select2" tabindex="-1" aria-hidden="true">'));
                $('#' + selectId).attr('data-cvoc-cur-vocab-changed', 'true');
                $('#' + selectId).attr('data-cvoc-cur-vocab', vocab);
                if (Object.keys(vocabs).length == 1 || $(ardcInput).val().startsWith("http")) {
                    $(anchorSib).parent().find('.cvoc-term > label').hide();
                    $('.cvoc-term').removeClass('col-sm-9');
                }
                changeOptions(selectId, vocab, vocabs, data);
            } else {
                $(anchorSib).after(
                    '<select id=' + selectId + ' class="form-control add-resource select2" tabindex="-1" aria-hidden="true">');
            }
            $("#" + selectId).select2({
                theme: "bootstrap",
                tags: false,
                delay: 200,
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
                language: {
                    searching: function(params) {
                        // Change this to be appropriate for your application
                        return 'Search by term…';
                    }
                },
                placeholder: "Add a Term",
                minimumInputLength: 0,
                allowClear: true,
            });
            // If the input has a value already, format it the same way as if it
            // were a new selection
            var id = $(ardcInput).val();
            if (id.length > 0) {
                console.log("ID : " + id);
                let {
                    termName,
                    vocabName
                } = findTerm(id, data);
                var text = termName + ", " + id;
                var newOption = new Option(text, id, true, true);
                newOption.title = 'Open in new tab to view Term page';
                $('#' + selectId).append(newOption).trigger('change');
            }

            // Could start with the selection menu open
            // $("#" + selectId).select2('open');
            // When a selection is made, set the value of the hidden input field
            $('#' + selectId).on('select2:select', function(e) {
                console.log('SELECT!');
                var data = e.params.data;
                // For terms, the id and text are different, but we just store the
                // data.id in either case (controlled uri or plain text)
                // if(data.id != data.text) {
                $("input[data-ardc='" + num + "']").val(data.id);
                if (Object.keys(vocabs).length > 1) {
                    $(anchorSib).parent().find('.cvoc-vocab').hide();
                    $(anchorSib).parent().find('.cvoc-term > label').hide();
                    $('.cvoc-term').removeClass('col-sm-9');
                }
                if ($("[data-cvoc-parentfield='" + parentField + "']").length > 0) {
                    console.log('num: ' + num);
                    console.log('In: ' + $("input[data-ardc='" + num + "']").length);
                    var parent = $("input[data-ardc='" + num + "']").closest("[data-cvoc-parentfield='" + parentField + "']");
                    //var parent = $("[data-cvoc-parentfield='" + parentField +"']");
                    console.log('parent len: ' + parent.length);

                    for (var key in managedFields) {
                        if (key == 'vocabularyName') {
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', $('#' + selectId).attr('data-cvoc-cur-vocab'));
                        } else if (key == 'vocabularyUri') {
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', vocabs[$('#' + selectId).attr('data-cvoc-cur-vocab')]);
                        } else if (key == 'termName') {
                            $(parent).find("input[data-cvoc-managed-field='" + managedFields[key] + "']").attr('value', data.text.substring(data.text.indexOf(', http')));
                        }
                    }
                }

            });
            // When changed, update vocab options
            $('#' + selectId).on('change.select2', function(e) {
                console.log("CHANGE!");
                var vocabChanged = $('#' + selectId).attr('data-cvoc-vocab-changed');
                console.log('Len: ' + $('#' + selectId + ' options').length);

                if ((typeof vocabChanged !== 'undefined')) {
                    console.log('vocab has changed');
                    $('#' + selectId).removeAttr('data-cvoc-vocab-changed');
                    var vocab = $('#' + selectId).attr('data-cvoc-cur-vocab');
                    changeOptions(selectId, vocab, vocabs, data);
                }
            });
            // When a selection is cleared, clear the hidden input
            $('#' + selectId).on('select2:clear', function(e) {
                console.log('CLEAR!');
                var vocab = $('#' + selectId).attr('data-cvoc-cur-vocab');
                //$('#' + selectId).attr('data-cvoc-cur-vocab-changed','true');
                changeOptions(selectId, vocab, vocabs, data);
                $("input[data-ardc ='" + num + "']").attr('value', '');
                //$('#' + selectId).text('');
                if (Object.keys(vocabs).length > 1) {
                    $(anchorSib).parent().find('.cvoc-vocab').show();
                    $(anchorSib).parent().find('.cvoc-term > label').show();
                    $('.cvoc-term').addClass('col-sm-9');
                }

                if ($("[data-cvoc-parentfield='" + parentField + "']").length > 0) {

                    var parent = $("input[data-ardc='" + num + "']").closest("[data-cvoc-parentfield='" + parentField + "']");
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

function findTerm(id, data) {
    var combinedValue = '';
    let vocabName = '';
    for (var aVal of data.term['ANZSRC-2020-FoR']) {
        if (aVal.endsWith(id)) {
            combinedValue = aVal;
            vocabName = 'ANZSRC-2020-FoR';
            break;
        }
    }
    if (combinedValue.length == 0) {
        for (var aVal of data.term['ANZSRC-2008-FoR']) {
            if (aVal.endsWith(id)) {
                combinedValue = aVal;
                vocabName = 'ANZSRC-2008-FoR';
                break;
            }
        }
    }
    if (combinedValue.length != 0) {
        var parts = combinedValue.split(':');
        let termName = parts[1].trim();
        return {
            termName,
            vocabName
        };
    }
}

function changeOptions(selectId, vocab, vocabs, data) {
    $("#" + selectId).find('option').remove();
    $("#" + selectId).append($('<option>'));
    for (var key of data.term[vocab]) {
        var parts = key.split(':');
        var name = parts[1].trim();
        var id = (parts[2] + ":" + parts[3]).trim();
        $("#" + selectId).append($('<option>').attr('value', id).html($('<a>').attr('href', vocabs[id]).attr('target', '_blank').attr('rel', 'noopener').text(name)));
    }
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