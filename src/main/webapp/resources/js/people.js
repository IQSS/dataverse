var personSelector = "span[data-cvoc-protocol='orcid']";
var personInputSelector = "input[data-cvoc-protocol='orcid']";

$(document).ready(function() {
    expandPeople();
    updatePeopleInputs();
});

function expandPeople() {
    //Check each selected element
    $(personSelector).each(function() {
        var personElement = this;
        //If it hasn't already been processed
        if (!$(personElement).hasClass('expanded')) {
            //Mark it as processed
            $(personElement).addClass('expanded');
            var id = personElement.textContent;
            if (id.startsWith("https://orcid.org/")) {
                id = id.substring(18);
            }
            //Try it as an ORCID (could validate that it has the right form and even that it validates as an ORCID, or can just let the GET fail
            $.ajax({
                type: "GET",
                url: "https://pub.orcid.org/v3.0/" + id + "/person",
                dataType: 'json',
                headers: {
                    'Accept': 'application/json'
                },
                success: function(person, status) {
                    //If found, construct the HTML for display
                    var name = person.name['family-name'].value + ", " + person.name['given-names'].value;
                    var html = "<a href='https://orcid.org/" + id + "' target='_blank' rel='noopener' >" + name + "</a>";
                    personElement.innerHTML = html;
                    //If email is public, show it using the jquery popover functionality
                    if (person.emails.email.length > 0) {
                        $(personElement).popover({
                            content: person.emails.email[0].email,
                            placement: 'top',
                            template: '<div class="popover" role="tooltip" style="max-width:600px;word-break:break-all"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>'
                        });
                        personElement.onmouseenter = function() {
                            $(this).popover('show');
                        };
                        personElement.onmouseleave = function() {
                            $(this).popover('hide');
                        };
                    }
                    //Store the most recent 100 ORCIDs - could cache results, but currently using this just to prioritized recently used ORCIDs in search results
                    if (localStorage.length > 100) {
                        localStorage.removeItem(localStorage.key(0));
                    }
                    localStorage.setItem(id, name);
                },
                failure: function(jqXHR, textStatus, errorThrown) {
                    //Generic logging - don't need to do anything if 404 (leave display as is)
                    if (jqXHR.status != 404) {
                        console.error("The following error occurred: " + textStatus, errorThrown);
                    }
                }
            });
        }
    });
}

function updatePeopleInputs() {
    //For each input element within personInputSelector elements 
    $(personInputSelector).each(function() {
        var personInput = this;
        if (!personInput.hasAttribute('data-person')) {
            //Random identifier
            let num = Math.floor(Math.random() * 100000000000);

            //Hide the actual input and give it a data-person number so we can find it
            $(personInput).hide();
            $(personInput).attr('data-person', num);
            //Todo: if not displayed, wait until it is to then create the select 2 with a non-zero width

            //Add a select2 element to allow search and provide a list of choices
            var selectId = "personAddSelect_" + num;
            $(personInput).after(
                '<select id=' + selectId + ' class="form-control add-resource select2" tabindex="-1" aria-hidden="true">');
            $("#" + selectId).select2({
                theme: "bootstrap",
                tags: true,
                delay: 500,
                templateResult: function(item) {
                    // No need to template the searching text
                    if (item.loading) {
                        return item.text;
                    }

                    //markMatch bolds the search term if/where it appears in the result
                    var $result = markMatch(item.text, term);
                    return $result;
                },
                templateSelection: function(item) {
                    //For a selection, add HTML to make the ORCID a link
                    var pos = item.text.search(/\d{4}-\d{4}-\d{4}-\d{3}[\dX]/);
                    if (pos >= 0) {
                        var orcid = item.text.substr(pos, 19);
                        return $('<span></span>').append(item.text.replace(orcid, "<a href='https://orcid.org/" + orcid + "'>" + orcid + "</a>"));
                    }
                    return item.text;
                },
                language: {
                    searching: function(params) {
                        // Change this to be appropriate for your application
                        return 'Search by name, email, or ORCID…';
                    }
                },
                placeholder: "Add a Person",
                minimumInputLength: 3,
                allowClear: true,
                ajax: {
                    //Use an ajax call to ORCID to retrieve matching results
                    url: function(params) {
                        var term = params.term;
                        if (!term) {
                            term = "";
                        }
                        //Use expanded-search to get the names, email directly in the results
                        return "https://pub.orcid.org/v3.0/expanded-search";
                    },
                    data: function(params) {
                        term = params.term;
                        if (!term) {
                            term = "";
                        }
                        var query = {
                            q: term,
                            //Currently we just get the top 10 hits. We could get, for example, the top 50, sort as below to put recently used orcids at the top, and then limit to 10
                            rows: 10
                        }
                        return query;
                    },
                    //request json (vs XML default)
                    headers: {
                        'Accept': 'application/json'
                    },
                    processResults: function(data, page) {
                        return {
                            results: data['expanded-result']
                                //Sort to bring recently used ORCIDS to the top of the list
                                .sort((a, b) => (localStorage.getItem(b['orcid-id'])) ? 1 : -1)
                                .map(
                                    function(x) {
                                        return {
                                            text: x['given-names'] + " " + x['family-names'] +
                                                ", " +
                                                x['orcid-id'] +
                                                ((x.email.length > 0) ? ", " + x.email[0] : ""),
                                            id: x['orcid-id'],
                                            //Since clicking in the selection re-opens the choice list, one has to use a right click/open in new tab/window to view the ORCID page
                                            //Using title to provide that hint as a popup
                                            title: 'Open in new tab to view ORCID page'
                                        }
                                    })
                        };
                    }
                }
            });
            //If the input has a value already, format it the same way as if it were a new selection
            var id = $(personInput).val();
            if (id.startsWith("https://orcid.org")) {
                id = id.substring(18);
                $.ajax({
                    type: "GET",
                    url: "https://pub.orcid.org/v3.0/" + id + "/person",
                    dataType: 'json',
                    headers: {
                        'Accept': 'application/json'
                    },
                    success: function(person, status) {
                        var name = person.name['given-names'].value + " " + person.name['family-name'].value;
                        var text = name + ", " + id;
                        if (person.emails.email.length > 0) {
                            text = text + ", " + person.emails.email[0].email;
                        }
                        var newOption = new Option(text, id, true, true);
                        newOption.title = 'Open in new tab to view ORCID page';
                        $('#' + selectId).append(newOption).trigger('change');
                    },
                    failure: function(jqXHR, textStatus, errorThrown) {
                        if (jqXHR.status != 404) {
                            console.error("The following error occurred: " + textStatus, errorThrown);
                        }
                    }
                });
            } else {
                //If the initial value is not an ORCID (legacy, or if tags are enabled), just display it as is 
                var newOption = new Option(id, id, true, true);
                $('#' + selectId).append(newOption).trigger('change');
            }
            //Cound start with the selection menu open
            //    $("#" + selectId).select2('open');
            //When a selection is made, set the value of the hidden input field
            $('#' + selectId).on('select2:select', function(e) {
                var data = e.params.data;
                //For ORCIDs, the id and text are different
                if (data.id != data.text) {
                    $("input[data-person='" + num + "']").val("https://orcid.org/" + data.id);
                } else {
                    //Tags are allowed, so just enter the text as is
                    $("input[data-person='" + num + "']").val(data.id);
                }
            });
            //When a selection is cleared, clear the hidden input
            $('#' + selectId).on('select2:clear', function(e) {
                $("input[data-person='" + num + "']").attr('value', '');
            });
        }
    });
}

//Put the text in a result that matches the term in a span with class select2-rendered__match that can be styled (e.g. bold)
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