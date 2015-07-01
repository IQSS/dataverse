var MYDATA_DEBUG_ON = true;
var APPEND_CARDS_TO_BOTTOM = false;

function clearForNewSearchResults(){
    clearWarningAlert();
    clearCardResults();
    clearPaginationResults();
    clearJsonResults();
}
function clearJsonResults(){
    $("#div-json-results").html('');
}
function clearWarningAlert(){
    $('#div-result-message').html('');
}
function clearCardResults(){
    $('#div-card-results').html('');
}
function clearPaginationResults(){
    console.log('clearPaginationResults');
    $("#div-pagination").html('');
}

function setWarningAlert(alert_msg){
    console.log('setWarningAlert');
    clearCardResults();
    clearPaginationResults();
    var alert_html = '<div class="alert alert-warning" role="alert">' + alert_msg + '</div>';
    console.log(alert_html);
    $('#div-result-message').html(alert_html);
}

function bindPages(){
   console.log("bindpages");
   $("a.page_link").click(function(evt) {
       evt.preventDefault(); // stop link from using href
       var page_num = $(this).attr('rel');
       $("#selected_page").val(page_num);  // update the selected page in the form
       regular_search();    // run search
   });
   
   $( "#lnk_add_more_cards").unbind( "click" );
   $("#lnk_add_more_cards").click(function(evt){
       evt.preventDefault(); // stop link from using href
       var page_num = $(this).attr('rel');
       $("#selected_page").val(page_num);  // update the selected page in the form
       search_add_more_cards();    // run search
   });
}

function search_add_more_cards(){
    console.log('search_add_more_cards init');
    APPEND_CARDS_TO_BOTTOM = true;
    submit_my_data_search();
}
function regular_search(){
    console.log('regular_search init');
    APPEND_CARDS_TO_BOTTOM = false;
    submit_my_data_search();
}


function submit_my_data_search(){


    // --------------------------------
    // Prelims: 
    // --------------------------------
    // Hide the "show more cards" button (may already be hidden)
    $('#div-more-cards-link').hide();
    // If needed, clear existing cards and pager
    if (!APPEND_CARDS_TO_BOTTOM){
        clearForNewSearchResults();
    }

    // --------------------------------
    // (1) Get the form parameters
    // --------------------------------
    var formData = $('#mydata_filter_form').serialize();

    // For debugging, show the search params
    if (MYDATA_DEBUG_ON){
        $("#div-search-params").html(formData);
    }

    // --------------------------------
    // (2) submit the form
    // --------------------------------
    $.getJSON( RETRIEVE_DATA_API_PATH + '?' + formData, function(data) {

        //  For debugging, show the returned JSON
        if (MYDATA_DEBUG_ON){
            $("#div-json-results").html(JSON.stringify(data));
        }

        // --------------------------------
        // (2a) Does the result look good?
        // --------------------------------
        // No, show error messsage and get out of here
        if (!data.success){
            setWarningAlert(data.error_message);
            return;
        }

        // --------------------------------
        // (2b) Looks good, let's make page
        // --------------------------------
        if (data.success){
            // --------------------------------
            // (3a) If JSON has pagination info, make a pager
            // --------------------------------
            if (data.data.hasOwnProperty('pagination')){
                console.log('render pagination');
                var pagination_json = data.data.pagination;
                var pagination_html =  nunjucks.render('mydata_templates/mydata.html', pagination_json);
                $("#div-pagination").html(pagination_html);

                // --------------------------------
                // (3b) If this isn't the last page, show
                //  a "more results" link after the last card
                // --------------------------------
                if (pagination_json.hasNextPageNumber){
                    $('#lnk_add_more_cards').attr("rel", pagination_json.nextPageNumber);
                    console.log("update link to: " + pagination_json.nextPageNumber);
                    $('#div-more-cards-link').show();
                }
                bindPages();
            }
            // --------------------------------
            // (4) Let's render the cards
            // --------------------------------
            // Pass the solr docs to the cards template
            var card_html =  nunjucks.render('mydata_templates/cards.html', data);
            if (APPEND_CARDS_TO_BOTTOM){
                console.log('add cards to bottom results');
                // Add new cards after existing cards
                var newCardsDiv = $('<div></div>', { css: { 'display': 'none' }});
                newCardsDiv.html(card_html);
                
                $("#div-card-results").append(newCardsDiv);
                newCardsDiv.slideDown('slow');;
                
                //$("#div-card-results").append('<hr /><hr />' + card_html).show('slow');;
            }else{
                console.log('regular search results');
                // Only show new cards
                $("#div-card-results").html(card_html);                            
            }
//
            //$.each(data.data.items.solr_docs, function( index, value ) {
                 //alert( index + ": " + value );
            //    $("#div-card-results").append('<div class="well">' + JSON.stringify(value) + '</div>');
            //});


            //alert(card_html);
            //alert(JSON.stringify(pagination_json));
        }
//alert(data);
//makePager(data);
   //     $("#divPagerJSON").html('<pre>' + JSON.stringify(data) + '</pre>');
    });
}
